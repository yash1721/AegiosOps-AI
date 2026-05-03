package com.aegisops.backend.dto;

import com.aegisops.backend.enums.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreateAlertRequest(
        @NotBlank String serviceName,
        @NotBlank String metric,
        @NotNull BigDecimal value,
        @NotNull BigDecimal threshold,
        @NotNull Severity severity,
        @NotBlank String region,
        String fingerprint,
        String rawPayload
) {
}
