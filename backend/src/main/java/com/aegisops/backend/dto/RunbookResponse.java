package com.aegisops.backend.dto;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public record RunbookResponse(
        UUID id,
        String serviceName,
        String title,
        String content,
        OffsetDateTime createdAt,
        boolean indexed,
        String indexingError,
        List<RunbookChunkResponse> chunks
) {
}
