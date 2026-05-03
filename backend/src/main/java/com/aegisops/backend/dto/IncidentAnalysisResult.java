package com.aegisops.backend.dto;

import java.util.List;

public record IncidentAnalysisResult(
        String summary,
        String probableRootCause,
        List<String> remediationSteps,
        double confidence,
        boolean needsHumanApproval,
        String modelUsed,
        String rawResponse
) {
}
