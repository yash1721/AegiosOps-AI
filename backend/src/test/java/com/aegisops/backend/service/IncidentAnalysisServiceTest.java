package com.aegisops.backend.service;

import com.aegisops.backend.client.LlmProvider;
import com.aegisops.backend.client.MockLlmProvider;
import com.aegisops.backend.dto.IncidentAnalysisResponse;
import com.aegisops.backend.dto.IncidentAnalysisResult;
import com.aegisops.backend.dto.RetrievedContextChunk;
import com.aegisops.backend.entity.AIRecommendation;
import com.aegisops.backend.entity.Alert;
import com.aegisops.backend.entity.AuditLog;
import com.aegisops.backend.entity.Incident;
import com.aegisops.backend.enums.IncidentStatus;
import com.aegisops.backend.enums.Severity;
import com.aegisops.backend.repository.AIRecommendationRepository;
import com.aegisops.backend.repository.AlertRepository;
import com.aegisops.backend.repository.IncidentRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IncidentAnalysisServiceTest {

    @Mock
    private IncidentRepository incidentRepository;

    @Mock
    private AlertRepository alertRepository;

    @Mock
    private AIRecommendationRepository aiRecommendationRepository;

    @Test
    void analyzeStoresRecommendationUpdatesStatusAndCreatesCompletedAuditLog() {
        Incident incident = incident();
        Alert alert = alert(incident);
        CapturingAuditLogService auditLogService = new CapturingAuditLogService();
        IncidentAnalysisService service = service(
                prompt -> new IncidentAnalysisResult(
                        "Checkout has elevated error rate.",
                        "Recent errors indicate a likely application regression.",
                        List.of("Inspect error logs.", "Rollback after approval."),
                        0.82d,
                        true,
                        "test-model",
                        "{\"summary\":\"ok\"}"
                ),
                auditLogService
        );

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(alertRepository.findByIncidentIdOrderByCreatedAtDesc(incident.getId())).thenReturn(List.of(alert));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiRecommendationRepository.save(any(AIRecommendation.class))).thenAnswer(invocation -> persistedRecommendation(invocation.getArgument(0)));

        IncidentAnalysisResponse response = service.analyzeIncident(incident.getId());

        assertThat(response.incidentId()).isEqualTo(incident.getId());
        assertThat(response.status()).isEqualTo("ANALYZED");
        assertThat(response.modelUsed()).isEqualTo("test-model");
        assertThat(response.remediationSteps()).hasSize(2);
        assertThat(incident.getStatus()).isEqualTo(IncidentStatus.ANALYZED);
        assertThat(auditLogService.actions()).contains("AI_ANALYSIS_COMPLETED");
    }

    @Test
    void analyzeFallsBackToMockProviderWhenConfiguredProviderFails() {
        Incident incident = incident();
        Alert alert = alert(incident);
        CapturingAuditLogService auditLogService = new CapturingAuditLogService();
        IncidentAnalysisService service = service(
                prompt -> {
                    throw new IllegalStateException("provider down");
                },
                auditLogService
        );

        when(incidentRepository.findById(incident.getId())).thenReturn(Optional.of(incident));
        when(alertRepository.findByIncidentIdOrderByCreatedAtDesc(incident.getId())).thenReturn(List.of(alert));
        when(incidentRepository.save(any(Incident.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(aiRecommendationRepository.save(any(AIRecommendation.class))).thenAnswer(invocation -> persistedRecommendation(invocation.getArgument(0)));

        IncidentAnalysisResponse response = service.analyzeIncident(incident.getId());

        assertThat(response.modelUsed()).isEqualTo("mock");
        assertThat(response.summary()).contains("Error rate");
        assertThat(response.status()).isEqualTo("ANALYZED");
        assertThat(auditLogService.actions()).contains("AI_ANALYSIS_FAILED", "AI_ANALYSIS_COMPLETED");
    }

    private IncidentAnalysisService service(LlmProvider llmProvider, CapturingAuditLogService auditLogService) {
        return new IncidentAnalysisService(
                incidentRepository,
                alertRepository,
                new StubRunbookRetrievalService(),
                llmProvider,
                new MockLlmProvider(),
                aiRecommendationRepository,
                auditLogService,
                new ObjectMapper()
        );
    }

    private AIRecommendation persistedRecommendation(AIRecommendation recommendation) {
        recommendation.setId(UUID.randomUUID());
        recommendation.setCreatedAt(OffsetDateTime.now());
        return recommendation;
    }

    private Incident incident() {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setTitle("SEV1 checkout error_rate breach in us-east-1");
        incident.setServiceName("checkout");
        incident.setSeverity(Severity.SEV1);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setDedupKey("checkout:error_rate:SEV1:us-east-1");
        incident.setStartedAt(OffsetDateTime.now());
        return incident;
    }

    private Alert alert(Incident incident) {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setIncident(incident);
        alert.setServiceName("checkout");
        alert.setMetric("error_rate");
        alert.setValue(BigDecimal.valueOf(12.5d));
        alert.setThreshold(BigDecimal.valueOf(5));
        alert.setSeverity(Severity.SEV1);
        alert.setRegion("us-east-1");
        alert.setCreatedAt(OffsetDateTime.now());
        return alert;
    }

    private static final class StubRunbookRetrievalService extends RunbookRetrievalService {

        private StubRunbookRetrievalService() {
            super(null, null, null);
        }

        @Override
        public List<RetrievedContextChunk> retrieveContextForIncident(Incident incident, List<Alert> alerts, int topK) {
            return List.of(new RetrievedContextChunk(
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    "checkout",
                    "Checkout errors",
                    "Inspect error logs and rollback recent deployments if error_rate remains elevated.",
                    0.9d,
                    "test"
            ));
        }
    }

    private static final class CapturingAuditLogService extends AuditLogService {

        private final List<String> actions = new ArrayList<>();

        private CapturingAuditLogService() {
            super(null);
        }

        @Override
        public AuditLog record(String action, String entityType, UUID entityId, String metadata) {
            actions.add(action);
            AuditLog auditLog = new AuditLog();
            auditLog.setId(UUID.randomUUID());
            auditLog.setAction(action);
            auditLog.setEntityType(entityType);
            auditLog.setEntityId(entityId);
            auditLog.setMetadata(metadata);
            auditLog.setCreatedAt(OffsetDateTime.now());
            return auditLog;
        }

        private List<String> actions() {
            return actions;
        }
    }
}
