package com.aegisops.backend.dto;

import java.util.UUID;

public record CreateAlertResponse(
        AlertResponse alert,
        UUID incidentId,
        boolean deduped
) {
}
