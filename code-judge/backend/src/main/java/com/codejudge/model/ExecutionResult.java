package com.codejudge.model;

import com.codejudge.model.enums.Verdict;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExecutionResult {
    private Verdict verdict;
    private String stdout;
    private String stderr;
    private long executionTimeMs;
    private long memoryUsedKb;
    private int exitCode;
    private boolean timedOut;
    private String errorMessage;
}
