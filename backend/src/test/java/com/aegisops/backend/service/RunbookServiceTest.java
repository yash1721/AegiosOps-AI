package com.aegisops.backend.service;

import com.aegisops.backend.client.EmbeddingClient;
import com.aegisops.backend.dto.CreateRunbookRequest;
import com.aegisops.backend.dto.RunbookResponse;
import com.aegisops.backend.entity.Runbook;
import com.aegisops.backend.entity.RunbookChunk;
import com.aegisops.backend.repository.RunbookChunkRepository;
import com.aegisops.backend.repository.RunbookRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunbookServiceTest {

    @Mock
    private RunbookRepository runbookRepository;

    @Mock
    private RunbookChunkRepository runbookChunkRepository;

    @Mock
    private EmbeddingClient embeddingClient;

    @Test
    void uploadSavesRunbookChunksAndIndexesVectors() {
        StubQdrantClientService qdrantClientService = new StubQdrantClientService(false);
        RunbookService runbookService = new RunbookService(
                runbookRepository,
                runbookChunkRepository,
                embeddingClient,
                qdrantClientService
        );
        when(runbookRepository.save(any(Runbook.class))).thenAnswer(invocation -> persistedRunbook(invocation.getArgument(0)));
        when(runbookChunkRepository.save(any(RunbookChunk.class))).thenAnswer(invocation -> persistedChunk(invocation.getArgument(0)));
        when(embeddingClient.embed(any())).thenReturn(List.of(0.1d, 0.2d, 0.3d));

        RunbookResponse response = runbookService.createRunbook(new CreateRunbookRequest(
                "checkout",
                "Checkout CPU remediation",
                longRunbookContent()
        ));

        assertThat(response.id()).isNotNull();
        assertThat(response.indexed()).isTrue();
        assertThat(response.indexingError()).isNull();
        assertThat(response.chunks()).hasSizeGreaterThan(1);
        assertThat(response.chunks()).allSatisfy(chunk -> {
            assertThat(chunk.chunkText()).hasSizeLessThanOrEqualTo(800);
            assertThat(chunk.qdrantPointId()).isNotNull();
        });
        assertThat(qdrantClientService.createCollectionCalled).isTrue();
    }

    @Test
    void uploadStillSucceedsWhenQdrantFails() {
        RunbookService runbookService = new RunbookService(
                runbookRepository,
                runbookChunkRepository,
                embeddingClient,
                new StubQdrantClientService(true)
        );
        when(runbookRepository.save(any(Runbook.class))).thenAnswer(invocation -> persistedRunbook(invocation.getArgument(0)));
        when(runbookChunkRepository.save(any(RunbookChunk.class))).thenAnswer(invocation -> persistedChunk(invocation.getArgument(0)));

        RunbookResponse response = runbookService.createRunbook(new CreateRunbookRequest(
                "checkout",
                "Checkout CPU remediation",
                "Restart checkout when cpu_usage exceeds threshold."
        ));

        assertThat(response.id()).isNotNull();
        assertThat(response.chunks()).hasSize(1);
        assertThat(response.indexed()).isFalse();
        assertThat(response.indexingError()).contains("qdrant down");
    }

    @Test
    void shortContentCreatesOneChunk() {
        RunbookService runbookService = new RunbookService(
                runbookRepository,
                runbookChunkRepository,
                embeddingClient,
                new StubQdrantClientService(false)
        );
        List<String> chunks = runbookService.splitIntoChunks("Restart checkout service.");

        assertThat(chunks).containsExactly("Restart checkout service.");
    }

    @Test
    void getRunbookReturnsChunks() {
        RunbookService runbookService = new RunbookService(
                runbookRepository,
                runbookChunkRepository,
                embeddingClient,
                new StubQdrantClientService(false)
        );
        Runbook runbook = new Runbook();
        runbook.setId(UUID.randomUUID());
        runbook.setServiceName("checkout");
        runbook.setTitle("Checkout CPU remediation");
        runbook.setContent("Restart checkout service.");
        runbook.setCreatedAt(OffsetDateTime.now());

        RunbookChunk chunk = new RunbookChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setRunbook(runbook);
        chunk.setServiceName("checkout");
        chunk.setChunkText("Restart checkout service.");
        chunk.setChunkIndex(0);
        chunk.setQdrantPointId(UUID.randomUUID());
        chunk.setCreatedAt(OffsetDateTime.now());

        when(runbookRepository.findById(runbook.getId())).thenReturn(Optional.of(runbook));
        when(runbookChunkRepository.findByRunbookIdOrderByChunkIndexAsc(runbook.getId())).thenReturn(List.of(chunk));

        RunbookResponse response = runbookService.getRunbook(runbook.getId());

        assertThat(response.id()).isEqualTo(runbook.getId());
        assertThat(response.indexed()).isTrue();
        assertThat(response.chunks()).hasSize(1);
    }

    private Runbook persistedRunbook(Runbook runbook) {
        runbook.setId(UUID.randomUUID());
        runbook.setCreatedAt(OffsetDateTime.now());
        return runbook;
    }

    private RunbookChunk persistedChunk(RunbookChunk chunk) {
        if (chunk.getId() == null) {
            chunk.setId(UUID.randomUUID());
        }
        if (chunk.getCreatedAt() == null) {
            chunk.setCreatedAt(OffsetDateTime.now());
        }
        return chunk;
    }

    private String longRunbookContent() {
        return """
                When checkout cpu_usage exceeds threshold, first confirm whether traffic increased.
                Inspect recent deployments, database latency, cache hit rate, and pod restart count.
                If the issue is isolated to checkout, restart one pod at a time and watch error rate.
                If the issue affects all regions, disable noncritical background jobs and escalate to payments.
                Record the remediation steps in the incident audit log and keep the incident open until latency recovers.
                Repeat this guidance with enough operational detail to force multiple chunks during tests.
                When checkout cpu_usage exceeds threshold, first confirm whether traffic increased.
                Inspect recent deployments, database latency, cache hit rate, and pod restart count.
                If the issue is isolated to checkout, restart one pod at a time and watch error rate.
                If the issue affects all regions, disable noncritical background jobs and escalate to payments.
                """;
    }

    private static final class StubQdrantClientService extends QdrantClientService {

        private final boolean fail;
        private boolean createCollectionCalled;

        private StubQdrantClientService(boolean fail) {
            super(RestClient.builder(), "http://localhost:6333", "test", 384);
            this.fail = fail;
        }

        @Override
        public void createCollectionIfMissing() {
            createCollectionCalled = true;
            if (fail) {
                throw new IllegalStateException("qdrant down");
            }
        }

        @Override
        public UUID upsertChunk(Runbook runbook, RunbookChunk chunk, List<Double> vector) {
            return UUID.randomUUID();
        }
    }
}
