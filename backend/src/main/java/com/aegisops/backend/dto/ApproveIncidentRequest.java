package com.aegisops.backend.dto;

import jakarta.validation.constraints.NotBlank;

public record ApproveIncidentRequest(
        @NotBlank String approvedBy,
        @NotBlank String actionType
) {
}
