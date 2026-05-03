package com.aegisops.backend.service;

import com.aegisops.backend.client.EmbeddingClient;
import com.aegisops.backend.dto.RetrievedContextChunk;
import com.aegisops.backend.entity.Alert;
import com.aegisops.backend.entity.Incident;
import com.aegisops.backend.entity.RunbookChunk;
import com.aegisops.backend.repository.RunbookChunkRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Service
public class RunbookRetrievalService {

    private final EmbeddingClient embeddingClient;
    private final QdrantClientService qdrantClientService;
    private final RunbookChunkRepository runbookChunkRepository;

    public RunbookRetrievalService(
            EmbeddingClient embeddingClient,
            QdrantClientService qdrantClientService,
            RunbookChunkRepository runbookChunkRepository
    ) {
        this.embeddingClient = embeddingClient;
        this.qdrantClientService = qdrantClientService;
        this.runbookChunkRepository = runbookChunkRepository;
    }

    @Transactional(readOnly = true)
    public List<RetrievedContextChunk> retrieveContextForIncident(Incident incident, List<Alert> alerts, int topK) {
        String query = buildQuery(incident, alerts);
        try {
            List<Double> embedding = embeddingClient.embed(query);
            List<RetrievedContextChunk> semanticResults = qdrantClientService.search(embedding, Math.max(topK * 2, topK));
            List<RetrievedContextChunk> preferred = preferSameService(semanticResults, incident.getServiceName(), topK);
            if (!preferred.isEmpty()) {
                return preferred;
            }
        } catch (RuntimeException ignored) {
            // Keyword fallback keeps analysis usable when the vector path is unavailable.
        }
        return keywordFallback(incident, alerts, topK);
    }

    private String buildQuery(Incident incident, List<Alert> alerts) {
        String metrics = alerts == null
                ? ""
                : alerts.stream()
                .map(Alert::getMetric)
                .filter(Objects::nonNull)
                .distinct()
                .reduce("", (left, right) -> left.isBlank() ? right : left + " " + right);
        return "%s %s %s %s".formatted(
                incident.getTitle(),
                incident.getServiceName(),
                incident.getSeverity(),
                metrics
        ).trim();
    }

    private List<RetrievedContextChunk> preferSameService(
            List<RetrievedContextChunk> chunks,
            String serviceName,
            int topK
    ) {
        return chunks.stream()
                .sorted(Comparator
                        .comparing((RetrievedContextChunk chunk) -> !chunk.serviceName().equalsIgnoreCase(serviceName))
                        .thenComparing(RetrievedContextChunk::score, Comparator.reverseOrder()))
                .limit(topK)
                .toList();
    }

    private List<RetrievedContextChunk> keywordFallback(Incident incident, List<Alert> alerts, int topK) {
        Map<String, RunbookChunk> matches = new LinkedHashMap<>();
        for (String term : fallbackTerms(incident, alerts)) {
            for (RunbookChunk chunk : runbookChunkRepository.searchByKeywordPreferService(term, incident.getServiceName())) {
                matches.putIfAbsent(chunk.getId().toString(), chunk);
                if (matches.size() >= topK) {
                    return toRetrievedChunks(matches.values().stream().toList());
                }
            }
        }
        return toRetrievedChunks(matches.values().stream().limit(topK).toList());
    }

    private List<String> fallbackTerms(Incident incident, List<Alert> alerts) {
        Map<String, String> terms = new LinkedHashMap<>();
        terms.put(incident.getServiceName().toLowerCase(Locale.ROOT), incident.getServiceName());
        if (incident.getTitle() != null) {
            for (String token : incident.getTitle().split("\\s+")) {
                if (token.length() >= 3) {
                    terms.put(token.toLowerCase(Locale.ROOT), token);
                }
            }
        }
        if (alerts != null) {
            alerts.stream()
                    .map(Alert::getMetric)
                    .filter(Objects::nonNull)
                    .filter(metric -> metric.length() >= 3)
                    .forEach(metric -> terms.put(metric.toLowerCase(Locale.ROOT), metric));
        }
        return terms.values().stream().toList();
    }

    private List<RetrievedContextChunk> toRetrievedChunks(List<RunbookChunk> chunks) {
        return chunks.stream()
                .map(chunk -> new RetrievedContextChunk(
                        chunk.getRunbook().getId(),
                        chunk.getId(),
                        chunk.getServiceName(),
                        chunk.getRunbook().getTitle(),
                        chunk.getChunkText(),
                        0.0d,
                        "keyword"
                ))
                .toList();
    }
}
