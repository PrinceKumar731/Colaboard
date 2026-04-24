package com.codejudge.service;

import com.codejudge.model.ExecutionResult;
import com.codejudge.model.Submission;
import com.codejudge.model.TestCase;
import com.codejudge.model.enums.SubmissionStatus;
import com.codejudge.model.enums.Verdict;
import com.codejudge.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Processes a single submission: runs it against all test cases and persists the verdict.
 * Lives in its own bean so @Transactional is applied via the Spring proxy
 * (calling from ExecutionQueueService crosses the bean boundary).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionProcessor {

    private final SubmissionRepository submissionRepository;
    private final DockerExecutionService dockerExecutionService;
    private final JudgeService judgeService;

    @Transactional
    public void process(String submissionId) {
        Submission submission = submissionRepository.findById(submissionId).orElse(null);
        if (submission == null) {
            log.warn("Submission {} not found — skipping", submissionId);
            return;
        }

        submission.setStatus(SubmissionStatus.RUNNING);
        submissionRepository.save(submission);

        try {
            List<TestCase> testCases = submission.getProblem().getTestCases();

            if (testCases.isEmpty()) {
                log.warn("Problem {} has no test cases", submission.getProblem().getId());
                finalize(submission, Verdict.ACCEPTED, null);
                return;
            }

            Verdict finalVerdict = Verdict.ACCEPTED;
            ExecutionResult lastResult = null;

            for (TestCase tc : testCases) {
                ExecutionResult result = dockerExecutionService.execute(
                        submission.getCode(),
                        submission.getLanguage(),
                        tc.getInput(),
                        submission.getProblem().getTimeLimitMs(),
                        submission.getProblem().getMemoryLimitMb()
                );
                lastResult = result;
                Verdict verdict = judgeService.judge(result, tc);

                if (verdict != Verdict.ACCEPTED) {
                    finalVerdict = verdict;
                    break;
                }
            }

            finalize(submission, finalVerdict, lastResult);

        } catch (Exception e) {
            log.error("Unhandled error for submission {}: {}", submissionId, e.getMessage(), e);
            submission.setStatus(SubmissionStatus.FAILED);
            submission.setVerdict(Verdict.RUNTIME_ERROR);
            submission.setErrorMessage("Internal error: " + e.getMessage());
            submissionRepository.save(submission);
        }
    }

    private void finalize(Submission submission, Verdict verdict, ExecutionResult result) {
        submission.setStatus(SubmissionStatus.COMPLETED);
        submission.setVerdict(verdict);
        if (result != null) {
            submission.setExecutionTimeMs(result.getExecutionTimeMs());
            submission.setMemoryUsedKb(result.getMemoryUsedKb());
            submission.setStdout(result.getStdout());
            submission.setStderr(result.getStderr());
            submission.setErrorMessage(result.getErrorMessage());
        }
        submissionRepository.save(submission);
        log.info("Submission {} → {} in {}ms",
                submission.getId(), verdict, result != null ? result.getExecutionTimeMs() : 0);
    }
}
