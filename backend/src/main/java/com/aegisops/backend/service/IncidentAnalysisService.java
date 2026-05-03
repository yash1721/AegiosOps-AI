package com.aegisops.backend.service;

import com.aegisops.backend.client.LlmProvider;
import com.aegisops.backend.client.MockLlmProvider;
import com.aegisops.backend.dto.IncidentAnalysisResponse;
import com.aegisops.backend.dto.IncidentAnalysisResult;
import com.aegisops.backend.dto.RemediationStep;
import com.aegisops.backend.dto.RetrievedContextChunk;
import com.aegisops.backend.entity.AIRecommendation;
import com.aegisops.backend.entity.Alert;
import com.aegisops.backend.entity.Incident;
import com.aegisops.backend.enums.IncidentStatus;
import com.aegisops.backend.exception.ResourceNotFoundException;
import com.aegisops.backend.repository.AIRecommendationRepository;
import com.aegisops.backend.repository.AlertRepository;
import com.aegisops.backend.repository.IncidentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@Service
public class IncidentAnalysisService {

    private static final int TOP_K_CONTEXT_CHUNKS = 5;

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final RunbookRetrievalService runbookRetrievalService;
    private final LlmProvider llmProvider;
    private final MockLlmProvider mockLlmProvider;
    private final AIRecommendationRepository aiRecommendationRepository;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    public IncidentAnalysisService(
            IncidentRepository incidentRepository,
            AlertRepository alertRepository,
            RunbookRetrievalService runbookRetrievalService,
            LlmProvider llmProvider,
            MockLlmProvider mockLlmProvider,
            AIRecommendationRepository aiRecommendationRepository,
            AuditLogService auditLogService,
            ObjectMapper objectMapper
    ) {
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.runbookRetrievalService = runbookRetrievalService;
        this.llmProvider = llmProvider;
        this.mockLlmProvider = mockLlmProvider;
        this.aiRecommendationRepository = aiRecommendationRepository;
        this.auditLogService = auditLogService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public IncidentAnalysisResponse analyzeIncident(UUID incidentId) {
        Incident incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));

        incident.setStatus(IncidentStatus.ANALYZING);
        incidentRepository.save(incident);

        List<Alert> alerts = alertRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId);
        List<RetrievedContextChunk> contextChunks = runbookRetrievalService
                .retrieveContextForIncident(incident, alerts, TOP_K_CONTEXT_CHUNKS);
        String prompt = buildPrompt(incident, alerts, contextChunks);

        IncidentAnalysisResult result;
        try {
            result = llmProvider.analyzeIncident(prompt);
        } catch (RuntimeException exception) {
            auditLogService.record(
                    "AI_ANALYSIS_FAILED",
                    "INCIDENT",
                    incident.getId(),
                    "{\"error\":\"" + sanitize(exception.getMessage()) + "\"}"
            );
            result = mockLlmProvider.analyzeIncident(prompt);
        }

        AIRecommendation recommendation = saveRecommendation(incident, result);
        incident.setStatus(IncidentStatus.ANALYZED);
        incidentRepository.save(incident);

        auditLogService.record(
                "AI_ANALYSIS_COMPLETED",
                "INCIDENT",
                incident.getId(),
                "{\"recommendationId\":\"" + recommendation.getId() + "\",\"modelUsed\":\"" + sanitize(result.modelUsed()) + "\"}"
        );

        return toResponse(recommendation, incident);
    }

    private String buildPrompt(Incident incident, List<Alert> alerts, List<RetrievedContextChunk> contextChunks) {
        return """
                You are an incident commander assistant for AegisOps.
                Use only the incident, alerts, and runbook context below.
                Return JSON only. Do not include markdown.
                Do not invent facts.
                Use low confidence if evidence is insufficient.
                Remediation requires human approval.

                Expected JSON:
                {
                  "summary": "...",
                  "probableRootCause": "...",
                  "remediationSteps": ["...", "..."],
                  "confidence": 0.82,
                  "needsHumanApproval": true
                }

                Incident:
                id=%s
                title=%s
                serviceName=%s
                severity=%s
                status=%s

                Alerts:
                %s

                Runbook context:
                %s
                """.formatted(
                incident.getId(),
                incident.getTitle(),
                incident.getServiceName(),
                incident.getSeverity(),
                incident.getStatus(),
                alertContext(alerts),
                runbookContext(contextChunks)
        );
    }

    private String alertContext(List<Alert> alerts) {
        if (alerts.isEmpty()) {
            return "No related alerts.";
        }
        return alerts.stream()
                .map(alert -> "- metric=%s value=%s threshold=%s severity=%s region=%s fingerprint=%s".formatted(
                        alert.getMetric(),
                        alert.getValue(),
                        alert.getThreshold(),
                        alert.getSeverity(),
                        alert.getRegion(),
                        alert.getFingerprint()
                ))
                .reduce("", (left, right) -> left.isBlank() ? right : left + "\n" + right);
    }

    private String runbookContext(List<RetrievedContextChunk> contextChunks) {
        if (contextChunks.isEmpty()) {
            return "No runbook context retrieved.";
        }
        return contextChunks.stream()
                .map(chunk -> "- runbookId=%s chunkId=%s serviceName=%s title=%s source=%s text=%s".formatted(
                        chunk.runbookId(),
                        chunk.chunkId(),
                        chunk.serviceName(),
                        chunk.title(),
                        chunk.source(),
                        chunk.chunkText()
                ))
                .reduce("", (left, right) -> left.isBlank() ? right : left + "\n" + right);
    }

    private AIRecommendation saveRecommendation(Incident incident, IncidentAnalysisResult result) {
        AIRecommendation recommendation = new AIRecommendation();
        recommendation.setIncident(incident);
        recommendation.setSummary(result.summary());
        recommendation.setProbableRootCause(result.probableRootCause());
        recommendation.setRemediationSteps(writeJson(result.remediationSteps()));
        recommendation.setConfidence(result.confidence());
        recommendation.setNeedsHumanApproval(result.needsHumanApproval());
        recommendation.setModelUsed(result.modelUsed());
        recommendation.setRawResponse(result.rawResponse());
        return aiRecommendationRepository.save(recommendation);
    }

    private IncidentAnalysisResponse toResponse(AIRecommendation recommendation, Incident incident) {
        List<String> steps = readSteps(recommendation.getRemediationSteps());
        List<RemediationStep> remediationSteps = IntStream.range(0, steps.size())
                .mapToObj(index -> new RemediationStep(index + 1, steps.get(index)))
                .toList();
        return new IncidentAnalysisResponse(
                recommendation.getId(),
                incident.getId(),
                incident.getStatus().name(),
                recommendation.getSummary(),
                recommendation.getProbableRootCause(),
                remediationSteps,
                recommendation.getConfidence(),
                recommendation.getNeedsHumanApproval(),
                recommendation.getModelUsed(),
                recommendation.getCreatedAt()
        );
    }

    private String writeJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            return "[]";
        }
    }

    private List<String> readSteps(String json) {
        try {
            return objectMapper.readerForListOf(String.class).readValue(json);
        } catch (Exception exception) {
            return List.of("Review recommendation manually.");
        }
    }

    private String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
