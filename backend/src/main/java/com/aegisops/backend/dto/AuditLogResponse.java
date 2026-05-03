package com.aegisops.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditLogResponse(
        UUID id,
        String action,
        String entityType,
        UUID entityId,
        String metadata,
        OffsetDateTime createdAt
) {
}
