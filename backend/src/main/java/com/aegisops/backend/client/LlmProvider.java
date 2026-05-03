package com.aegisops.backend.client;

import com.aegisops.backend.dto.IncidentAnalysisResult;

public interface LlmProvider {

    IncidentAnalysisResult analyzeIncident(String prompt);
}
