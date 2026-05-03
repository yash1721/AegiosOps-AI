package com.aegisops.backend.controller;

import com.aegisops.backend.dto.IncidentAnalysisResponse;
import com.aegisops.backend.dto.RemediationStep;
import com.aegisops.backend.service.IncidentAnalysisService;
import com.aegisops.backend.service.IncidentService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IncidentAnalysisControllerTest {

    @Test
    void analyzeEndpointReturnsIncidentAnalysis() throws Exception {
        UUID incidentId = UUID.randomUUID();
        UUID recommendationId = UUID.randomUUID();
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new IncidentController(
                        new StubIncidentService(),
                        new StubIncidentAnalysisService(incidentId, recommendationId)
                ))
                .build();

        mockMvc.perform(post("/api/v1/incidents/{incidentId}/analyze", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.recommendationId", is(recommendationId.toString())))
                .andExpect(jsonPath("$.incidentId", is(incidentId.toString())))
                .andExpect(jsonPath("$.status", is("ANALYZED")))
                .andExpect(jsonPath("$.needsHumanApproval", is(true)))
                .andExpect(jsonPath("$.remediationSteps[0].description", is("Inspect error logs.")));
    }

    private static final class StubIncidentAnalysisService extends IncidentAnalysisService {

        private final UUID incidentId;
        private final UUID recommendationId;

        private StubIncidentAnalysisService(UUID incidentId, UUID recommendationId) {
            super(null, null, null, null, null, null, null, null);
            this.incidentId = incidentId;
            this.recommendationId = recommendationId;
        }

        @Override
        public IncidentAnalysisResponse analyzeIncident(UUID incidentId) {
            return new IncidentAnalysisResponse(
                    recommendationId,
                    this.incidentId,
                    "ANALYZED",
                    "Checkout has elevated error rate.",
                    "Recent errors indicate a likely application regression.",
                    List.of(new RemediationStep(1, "Inspect error logs.")),
                    0.82d,
                    true,
                    "mock",
                    OffsetDateTime.now()
            );
        }
    }

    private static final class StubIncidentService extends IncidentService {

        private StubIncidentService() {
            super(null, null, null, null);
        }
    }
}
