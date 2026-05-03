package com.aegisops.backend.controller;

import com.aegisops.backend.dto.CreateRunbookRequest;
import com.aegisops.backend.dto.RunbookResponse;
import com.aegisops.backend.service.RunbookService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class RunbookControllerTest {

    @Test
    void createRunbookApiWorks() throws Exception {
        UUID runbookId = UUID.randomUUID();
        MockMvc mockMvc = mockMvc(response(runbookId));

        mockMvc.perform(post("/api/v1/runbooks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "serviceName": "checkout",
                                  "title": "Checkout CPU remediation",
                                  "content": "Restart checkout when cpu_usage exceeds threshold."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", is(runbookId.toString())))
                .andExpect(jsonPath("$.serviceName", is("checkout")))
                .andExpect(jsonPath("$.indexed", is(true)));
    }

    @Test
    void listRunbooksApiWorks() throws Exception {
        UUID runbookId = UUID.randomUUID();
        MockMvc mockMvc = mockMvc(response(runbookId));

        mockMvc.perform(get("/api/v1/runbooks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id", is(runbookId.toString())))
                .andExpect(jsonPath("$[0].title", is("Checkout CPU remediation")));
    }

    @Test
    void getRunbookApiWorks() throws Exception {
        UUID runbookId = UUID.randomUUID();
        MockMvc mockMvc = mockMvc(response(runbookId));

        mockMvc.perform(get("/api/v1/runbooks/{runbookId}", runbookId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(runbookId.toString())))
                .andExpect(jsonPath("$.content", is("Restart checkout when cpu_usage exceeds threshold.")));
    }

    private MockMvc mockMvc(RunbookResponse response) {
        return MockMvcBuilders.standaloneSetup(new RunbookController(new StubRunbookService(response))).build();
    }

    private RunbookResponse response(UUID runbookId) {
        return new RunbookResponse(
                runbookId,
                "checkout",
                "Checkout CPU remediation",
                "Restart checkout when cpu_usage exceeds threshold.",
                OffsetDateTime.now(),
                true,
                null,
                List.of()
        );
    }

    private static final class StubRunbookService extends RunbookService {

        private final RunbookResponse response;

        private StubRunbookService(RunbookResponse response) {
            super(null, null, null, null);
            this.response = response;
        }

        @Override
        public RunbookResponse createRunbook(CreateRunbookRequest request) {
            return response;
        }

        @Override
        public List<RunbookResponse> listRunbooks() {
            return List.of(response);
        }

        @Override
        public RunbookResponse getRunbook(UUID runbookId) {
            return response;
        }
    }
}
