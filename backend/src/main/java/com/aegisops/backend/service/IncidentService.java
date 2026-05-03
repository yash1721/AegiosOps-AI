package com.aegisops.backend.service;

import com.aegisops.backend.dto.AlertResponse;
import com.aegisops.backend.dto.ApprovalResponse;
import com.aegisops.backend.dto.ApproveIncidentRequest;
import com.aegisops.backend.dto.IncidentDetailResponse;
import com.aegisops.backend.dto.IncidentResponse;
import com.aegisops.backend.entity.Approval;
import com.aegisops.backend.entity.Incident;
import com.aegisops.backend.enums.ApprovalStatus;
import com.aegisops.backend.enums.IncidentStatus;
import com.aegisops.backend.exception.ResourceNotFoundException;
import com.aegisops.backend.mapper.IncidentManagementMapper;
import com.aegisops.backend.repository.AlertRepository;
import com.aegisops.backend.repository.ApprovalRepository;
import com.aegisops.backend.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class IncidentService {

    private final IncidentRepository incidentRepository;
    private final AlertRepository alertRepository;
    private final ApprovalRepository approvalRepository;
    private final AuditLogService auditLogService;

    public IncidentService(
            IncidentRepository incidentRepository,
            AlertRepository alertRepository,
            ApprovalRepository approvalRepository,
            AuditLogService auditLogService
    ) {
        this.incidentRepository = incidentRepository;
        this.alertRepository = alertRepository;
        this.approvalRepository = approvalRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public List<IncidentResponse> listIncidents() {
        return incidentRepository.findAllByOrderByStartedAtDesc()
                .stream()
                .map(IncidentManagementMapper::toIncidentResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public IncidentDetailResponse getIncident(UUID incidentId) {
        Incident incident = getIncidentOrThrow(incidentId);
        List<AlertResponse> alerts = alertRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
                .stream()
                .map(IncidentManagementMapper::toAlertResponse)
                .toList();
        List<ApprovalResponse> approvals = approvalRepository.findByIncidentIdOrderByCreatedAtDesc(incidentId)
                .stream()
                .map(IncidentManagementMapper::toApprovalResponse)
                .toList();
        return IncidentManagementMapper.toIncidentDetailResponse(incident, alerts, approvals);
    }

    @Transactional
    public ApprovalResponse approveIncident(UUID incidentId, ApproveIncidentRequest request) {
        Incident incident = getIncidentOrThrow(incidentId);
        Approval approval = new Approval();
        approval.setIncident(incident);
        approval.setActionType(request.actionType());
        approval.setStatus(ApprovalStatus.APPROVED);
        approval.setApprovedBy(request.approvedBy());
        Approval savedApproval = approvalRepository.save(approval);

        incident.setStatus(IncidentStatus.APPROVED);
        incidentRepository.save(incident);

        auditLogService.record(
                "INCIDENT_APPROVED",
                "INCIDENT",
                incident.getId(),
                "{\"approvalId\":\"" + savedApproval.getId() + "\",\"approvedBy\":\"" + request.approvedBy() + "\"}"
        );

        return IncidentManagementMapper.toApprovalResponse(savedApproval);
    }

    @Transactional
    public IncidentResponse resolveIncident(UUID incidentId) {
        Incident incident = getIncidentOrThrow(incidentId);
        incident.setStatus(IncidentStatus.RESOLVED);
        incident.setResolvedAt(OffsetDateTime.now());
        Incident savedIncident = incidentRepository.save(incident);

        auditLogService.record(
                "INCIDENT_RESOLVED",
                "INCIDENT",
                savedIncident.getId(),
                "{\"resolvedAt\":\"" + savedIncident.getResolvedAt() + "\"}"
        );

        return IncidentManagementMapper.toIncidentResponse(savedIncident);
    }

    private Incident getIncidentOrThrow(UUID incidentId) {
        return incidentRepository.findById(incidentId)
                .orElseThrow(() -> new ResourceNotFoundException("Incident not found: " + incidentId));
    }
}
