package com.codejudge.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Problem definition with test cases")
public class ProblemDto {

    private String id;

    @NotBlank(message = "Title is required")
    private String title;

    @NotBlank(message = "Description is required")
    private String description;

    @Positive
    @Builder.Default
    private int timeLimitMs = 2000;

    @Positive
    @Builder.Default
    private int memoryLimitMb = 256;

    private String difficulty;

    private List<TestCaseDto> testCases;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TestCaseDto {
        @NotBlank
        private String input;
        @NotBlank
        private String expectedOutput;
        private boolean sample;
    }
}
