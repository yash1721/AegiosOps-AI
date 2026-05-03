package com.aegisops.backend.client;

import com.aegisops.backend.dto.IncidentAnalysisResult;
import com.aegisops.backend.service.IncidentAnalysisResultParser;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class OpenAiCompatibleLlmProvider implements LlmProvider {

    private final RestClient restClient;
    private final IncidentAnalysisResultParser parser;
    private final String apiKey;
    private final String model;

    public OpenAiCompatibleLlmProvider(
            RestClient.Builder restClientBuilder,
            IncidentAnalysisResultParser parser,
            @Value("${aegisops.llm.openai.base-url:https://api.openai.com/v1}") String baseUrl,
            @Value("${aegisops.llm.openai.api-key:}") String apiKey,
            @Value("${aegisops.llm.openai.chat-model:gpt-4o-mini}") String model
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.parser = parser;
        this.apiKey = apiKey;
        this.model = model;
    }

    @Override
    public IncidentAnalysisResult analyzeIncident(String prompt) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY is required for openai-compatible provider");
        }

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .body(Map.of(
                        "model", model,
                        "messages", List.of(Map.of("role", "user", "content", prompt)),
                        "temperature", 0
                ))
                .retrieve()
                .body(JsonNode.class);

        String raw = response == null
                ? ""
                : response.path("choices").path(0).path("message").path("content").asText();
        return parser.parse(raw, "openai-compatible:" + model);
    }
}
