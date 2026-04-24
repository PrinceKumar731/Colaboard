package com.codejudge.service;

import com.codejudge.model.ExecutionResult;
import com.codejudge.model.enums.Language;
import com.codejudge.model.enums.Verdict;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.*;
import java.util.UUID;
import java.util.concurrent.*;

/**
 * Executes user-submitted code inside an isolated Docker container.
 *
 * Security model:
 *  - --network=none          : no network access
 *  - --memory / --memory-swap: hard memory ceiling (OOM-kill = exit 137)
 *  - --cpus=0.5              : CPU throttle
 *  - --read-only + --tmpfs   : read-only rootfs, writable /tmp only
 *  - --no-new-privileges     : blocks setuid escalation
 *  - --ulimit nproc / nofile : limit fork-bombs and file-descriptor leaks
 *  - timeout <N> inside shell: sends SIGTERM then SIGKILL after N seconds
 */
@Slf4j
@Service
public class DockerExecutionService {

    @Value("${judge.docker.java-image:judge-java:latest}")
    private String javaImage;

    @Value("${judge.docker.python-image:judge-python:latest}")
    private String pythonImage;

    @Value("${judge.docker.cpp-image:judge-cpp:latest}")
    private String cppImage;

    @Value("${judge.execution.temp-dir:/tmp/judge}")
    private String tempDir;

    private static final int DOCKER_OVERHEAD_SECONDS = 15;
    private static final int MAX_OUTPUT_CHARS = 10_000;

    public ExecutionResult execute(String code, Language language, String input,
                                   int timeLimitMs, int memoryLimitMb) {
        String submissionId = UUID.randomUUID().toString();
        Path workDir = Paths.get(tempDir, submissionId);

        try {
            Files.createDirectories(workDir);
            Files.writeString(workDir.resolve(language.getFileName()), code);
            Files.writeString(workDir.resolve("input.txt"), input != null ? input : "");

            return runContainer(workDir, language, timeLimitMs, memoryLimitMb);

        } catch (IOException e) {
            log.error("Failed to prepare execution environment for {}: {}", submissionId, e.getMessage());
            return ExecutionResult.builder()
                    .verdict(Verdict.RUNTIME_ERROR)
                    .errorMessage("Environment setup failed: " + e.getMessage())
                    .build();
        } finally {
            deleteDirectory(workDir);
        }
    }

    private ExecutionResult runContainer(Path workDir, Language language,
                                          int timeLimitMs, int memoryLimitMb) {
        String containerName = "judge-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        String absolutePath = workDir.toAbsolutePath().toString();
        int timeLimitSeconds = Math.max(1, timeLimitMs / 1000);

        String[] cmd = buildDockerCommand(
                containerName, absolutePath, getImage(language),
                buildRunScript(language, timeLimitSeconds), memoryLimitMb
        );

        log.debug("Running container {}: {}", containerName, String.join(" ", cmd));

        long startTime = System.currentTimeMillis();
        try {
            Process process = new ProcessBuilder(cmd)
                    .redirectErrorStream(false)
                    .start();

            StringBuffer stdout = new StringBuffer();
            StringBuffer stderr = new StringBuffer();

            // Read stdout/stderr concurrently — blocking one stream while the other fills causes deadlock
            CompletableFuture<Void> stdoutReader = CompletableFuture.runAsync(
                    () -> drainStream(process.getInputStream(), stdout));
            CompletableFuture<Void> stderrReader = CompletableFuture.runAsync(
                    () -> drainStream(process.getErrorStream(), stderr));

            int wallClockTimeoutSeconds = timeLimitSeconds + DOCKER_OVERHEAD_SECONDS;
            boolean exited = process.waitFor(wallClockTimeoutSeconds, TimeUnit.SECONDS);

            // Give stream readers a moment to finish
            stdoutReader.orTimeout(3, TimeUnit.SECONDS).exceptionally(t -> null).join();
            stderrReader.orTimeout(3, TimeUnit.SECONDS).exceptionally(t -> null).join();

            long elapsedMs = System.currentTimeMillis() - startTime;

            if (!exited) {
                process.destroyForcibly();
                forceRemoveContainer(containerName);
                log.warn("Container {} killed: wall-clock timeout ({}s)", containerName, wallClockTimeoutSeconds);
                return ExecutionResult.builder()
                        .verdict(Verdict.TIME_LIMIT_EXCEEDED)
                        .executionTimeMs(elapsedMs)
                        .timedOut(true)
                        .stderr("Killed: exceeded wall-clock timeout of " + wallClockTimeoutSeconds + "s")
                        .build();
            }

            int exitCode = process.exitValue();
            String out = truncate(stdout.toString(), MAX_OUTPUT_CHARS);
            String err = truncate(stderr.toString(), MAX_OUTPUT_CHARS);

            return mapExitCode(exitCode, out, err, elapsedMs, timeLimitMs);

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return ExecutionResult.builder()
                    .verdict(Verdict.RUNTIME_ERROR)
                    .errorMessage("Execution interrupted")
                    .build();
        } catch (IOException e) {
            log.error("Docker process error: {}", e.getMessage());
            return ExecutionResult.builder()
                    .verdict(Verdict.RUNTIME_ERROR)
                    .errorMessage("Docker execution failed: " + e.getMessage())
                    .build();
        }
    }

    private String[] buildDockerCommand(String containerName, String hostCodePath,
                                         String image, String runScript, int memoryMb) {
        return new String[]{
                "docker", "run",
                "--rm",
                "--name", containerName,
                "--memory=" + memoryMb + "m",
                "--memory-swap=" + memoryMb + "m",    // disable swap entirely
                "--cpus=0.5",
                "--network=none",                      // no outbound network
                "--read-only",                         // immutable rootfs
                "--tmpfs=/tmp:size=64m,noexec",        // writable /tmp, no exec bit
                "--no-new-privileges",                 // block setuid escalation
                "--ulimit", "nofile=64:64",            // limit open file descriptors
                "--ulimit", "nproc=64:64",             // limit spawnable processes (anti-fork-bomb)
                "--security-opt=no-new-privileges",
                "-v", hostCodePath + ":/code:ro",      // mount code read-only
                "-w", "/code",
                image,
                "sh", "-c", runScript
        };
    }

    /**
     * Builds the shell command that runs inside the container.
     * Uses the system 'timeout' binary so TLE is signalled with exit code 124.
     */
    private String buildRunScript(Language language, int timeLimitSeconds) {
        return switch (language) {
            case JAVA -> String.format(
                    "javac Solution.java 2>&1 && timeout %d java -Xmx200m -Xss8m Solution < input.txt",
                    timeLimitSeconds
            );
            case PYTHON -> String.format(
                    "timeout %d python3 solution.py < input.txt",
                    timeLimitSeconds
            );
            case CPP -> String.format(
                    "g++ -O2 -std=c++17 -o /tmp/sol solution.cpp 2>&1 && timeout %d /tmp/sol < input.txt",
                    timeLimitSeconds
            );
        };
    }

    private ExecutionResult mapExitCode(int exitCode, String stdout, String stderr,
                                         long elapsedMs, int timeLimitMs) {
        if (exitCode == 124) {
            return ExecutionResult.builder()
                    .verdict(Verdict.TIME_LIMIT_EXCEEDED)
                    .executionTimeMs(timeLimitMs)
                    .stdout(stdout).stderr(stderr)
                    .exitCode(exitCode).timedOut(true)
                    .build();
        }
        if (exitCode == 137) {
            return ExecutionResult.builder()
                    .verdict(Verdict.MEMORY_LIMIT_EXCEEDED)
                    .executionTimeMs(elapsedMs)
                    .stdout(stdout).stderr(stderr)
                    .exitCode(exitCode)
                    .build();
        }
        if (exitCode != 0 && stdout.isBlank() && !stderr.isBlank()) {
            return ExecutionResult.builder()
                    .verdict(Verdict.COMPILATION_ERROR)
                    .executionTimeMs(elapsedMs)
                    .stdout(stdout).stderr(stderr)
                    .exitCode(exitCode)
                    .build();
        }
        if (exitCode != 0) {
            return ExecutionResult.builder()
                    .verdict(Verdict.RUNTIME_ERROR)
                    .executionTimeMs(elapsedMs)
                    .stdout(stdout).stderr(stderr)
                    .exitCode(exitCode)
                    .build();
        }
        return ExecutionResult.builder()
                .verdict(Verdict.ACCEPTED)   // JudgeService will perform final diff
                .executionTimeMs(elapsedMs)
                .stdout(stdout).stderr(stderr)
                .exitCode(0)
                .build();
    }

    private String getImage(Language language) {
        return switch (language) {
            case JAVA -> javaImage;
            case PYTHON -> pythonImage;
            case CPP -> cppImage;
        };
    }

    private void drainStream(InputStream stream, StringBuffer buffer) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line).append("\n");
            }
        } catch (IOException ignored) {}
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() > max ? s.substring(0, max) + "\n...[output truncated]" : s;
    }

    private void forceRemoveContainer(String containerName) {
        try {
            new ProcessBuilder("docker", "rm", "-f", containerName).start();
        } catch (IOException ignored) {}
    }

    private void deleteDirectory(Path dir) {
        try {
            if (!Files.exists(dir)) return;
            Files.walk(dir)
                 .sorted(java.util.Comparator.reverseOrder())
                 .forEach(p -> {
                     try { Files.delete(p); } catch (IOException ignored) {}
                 });
        } catch (IOException ignored) {}
    }
}
