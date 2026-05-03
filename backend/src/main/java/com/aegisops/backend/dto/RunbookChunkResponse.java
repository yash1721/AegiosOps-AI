package com.aegisops.backend.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record RunbookChunkResponse(
        UUID id,
        UUID runbookId,
        String serviceName,
        String chunkText,
        Integer chunkIndex,
        UUID qdrantPointId,
        OffsetDateTime createdAt
) {
}
