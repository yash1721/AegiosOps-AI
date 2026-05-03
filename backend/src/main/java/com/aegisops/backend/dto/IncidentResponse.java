package com.aegisops.backend.dto;

import com.aegisops.backend.enums.IncidentStatus;
import com.aegisops.backend.enums.Severity;

import java.time.OffsetDateTime;
import java.util.UUID;

public record IncidentResponse(
        UUID id,
        String title,
        String serviceName,
        Severity severity,
        IncidentStatus status,
        String dedupKey,
        OffsetDateTime startedAt,
        OffsetDateTime resolvedAt
) {
}
