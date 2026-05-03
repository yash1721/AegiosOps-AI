package com.aegisops.backend.dto;

import java.time.OffsetDateTime;
import java.util.Map;

public record ErrorResponse(
        String code,
        String message,
        Map<String, String> fieldErrors,
        OffsetDateTime timestamp
) {
}
