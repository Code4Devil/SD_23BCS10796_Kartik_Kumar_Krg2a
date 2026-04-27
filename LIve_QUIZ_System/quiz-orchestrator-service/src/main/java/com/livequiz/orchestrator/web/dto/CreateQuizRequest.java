package com.livequiz.orchestrator.web.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record CreateQuizRequest(
        @NotBlank String title,
        @NotBlank String hostId,
        @NotEmpty List<QuestionDto> questions
) {
    public record QuestionDto(
            @NotBlank String text,
            @Size(min = 2, max = 10) List<String> options,
            @Min(0) int correctOptionIndex,
            @Min(5) @Max(300) int timeLimitSeconds
    ) {}
}

