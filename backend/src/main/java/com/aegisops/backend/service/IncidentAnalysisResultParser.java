package com.aegisops.backend.service;

import com.aegisops.backend.dto.IncidentAnalysisResult;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class IncidentAnalysisResultParser {

    private final ObjectMapper objectMapper;

    public IncidentAnalysisResultParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public IncidentAnalysisResult parse(String rawResponse, String modelUsed) {
        try {
            JsonNode root = objectMapper.readTree(extractJson(rawResponse));
            return new IncidentAnalysisResult(
                    textOrFallback(root, "summary", "Analysis completed, but the model response was incomplete."),
                    textOrFallback(root, "probableRootCause", "Evidence was insufficient for a precise root cause."),
                    remediationSteps(root),
                    clamp(root.path("confidence").asDouble(0.25d)),
                    root.path("needsHumanApproval").asBoolean(true),
                    modelUsed,
                    rawResponse
            );
        } catch (RuntimeException exception) {
            return safeFallback(rawResponse, modelUsed);
        } catch (Exception exception) {
            return safeFallback(rawResponse, modelUsed);
        }
    }

    public IncidentAnalysisResult safeFallback(String rawResponse, String modelUsed) {
        return new IncidentAnalysisResult(
                "Analysis response could not be parsed safely.",
                "The model did not return valid structured JSON, so no precise root cause should be inferred.",
                List.of("Review incident alerts and runbook context manually.", "Retry analysis with a healthy provider.", "Require human approval before remediation."),
                0.2d,
                true,
                modelUsed,
                rawResponse
        );
    }

    private String extractJson(String rawResponse) {
        if (rawResponse == null) {
            return "{}";
        }
        int start = rawResponse.indexOf('{');
        int end = rawResponse.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return rawResponse.substring(start, end + 1);
        }
        return rawResponse;
    }

    private String textOrFallback(JsonNode root, String field, String fallback) {
        String value = root.path(field).asText();
        return value == null || value.isBlank() ? fallback : value;
    }

    private List<String> remediationSteps(JsonNode root) {
        JsonNode stepsNode = root.path("remediationSteps");
        List<String> steps = new ArrayList<>();
        if (stepsNode.isArray()) {
            for (JsonNode step : stepsNode) {
                String value = step.asText();
                if (!value.isBlank()) {
                    steps.add(value);
                }
            }
        }
        if (steps.isEmpty()) {
            return List.of("Review available evidence manually before taking action.");
        }
        return steps;
    }

    private double clamp(double value) {
        return Math.max(0.0d, Math.min(1.0d, value));
    }
}
