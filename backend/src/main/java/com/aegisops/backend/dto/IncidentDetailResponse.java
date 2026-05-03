package com.aegisops.backend.dto;

import com.aegisops.backend.enums.IncidentStatus;
import com.aegisops.backend.enums.Severity;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record IncidentDetailResponse(
        UUID id,
        String title,
        String serviceName,
        Severity severity,
        IncidentStatus status,
        String dedupKey,
        OffsetDateTime startedAt,
        OffsetDateTime resolvedAt,
        List<AlertResponse> alerts,
        List<ApprovalResponse> approvals
) {
}
