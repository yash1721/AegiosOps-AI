package com.aegisops.backend.controller;

import com.aegisops.backend.dto.ApprovalResponse;
import com.aegisops.backend.dto.ApproveIncidentRequest;
import com.aegisops.backend.dto.IncidentDetailResponse;
import com.aegisops.backend.dto.IncidentResponse;
import com.aegisops.backend.service.IncidentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/incidents")
public class IncidentController {

    private final IncidentService incidentService;

    public IncidentController(IncidentService incidentService) {
        this.incidentService = incidentService;
    }

    @GetMapping
    public List<IncidentResponse> listIncidents() {
        return incidentService.listIncidents();
    }

    @GetMapping("/{incidentId}")
    public IncidentDetailResponse getIncident(@PathVariable UUID incidentId) {
        return incidentService.getIncident(incidentId);
    }

    @PostMapping("/{incidentId}/approve")
    public ApprovalResponse approveIncident(
            @PathVariable UUID incidentId,
            @Valid @RequestBody ApproveIncidentRequest request
    ) {
        return incidentService.approveIncident(incidentId, request);
    }

    @PostMapping("/{incidentId}/resolve")
    public IncidentResponse resolveIncident(@PathVariable UUID incidentId) {
        return incidentService.resolveIncident(incidentId);
    }
}
