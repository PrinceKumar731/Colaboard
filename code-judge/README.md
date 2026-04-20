# Code Judge Engine

A production-grade online judge backend — think mini LeetCode — built with **Java 17 + Spring Boot 3**. Each code submission runs inside an isolated Docker container with hard resource limits, making it safe to execute untrusted code.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     Spring Boot Backend                     │
│                                                             │
│  POST /submissions ──► SubmissionService                    │
│                              │                              │
│                              ▼                              │
│                   LinkedBlockingQueue<submissionId>         │
│                              │                              │
│              ┌───────────────┼───────────────┐              │
│           Worker 1        Worker 2        Worker 3          │
│              │                                              │
│              ▼                                              │
│      SubmissionProcessor                                    │
│         │          │                                        │
│         ▼          ▼                                        │
│  DockerExecution  JudgeService (diff output)                │
│         │                                                   │
│         ▼                                                   │
│   docker run --rm                                           │
│     --memory=256m --cpus=0.5                                │
│     --network=none --read-only                              │
│     --no-new-privileges                                     │
│     --ulimit nproc=64 --ulimit nofile=64                    │
│     -v /tmp/judge/<id>:/code:ro                             │
│     judge-python:latest  sh -c "timeout 2 python3 …"       │
└─────────────────────────────────────────────────────────────┘
```

---

## Security Model

| Constraint | Mechanism | What it blocks |
|---|---|---|
| Network isolation | `--network=none` | Data exfiltration, shell downloads |
| Memory cap | `--memory` + `--memory-swap` | Memory bombs, OOM exploitation |
| CPU cap | `--cpus=0.5` | CPU exhaustion DoS |
| Time limit | `timeout <N>` inside container | Infinite loops (exit 124) |
| Read-only FS | `--read-only` + `--tmpfs /tmp` | Persistent writes, rootkit drops |
| Privilege lock | `--no-new-privileges` | `setuid` escalation |
| Fork bomb mitigation | `--ulimit nproc=64` | Fork-bomb (`:(){ :|:& };:`) |
| FD exhaustion | `--ulimit nofile=64` | File-descriptor starvation |
| Unprivileged user | `USER sandbox` in image | Container escape via root |

---

## Supported Languages

| Language | Compile | Run |
|---|---|---|
| **Java 17** | `javac Solution.java` | `java -Xmx200m Solution` |
| **Python 3.11** | — | `python3 solution.py` |
| **C++ (GCC 12)** | `g++ -O2 -std=c++17` | `./solution` |

---

## REST API

| Method | Endpoint | Description |
|---|---|---|
| `POST` | `/api/v1/submissions` | Submit code |
| `GET` | `/api/v1/submissions/{id}` | Poll result |
| `GET` | `/api/v1/submissions` | List (filter by userId / problemId) |
| `GET` | `/api/v1/submissions/queue/status` | Queue depth |
| `POST` | `/api/v1/problems` | Create problem + test cases |
| `GET` | `/api/v1/problems/{id}` | Get problem |
| `GET` | `/api/v1/problems` | List problems |

Interactive docs: **http://localhost:8080/swagger-ui.html**

---

## Quick Start

### Prerequisites
- Java 17+, Maven 3.8+
- Docker Desktop running

### 1. Build sandbox images (one-time)

```bash
cd sandbox
chmod +x build-images.sh
./build-images.sh
```

### 2. Run the backend

```bash
cd backend
mvn spring-boot:run
```

Or with Docker Compose (builds backend image + runs everything):

```bash
# From repo root
docker-compose up --build
```

### 3. Try it

```bash
# Create a problem
curl -X POST http://localhost:8080/api/v1/problems \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Hello World",
    "description": "Print Hello, World!",
    "timeLimitMs": 2000,
    "memoryLimitMb": 256,
    "difficulty": "EASY",
    "testCases": [{"input": "", "expectedOutput": "Hello, World!", "sample": true}]
  }'

# Submit Python solution (use the problemId from above)
curl -X POST http://localhost:8080/api/v1/submissions \
  -H "Content-Type: application/json" \
  -d '{
    "code": "print(\"Hello, World!\")",
    "language": "PYTHON",
    "problemId": "<PROBLEM_ID>"
  }'

# Poll result
curl http://localhost:8080/api/v1/submissions/<SUBMISSION_ID>
```

---

## Verdict Codes

| Verdict | Meaning |
|---|---|
| `PENDING` | Queued, not yet executed |
| `ACCEPTED` | All test cases passed |
| `WRONG_ANSWER` | Output mismatch |
| `TIME_LIMIT_EXCEEDED` | `timeout` exit 124 or wall-clock exceeded |
| `MEMORY_LIMIT_EXCEEDED` | Docker OOM kill (exit 137) |
| `COMPILATION_ERROR` | Compiler error (non-zero exit, stderr non-empty) |
| `RUNTIME_ERROR` | Non-zero exit at runtime |

---

## Project Structure

```
code-judge/
├── backend/                         # Spring Boot application
│   ├── src/main/java/com/codejudge/
│   │   ├── CodeJudgeApplication.java
│   │   ├── config/DataInitializer.java   # Seeds 5 sample problems on startup
│   │   ├── controller/                  # REST controllers
│   │   ├── dto/                         # Request / Response objects
│   │   ├── exception/                   # Exception hierarchy + global handler
│   │   ├── model/                       # JPA entities + enums
│   │   ├── repository/                  # Spring Data JPA repos
│   │   └── service/
│   │       ├── DockerExecutionService   # Docker container lifecycle
│   │       ├── ExecutionQueueService    # LinkedBlockingQueue + worker pool
│   │       ├── JudgeService             # Output diffing
│   │       ├── ProblemService
│   │       ├── SubmissionProcessor      # Transactional execution coordinator
│   │       └── SubmissionService
│   ├── src/main/resources/application.yml
│   ├── pom.xml
│   └── Dockerfile
├── sandbox/
│   ├── java/Dockerfile              # judge-java:latest
│   ├── python/Dockerfile            # judge-python:latest
│   ├── cpp/Dockerfile               # judge-cpp:latest
│   └── build-images.sh
├── docker-compose.yml
└── README.md
```

---

## Design Highlights

- **Queue-based fair scheduling** — `LinkedBlockingQueue<String>` + fixed thread pool ensures no single user monopolizes workers.
- **Sibling container pattern** — Backend container mounts the Docker socket and spawns sibling sandbox containers rather than nesting Docker-in-Docker.
- **Anti-cheat exit code mapping** — Exit 124 → TLE, exit 137 → MLE, non-zero with empty stdout → CE, other non-zero → RE.
- **Concurrent stream draining** — stdout and stderr are read by separate `CompletableFuture` threads to prevent deadlocks on large output.
- **Separation of concerns** — `DockerExecutionService` owns container lifecycle; `JudgeService` owns output comparison; `SubmissionProcessor` orchestrates the flow; `ExecutionQueueService` owns scheduling.
