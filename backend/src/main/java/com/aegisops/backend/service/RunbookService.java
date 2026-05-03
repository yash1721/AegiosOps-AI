package com.aegisops.backend.service;

import com.aegisops.backend.client.EmbeddingClient;
import com.aegisops.backend.dto.CreateRunbookRequest;
import com.aegisops.backend.dto.RunbookResponse;
import com.aegisops.backend.entity.Runbook;
import com.aegisops.backend.entity.RunbookChunk;
import com.aegisops.backend.exception.ResourceNotFoundException;
import com.aegisops.backend.mapper.RunbookMapper;
import com.aegisops.backend.repository.RunbookChunkRepository;
import com.aegisops.backend.repository.RunbookRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class RunbookService {

    private static final int TARGET_CHUNK_SIZE = 700;
    private static final int MAX_CHUNK_SIZE = 800;

    private final RunbookRepository runbookRepository;
    private final RunbookChunkRepository runbookChunkRepository;
    private final EmbeddingClient embeddingClient;
    private final QdrantClientService qdrantClientService;

    public RunbookService(
            RunbookRepository runbookRepository,
            RunbookChunkRepository runbookChunkRepository,
            EmbeddingClient embeddingClient,
            QdrantClientService qdrantClientService
    ) {
        this.runbookRepository = runbookRepository;
        this.runbookChunkRepository = runbookChunkRepository;
        this.embeddingClient = embeddingClient;
        this.qdrantClientService = qdrantClientService;
    }

    @Transactional
    public RunbookResponse createRunbook(CreateRunbookRequest request) {
        Runbook runbook = new Runbook();
        runbook.setServiceName(request.serviceName().trim());
        runbook.setTitle(request.title().trim());
        runbook.setContent(request.content().trim());
        Runbook savedRunbook = runbookRepository.save(runbook);

        List<RunbookChunk> chunks = saveChunks(savedRunbook, splitIntoChunks(savedRunbook.getContent()));
        String indexingError = indexChunks(savedRunbook, chunks);
        boolean indexed = indexingError == null;

        return RunbookMapper.toRunbookResponse(savedRunbook, chunks, indexed, indexingError);
    }

    @Transactional(readOnly = true)
    public List<RunbookResponse> listRunbooks() {
        return runbookRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(RunbookMapper::toRunbookListResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public RunbookResponse getRunbook(UUID runbookId) {
        Runbook runbook = runbookRepository.findById(runbookId)
                .orElseThrow(() -> new ResourceNotFoundException("Runbook not found: " + runbookId));
        List<RunbookChunk> chunks = runbookChunkRepository.findByRunbookIdOrderByChunkIndexAsc(runbookId);
        boolean indexed = chunks.stream().allMatch(chunk -> chunk.getQdrantPointId() != null);
        return RunbookMapper.toRunbookResponse(runbook, chunks, indexed, null);
    }

    List<String> splitIntoChunks(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.isEmpty()) {
            return List.of();
        }
        if (normalized.length() <= MAX_CHUNK_SIZE) {
            return List.of(normalized);
        }

        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String word : normalized.split(" ")) {
            if (current.isEmpty()) {
                current.append(word);
                continue;
            }

            int nextLength = current.length() + 1 + word.length();
            if (nextLength > MAX_CHUNK_SIZE || current.length() >= TARGET_CHUNK_SIZE) {
                chunks.add(current.toString());
                current = new StringBuilder(word);
            } else {
                current.append(' ').append(word);
            }
        }
        if (!current.isEmpty()) {
            chunks.add(current.toString());
        }
        return chunks;
    }

    private List<RunbookChunk> saveChunks(Runbook runbook, List<String> chunkTexts) {
        List<RunbookChunk> chunks = new ArrayList<>();
        for (int index = 0; index < chunkTexts.size(); index++) {
            RunbookChunk chunk = new RunbookChunk();
            chunk.setRunbook(runbook);
            chunk.setServiceName(runbook.getServiceName());
            chunk.setChunkText(chunkTexts.get(index));
            chunk.setChunkIndex(index);
            chunks.add(runbookChunkRepository.save(chunk));
        }
        return chunks;
    }

    private String indexChunks(Runbook runbook, List<RunbookChunk> chunks) {
        try {
            qdrantClientService.createCollectionIfMissing();
            for (RunbookChunk chunk : chunks) {
                List<Double> embedding = embeddingClient.embed(chunk.getChunkText());
                UUID pointId = qdrantClientService.upsertChunk(runbook, chunk, embedding);
                chunk.setQdrantPointId(pointId);
                runbookChunkRepository.save(chunk);
            }
            return null;
        } catch (RuntimeException exception) {
            return exception.getClass().getSimpleName() + ": " + exception.getMessage();
        }
    }
}
