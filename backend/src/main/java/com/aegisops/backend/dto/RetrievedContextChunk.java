package com.aegisops.backend.dto;

import java.util.UUID;

public record RetrievedContextChunk(
        UUID runbookId,
        UUID chunkId,
        String serviceName,
        String title,
        String chunkText,
        double score,
        String source
) {
}
