package com.codejudge.dto;

import com.codejudge.model.enums.Language;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Code submission request")
public class SubmissionRequest {

    @NotBlank(message = "Code cannot be empty")
    @Size(max = 10000, message = "Code cannot exceed 10,000 characters")
    @Schema(description = "Source code to execute", example = "print('Hello, World!')")
    private String code;

    @NotNull(message = "Language is required")
    @Schema(description = "Programming language", example = "PYTHON")
    private Language language;

    @NotBlank(message = "Problem ID is required")
    @Schema(description = "ID of the problem to solve")
    private String problemId;

    @Schema(description = "Optional user identifier")
    private String userId;
}
