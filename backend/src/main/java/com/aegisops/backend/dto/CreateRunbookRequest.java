package com.aegisops.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record CreateRunbookRequest(
        @NotBlank String serviceName,
        @NotBlank String title,
        @NotBlank String content
) {
}
