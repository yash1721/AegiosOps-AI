package com.aegisops.backend.service;

import com.aegisops.backend.client.EmbeddingClient;
import com.aegisops.backend.dto.RetrievedContextChunk;
import com.aegisops.backend.entity.Alert;
import com.aegisops.backend.entity.Incident;
import com.aegisops.backend.entity.Runbook;
import com.aegisops.backend.entity.RunbookChunk;
import com.aegisops.backend.enums.IncidentStatus;
import com.aegisops.backend.enums.Severity;
import com.aegisops.backend.repository.RunbookChunkRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RunbookRetrievalServiceTest {

    @Mock
    private EmbeddingClient embeddingClient;

    @Mock
    private RunbookChunkRepository runbookChunkRepository;

    @Test
    void semanticRetrievalPrefersSameServiceChunks() {
        Incident incident = incident();
        RetrievedContextChunk otherService = new RetrievedContextChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "billing",
                "Billing CPU",
                "Billing guidance",
                0.99d,
                "qdrant"
        );
        RetrievedContextChunk sameService = new RetrievedContextChunk(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "checkout",
                "Checkout CPU",
                "Checkout guidance",
                0.80d,
                "qdrant"
        );
        when(embeddingClient.embed(any())).thenReturn(List.of(0.1d, 0.2d));
        RunbookRetrievalService retrievalService = new RunbookRetrievalService(
                embeddingClient,
                new StubQdrantClientService(List.of(otherService, sameService), false),
                runbookChunkRepository
        );

        List<RetrievedContextChunk> results = retrievalService.retrieveContextForIncident(incident, List.of(alert()), 2);

        assertThat(results).extracting(RetrievedContextChunk::serviceName)
                .containsExactly("checkout", "billing");
    }

    @Test
    void keywordFallbackReturnsPostgresChunksWhenVectorSearchFails() {
        Incident incident = incident();
        RunbookRetrievalService retrievalService = new RunbookRetrievalService(
                embeddingClient,
                new StubQdrantClientService(List.of(), true),
                runbookChunkRepository
        );
        Runbook runbook = new Runbook();
        runbook.setId(UUID.randomUUID());
        runbook.setServiceName("checkout");
        runbook.setTitle("Checkout CPU remediation");
        runbook.setContent("Restart checkout on cpu_usage breach.");
        runbook.setCreatedAt(OffsetDateTime.now());

        RunbookChunk chunk = new RunbookChunk();
        chunk.setId(UUID.randomUUID());
        chunk.setRunbook(runbook);
        chunk.setServiceName("checkout");
        chunk.setChunkText("Restart checkout when cpu_usage exceeds threshold.");
        chunk.setChunkIndex(0);
        chunk.setCreatedAt(OffsetDateTime.now());

        when(embeddingClient.embed(any())).thenThrow(new IllegalStateException("ollama down"));
        when(runbookChunkRepository.searchByKeywordPreferService(any(), any())).thenReturn(List.of(chunk));

        List<RetrievedContextChunk> results = retrievalService.retrieveContextForIncident(incident, List.of(alert()), 3);

        assertThat(results).hasSize(1);
        assertThat(results.getFirst().source()).isEqualTo("keyword");
        assertThat(results.getFirst().serviceName()).isEqualTo("checkout");
    }

    private Incident incident() {
        Incident incident = new Incident();
        incident.setId(UUID.randomUUID());
        incident.setTitle("SEV1 checkout cpu_usage breach in us-east-1");
        incident.setServiceName("checkout");
        incident.setSeverity(Severity.SEV1);
        incident.setStatus(IncidentStatus.OPEN);
        incident.setDedupKey("checkout:cpu_usage:SEV1:us-east-1");
        incident.setStartedAt(OffsetDateTime.now());
        return incident;
    }

    private Alert alert() {
        Alert alert = new Alert();
        alert.setId(UUID.randomUUID());
        alert.setServiceName("checkout");
        alert.setMetric("cpu_usage");
        alert.setValue(BigDecimal.valueOf(99.5d));
        alert.setThreshold(BigDecimal.valueOf(90));
        alert.setSeverity(Severity.SEV1);
        alert.setRegion("us-east-1");
        alert.setCreatedAt(OffsetDateTime.now());
        return alert;
    }

    private static final class StubQdrantClientService extends QdrantClientService {

        private final List<RetrievedContextChunk> results;
        private final boolean fail;

        private StubQdrantClientService(List<RetrievedContextChunk> results, boolean fail) {
            super(RestClient.builder(), "http://localhost:6333", "test", 384);
            this.results = results;
            this.fail = fail;
        }

        @Override
        public List<RetrievedContextChunk> search(List<Double> queryVector, int topK) {
            if (fail) {
                throw new IllegalStateException("qdrant down");
            }
            return results;
        }
    }
}
