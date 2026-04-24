package com.codejudge.service;

import com.codejudge.dto.SubmissionRequest;
import com.codejudge.dto.SubmissionResponse;
import com.codejudge.exception.ResourceNotFoundException;
import com.codejudge.model.Problem;
import com.codejudge.model.Submission;
import com.codejudge.model.enums.SubmissionStatus;
import com.codejudge.model.enums.Verdict;
import com.codejudge.repository.ProblemRepository;
import com.codejudge.repository.SubmissionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubmissionService {

    private final SubmissionRepository submissionRepository;
    private final ProblemRepository problemRepository;
    private final ExecutionQueueService executionQueueService;

    @Transactional
    public SubmissionResponse submit(SubmissionRequest request) {
        Problem problem = problemRepository.findById(request.getProblemId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Problem not found: " + request.getProblemId()));

        Submission submission = Submission.builder()
                .code(request.getCode())
                .language(request.getLanguage())
                .problem(problem)
                .userId(request.getUserId())
                .status(SubmissionStatus.QUEUED)
                .verdict(Verdict.PENDING)
                .build();

        submission = submissionRepository.save(submission);
        executionQueueService.enqueue(submission.getId());

        log.info("Accepted submission {} for problem {}", submission.getId(), problem.getId());
        return SubmissionResponse.from(submission);
    }

    @Transactional(readOnly = true)
    public SubmissionResponse getSubmission(String id) {
        return submissionRepository.findById(id)
                .map(SubmissionResponse::from)
                .orElseThrow(() -> new ResourceNotFoundException("Submission not found: " + id));
    }

    @Transactional(readOnly = true)
    public Page<SubmissionResponse> listSubmissions(String userId, String problemId, Pageable pageable) {
        Page<Submission> page;
        if (userId != null) {
            page = submissionRepository.findByUserId(userId, pageable);
        } else if (problemId != null) {
            page = submissionRepository.findByProblemId(problemId, pageable);
        } else {
            page = submissionRepository.findAll(pageable);
        }
        return page.map(SubmissionResponse::from);
    }
}
