package com.codejudge.controller;

import com.codejudge.dto.SubmissionRequest;
import com.codejudge.dto.SubmissionResponse;
import com.codejudge.service.ExecutionQueueService;
import com.codejudge.service.SubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/submissions")
@RequiredArgsConstructor
@Tag(name = "Submissions", description = "Submit code and poll results")
public class SubmissionController {

    private final SubmissionService submissionService;
    private final ExecutionQueueService executionQueueService;

    @PostMapping
    @Operation(summary = "Submit code for judgment")
    public ResponseEntity<SubmissionResponse> submit(@Valid @RequestBody SubmissionRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(submissionService.submit(request));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Poll submission status and result")
    public ResponseEntity<SubmissionResponse> getSubmission(@PathVariable String id) {
        return ResponseEntity.ok(submissionService.getSubmission(id));
    }

    @GetMapping
    @Operation(summary = "List submissions (optionally filtered by userId or problemId)")
    public ResponseEntity<Page<SubmissionResponse>> listSubmissions(
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) String problemId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        return ResponseEntity.ok(submissionService.listSubmissions(userId, problemId, pageable));
    }

    @GetMapping("/queue/status")
    @Operation(summary = "Get current execution queue depth")
    public ResponseEntity<Map<String, Object>> queueStatus() {
        return ResponseEntity.ok(Map.of("queueDepth", executionQueueService.getQueueDepth()));
    }
}
