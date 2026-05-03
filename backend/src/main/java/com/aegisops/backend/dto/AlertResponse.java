package com.aegisops.backend.dto;

import com.aegisops.backend.enums.Severity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record AlertResponse(
        UUID id,
        UUID incidentId,
        String serviceName,
        String metric,
        BigDecimal value,
        BigDecimal threshold,
        Severity severity,
        String region,
        String fingerprint,
        String rawPayload,
        OffsetDateTime createdAt
) {
}
