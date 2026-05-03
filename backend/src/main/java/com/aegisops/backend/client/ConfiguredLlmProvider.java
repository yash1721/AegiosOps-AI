package com.aegisops.backend.client;

import com.aegisops.backend.dto.IncidentAnalysisResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
@Primary
public class ConfiguredLlmProvider implements LlmProvider {

    private final String provider;
    private final MockLlmProvider mockLlmProvider;
    private final OllamaLlmProvider ollamaLlmProvider;
    private final OpenAiCompatibleLlmProvider openAiCompatibleLlmProvider;

    public ConfiguredLlmProvider(
            @Value("${aegisops.llm.provider:mock}") String provider,
            MockLlmProvider mockLlmProvider,
            OllamaLlmProvider ollamaLlmProvider,
            OpenAiCompatibleLlmProvider openAiCompatibleLlmProvider
    ) {
        this.provider = provider;
        this.mockLlmProvider = mockLlmProvider;
        this.ollamaLlmProvider = ollamaLlmProvider;
        this.openAiCompatibleLlmProvider = openAiCompatibleLlmProvider;
    }

    @Override
    public IncidentAnalysisResult analyzeIncident(String prompt) {
        return switch (provider.toLowerCase(Locale.ROOT)) {
            case "ollama" -> ollamaLlmProvider.analyzeIncident(prompt);
            case "openai", "openai-compatible" -> openAiCompatibleLlmProvider.analyzeIncident(prompt);
            case "mock" -> mockLlmProvider.analyzeIncident(prompt);
            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + provider);
        };
    }
}
