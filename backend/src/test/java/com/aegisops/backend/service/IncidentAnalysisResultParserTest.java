package com.aegisops.backend.service;

import com.aegisops.backend.dto.IncidentAnalysisResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IncidentAnalysisResultParserTest {

    private final IncidentAnalysisResultParser parser = new IncidentAnalysisResultParser(new ObjectMapper());

    @Test
    void parsesExpectedJson() {
        IncidentAnalysisResult result = parser.parse("""
                {
                  "summary": "Latency is elevated.",
                  "probableRootCause": "Downstream saturation.",
                  "remediationSteps": ["Check dependency health.", "Scale after approval."],
                  "confidence": 0.82,
                  "needsHumanApproval": true
                }
                """, "test-model");

        assertThat(result.summary()).isEqualTo("Latency is elevated.");
        assertThat(result.remediationSteps()).hasSize(2);
        assertThat(result.confidence()).isEqualTo(0.82d);
        assertThat(result.needsHumanApproval()).isTrue();
    }

    @Test
    void returnsSafeFallbackForMalformedJson() {
        IncidentAnalysisResult result = parser.parse("not json", "bad-model");

        assertThat(result.summary()).contains("could not be parsed");
        assertThat(result.confidence()).isLessThan(0.3d);
        assertThat(result.needsHumanApproval()).isTrue();
    }
}
