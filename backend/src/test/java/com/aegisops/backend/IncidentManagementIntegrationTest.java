package com.aegisops.backend;

import com.aegisops.backend.repository.AlertRepository;
import com.aegisops.backend.repository.ApprovalRepository;
import com.aegisops.backend.repository.AuditLogRepository;
import com.aegisops.backend.repository.IncidentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@AutoConfigureMockMvc
class IncidentManagementIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("aegisops")
            .withUsername("postgres")
            .withPassword("postgres");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private ApprovalRepository approvalRepository;

    @Autowired
    private AlertRepository alertRepository;

    @Autowired
    private IncidentRepository incidentRepository;

    @DynamicPropertySource
    static void registerDatasourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @BeforeEach
    void cleanDatabase() {
        auditLogRepository.deleteAll();
        approvalRepository.deleteAll();
        alertRepository.deleteAll();
        incidentRepository.deleteAll();
    }

    @Test
    void healthEndpointStillWorks() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("UP")))
                .andExpect(jsonPath("$.service", is("aegisops-backend")));
    }

    @Test
    void alertIngestionCreatesIncidentAndAuditLogs() throws Exception {
        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertPayload("fp-1", "us-east-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deduped", is(false)))
                .andExpect(jsonPath("$.incidentId", notNullValue()))
                .andExpect(jsonPath("$.alert.id", notNullValue()))
                .andExpect(jsonPath("$.alert.serviceName", is("checkout")))
                .andExpect(jsonPath("$.alert.metric", is("cpu_usage")))
                .andExpect(jsonPath("$.alert.severity", is("SEV1")));

        mockMvc.perform(get("/api/v1/incidents"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].status", is("OPEN")))
                .andExpect(jsonPath("$[0].dedupKey", is("checkout:cpu_usage:SEV1:us-east-1")));

        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].action", containsInAnyOrder("ALERT_RECEIVED", "INCIDENT_CREATED")));
    }

    @Test
    void duplicateAlertDeduplicatesIntoSameIncident() throws Exception {
        String firstResponse = mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertPayload("fp-1", "us-east-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deduped", is(false)))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String incidentId = firstResponse.replaceAll(".*\"incidentId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertPayload("fp-2", "us-east-1")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.deduped", is(true)))
                .andExpect(jsonPath("$.incidentId", is(incidentId)));

        mockMvc.perform(get("/api/v1/incidents/{incidentId}", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(incidentId)))
                .andExpect(jsonPath("$.alerts", hasSize(2)));

        mockMvc.perform(get("/api/v1/audit-logs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[*].action", containsInAnyOrder(
                        "ALERT_RECEIVED",
                        "INCIDENT_CREATED",
                        "ALERT_RECEIVED",
                        "INCIDENT_DEDUPED"
                )));
    }

    @Test
    void approveRemediationChangesIncidentStatusToApproved() throws Exception {
        String incidentId = createIncident();

        mockMvc.perform(post("/api/v1/incidents/{incidentId}/approve", incidentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "approvedBy": "ops@example.com",
                                  "actionType": "RESTART_SERVICE"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId", is(incidentId)))
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.approvedBy", is("ops@example.com")))
                .andExpect(jsonPath("$.actionType", is("RESTART_SERVICE")));

        mockMvc.perform(get("/api/v1/incidents/{incidentId}", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("APPROVED")))
                .andExpect(jsonPath("$.approvals", hasSize(1)));
    }

    @Test
    void resolveIncidentChangesStatusToResolved() throws Exception {
        String incidentId = createIncident();

        mockMvc.perform(post("/api/v1/incidents/{incidentId}/resolve", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(incidentId)))
                .andExpect(jsonPath("$.status", is("RESOLVED")))
                .andExpect(jsonPath("$.resolvedAt", notNullValue()));

        mockMvc.perform(get("/api/v1/incidents/{incidentId}", incidentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("RESOLVED")))
                .andExpect(jsonPath("$.resolvedAt", notNullValue()));
    }

    @Test
    void validationErrorsReturnBadRequest() throws Exception {
        mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "metric": "cpu_usage",
                                  "severity": "SEV1",
                                  "region": "us-east-1",
                                  "value": 99.5,
                                  "threshold": 90
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code", is("VALIDATION_FAILED")))
                .andExpect(jsonPath("$.fieldErrors.serviceName", notNullValue()));
    }

    private String createIncident() throws Exception {
        String response = mockMvc.perform(post("/api/v1/alerts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(alertPayload("fp-1", "us-east-1")))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return response.replaceAll(".*\"incidentId\":\"([^\"]+)\".*", "$1");
    }

    private String alertPayload(String fingerprint, String region) {
        return """
                {
                  "serviceName": "checkout",
                  "metric": "cpu_usage",
                  "value": 99.5,
                  "threshold": 90,
                  "severity": "SEV1",
                  "region": "%s",
                  "fingerprint": "%s",
                  "rawPayload": "{\\"source\\":\\"test\\"}"
                }
                """.formatted(region, fingerprint);
    }
}
