package com.aegisops.backend.service;

import com.aegisops.backend.dto.RetrievedContextChunk;
import com.aegisops.backend.entity.Runbook;
import com.aegisops.backend.entity.RunbookChunk;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.StreamSupport;

@Service
public class QdrantClientService {

    private final RestClient restClient;
    private final String collection;
    private final int vectorSize;

    public QdrantClientService(
            RestClient.Builder restClientBuilder,
            @Value("${aegisops.qdrant.url:http://localhost:6333}") String qdrantUrl,
            @Value("${aegisops.qdrant.collection:aegisops_runbooks}") String collection,
            @Value("${aegisops.qdrant.vector-size:384}") int vectorSize
    ) {
        this.restClient = restClientBuilder.baseUrl(qdrantUrl).build();
        this.collection = collection;
        this.vectorSize = vectorSize;
    }

    public void createCollectionIfMissing() {
        try {
            restClient.get()
                    .uri("/collections/{collection}", collection)
                    .retrieve()
                    .toBodilessEntity();
            return;
        } catch (RestClientResponseException exception) {
            if (exception.getStatusCode().value() != 404) {
                throw exception;
            }
        }

        Map<String, Object> vectors = Map.of(
                "size", vectorSize,
                "distance", "Cosine"
        );
        restClient.put()
                .uri("/collections/{collection}", collection)
                .body(Map.of("vectors", vectors))
                .retrieve()
                .toBodilessEntity();
    }

    public UUID upsertChunk(Runbook runbook, RunbookChunk chunk, List<Double> vector) {
        UUID pointId = UUID.randomUUID();
        Map<String, Object> payload = new HashMap<>();
        payload.put("runbookId", runbook.getId().toString());
        payload.put("chunkId", chunk.getId().toString());
        payload.put("serviceName", chunk.getServiceName());
        payload.put("title", runbook.getTitle());
        payload.put("chunkText", chunk.getChunkText());

        Map<String, Object> point = Map.of(
                "id", pointId.toString(),
                "vector", vector,
                "payload", payload
        );

        restClient.put()
                .uri("/collections/{collection}/points?wait=true", collection)
                .body(Map.of("points", List.of(point)))
                .retrieve()
                .toBodilessEntity();
        return pointId;
    }

    public List<RetrievedContextChunk> search(List<Double> queryVector, int topK) {
        JsonNode response = restClient.post()
                .uri("/collections/{collection}/points/search", collection)
                .body(Map.of(
                        "vector", queryVector,
                        "limit", topK,
                        "with_payload", true
                ))
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("result")) {
            return List.of();
        }

        List<RetrievedContextChunk> chunks = new ArrayList<>();
        for (JsonNode result : StreamSupport.stream(response.get("result").spliterator(), false).toList()) {
            JsonNode payload = result.get("payload");
            if (payload == null) {
                continue;
            }
            chunks.add(new RetrievedContextChunk(
                    UUID.fromString(payload.path("runbookId").asText()),
                    UUID.fromString(payload.path("chunkId").asText()),
                    payload.path("serviceName").asText(),
                    payload.path("title").asText(),
                    payload.path("chunkText").asText(),
                    result.path("score").asDouble(),
                    "qdrant"
            ));
        }
        return chunks;
    }
}
