package com.aegisops.backend.client;

import com.aegisops.backend.dto.IncidentAnalysisResult;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

@Component
public class MockLlmProvider implements LlmProvider {

    @Override
    public IncidentAnalysisResult analyzeIncident(String prompt) {
        String normalized = prompt == null ? "" : prompt.toLowerCase(Locale.ROOT);
        if (normalized.contains("p99_latency")) {
            return result(
                    "Latency is elevated for the affected service.",
                    "The strongest signal is p99_latency breaching threshold, likely due to downstream latency, saturation, or a recent deployment.",
                    List.of("Check recent deployments and rollback candidates.", "Inspect downstream latency and saturation.", "Scale the service only after confirming demand or saturation."),
                    0.78d
            );
        }
        if (normalized.contains("error_rate")) {
            return result(
                    "Error rate is elevated for the affected service.",
                    "The strongest signal is error_rate breaching threshold, likely caused by application errors, dependency failures, or bad configuration.",
                    List.of("Inspect recent error logs and exception groups.", "Check dependency health and timeout rates.", "Rollback or disable the suspected change after human approval."),
                    0.76d
            );
        }
        if (normalized.contains("queue_lag")) {
            return result(
                    "Queue lag is increasing for the affected service.",
                    "The strongest signal is queue_lag, likely caused by slow consumers, insufficient workers, or downstream backpressure.",
                    List.of("Check consumer health and processing rate.", "Scale consumers if downstream systems are healthy.", "Pause noncritical producers if lag keeps growing."),
                    0.74d
            );
        }
        if (normalized.contains("db_cpu")) {
            return result(
                    "Database CPU is elevated and may be affecting service health.",
                    "The strongest signal is db_cpu, likely caused by expensive queries, missing indexes, or increased traffic.",
                    List.of("Inspect top queries and query plans.", "Check connection pool saturation.", "Apply query mitigation or traffic shaping after approval."),
                    0.75d
            );
        }
        return result(
                "The incident has limited evidence for a precise root cause.",
                "Available incident, alert, and runbook context is insufficient for a confident root cause.",
                List.of("Gather additional metrics and logs.", "Compare against recent deployments.", "Keep remediation conservative until evidence improves."),
                0.35d
        );
    }

    private IncidentAnalysisResult result(
            String summary,
            String probableRootCause,
            List<String> remediationSteps,
            double confidence
    ) {
        return new IncidentAnalysisResult(
                summary,
                probableRootCause,
                remediationSteps,
                confidence,
                true,
                "mock",
                null
        );
    }
}
