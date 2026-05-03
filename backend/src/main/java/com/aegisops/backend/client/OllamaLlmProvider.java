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
public class OllamaLlmProvider implements LlmProvider {

    private final RestClient restClient;
    private final String chatModel;
    private final IncidentAnalysisResultParser parser;

    public OllamaLlmProvider(
            RestClient.Builder restClientBuilder,
            IncidentAnalysisResultParser parser,
            @Value("${aegisops.llm.ollama.base-url:http://localhost:11434}") String baseUrl,
            @Value("${aegisops.llm.ollama.chat-model:llama3.1}") String chatModel
    ) {
        this.restClient = restClientBuilder.baseUrl(baseUrl).build();
        this.parser = parser;
        this.chatModel = chatModel;
    }

    @Override
    public IncidentAnalysisResult analyzeIncident(String prompt) {
        JsonNode response = restClient.post()
                .uri("/api/chat")
                .body(Map.of(
                        "model", chatModel,
                        "stream", false,
                        "messages", List.of(Map.of("role", "user", "content", prompt))
                ))
                .retrieve()
                .body(JsonNode.class);

        String raw = response == null ? "" : response.path("message").path("content").asText();
        return parser.parse(raw, "ollama:" + chatModel);
    }
}
