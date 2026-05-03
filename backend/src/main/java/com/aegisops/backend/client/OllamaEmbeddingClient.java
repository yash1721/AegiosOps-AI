package com.aegisops.backend.client;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

@Component
@ConditionalOnProperty(name = "aegisops.embedding.provider", havingValue = "ollama")
public class OllamaEmbeddingClient implements EmbeddingClient {

    private final RestClient restClient;
    private final String embeddingModel;

    public OllamaEmbeddingClient(
            RestClient.Builder restClientBuilder,
            @Value("${aegisops.llm.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${aegisops.llm.ollama.embedding-model:nomic-embed-text}") String embeddingModel
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.embeddingModel = embeddingModel;
    }

    @Override
    public List<Double> embed(String text) {
        JsonNode response = restClient.post()
                .uri("/api/embeddings")
                .body(Map.of("model", embeddingModel, "prompt", text))
                .retrieve()
                .body(JsonNode.class);

        if (response == null || !response.has("embedding")) {
            throw new IllegalStateException("Ollama embedding response did not include an embedding");
        }

        return StreamSupport.stream(response.get("embedding").spliterator(), false)
                .map(JsonNode::asDouble)
                .toList();
    }
}
