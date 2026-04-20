package com.codejudge.dto;

import com.codejudge.model.Submission;
import com.codejudge.model.enums.SubmissionStatus;
import com.codejudge.model.enums.Verdict;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Code submission result")
public class SubmissionResponse {

    private String id;
    private String problemId;
    private String language;
    private SubmissionStatus status;
    private Verdict verdict;
    private Long executionTimeMs;
    private Long memoryUsedKb;
    private String stdout;
    private String stderr;
    private String errorMessage;
    private LocalDateTime createdAt;

    public static SubmissionResponse from(Submission s) {
        return SubmissionResponse.builder()
                .id(s.getId())
                .problemId(s.getProblem() != null ? s.getProblem().getId() : null)
                .language(s.getLanguage().name())
                .status(s.getStatus())
                .verdict(s.getVerdict())
                .executionTimeMs(s.getExecutionTimeMs())
                .memoryUsedKb(s.getMemoryUsedKb())
                .stdout(s.getStdout())
                .stderr(s.getStderr())
                .errorMessage(s.getErrorMessage())
                .createdAt(s.getCreatedAt())
                .build();
    }
}
