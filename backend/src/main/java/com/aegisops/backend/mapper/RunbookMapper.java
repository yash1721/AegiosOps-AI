package com.aegisops.backend.mapper;

import com.aegisops.backend.dto.RunbookChunkResponse;
import com.aegisops.backend.dto.RunbookResponse;
import com.aegisops.backend.entity.Runbook;
import com.aegisops.backend.entity.RunbookChunk;

import java.util.List;

public final class RunbookMapper {

    private RunbookMapper() {
    }

    public static RunbookResponse toRunbookResponse(
            Runbook runbook,
            List<RunbookChunk> chunks,
            boolean indexed,
            String indexingError
    ) {
        return new RunbookResponse(
                runbook.getId(),
                runbook.getServiceName(),
                runbook.getTitle(),
                runbook.getContent(),
                runbook.getCreatedAt(),
                indexed,
                indexingError,
                chunks.stream().map(RunbookMapper::toRunbookChunkResponse).toList()
        );
    }

    public static RunbookResponse toRunbookListResponse(Runbook runbook) {
        return new RunbookResponse(
                runbook.getId(),
                runbook.getServiceName(),
                runbook.getTitle(),
                null,
                runbook.getCreatedAt(),
                false,
                null,
                List.of()
        );
    }

    public static RunbookChunkResponse toRunbookChunkResponse(RunbookChunk chunk) {
        return new RunbookChunkResponse(
                chunk.getId(),
                chunk.getRunbook().getId(),
                chunk.getServiceName(),
                chunk.getChunkText(),
                chunk.getChunkIndex(),
                chunk.getQdrantPointId(),
                chunk.getCreatedAt()
        );
    }
}
