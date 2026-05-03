package com.aegisops.backend.dto;

import com.aegisops.backend.enums.ApprovalStatus;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ApprovalResponse(
        UUID id,
        UUID incidentId,
        String actionType,
        ApprovalStatus status,
        String approvedBy,
        OffsetDateTime createdAt
) {
}
