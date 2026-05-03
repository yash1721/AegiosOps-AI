package com.aegisops.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record IncidentAnalysisResponse(
        UUID recommendationId,
        UUID incidentId,
        String status,
        String summary,
        String probableRootCause,
        List<RemediationStep> remediationSteps,
        double confidence,
        boolean needsHumanApproval,
        String modelUsed,
        OffsetDateTime createdAt
) {
}
