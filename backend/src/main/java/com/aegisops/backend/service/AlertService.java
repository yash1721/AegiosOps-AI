package com.aegisops.backend.service;

import com.aegisops.backend.dto.AlertResponse;
import com.aegisops.backend.dto.CreateAlertRequest;
import com.aegisops.backend.dto.CreateAlertResponse;
import com.aegisops.backend.entity.Alert;
import com.aegisops.backend.entity.Incident;
import com.aegisops.backend.enums.IncidentStatus;
import com.aegisops.backend.mapper.IncidentManagementMapper;
import com.aegisops.backend.repository.AlertRepository;
import com.aegisops.backend.repository.IncidentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.EnumSet;
import java.util.List;
import java.util.Optional;

@Service
public class AlertService {

    private static final EnumSet<IncidentStatus> ACTIVE_STATUSES = EnumSet.of(
            IncidentStatus.OPEN,
            IncidentStatus.ANALYZING,
            IncidentStatus.ANALYZED,
            IncidentStatus.APPROVED
    );

    private final AlertRepository alertRepository;
    private final IncidentRepository incidentRepository;
    private final AuditLogService auditLogService;

    public AlertService(
            AlertRepository alertRepository,
            IncidentRepository incidentRepository,
            AuditLogService auditLogService
    ) {
        this.alertRepository = alertRepository;
        this.incidentRepository = incidentRepository;
        this.auditLogService = auditLogService;
    }

    @Transactional
    public CreateAlertResponse createAlert(CreateAlertRequest request) {
        String dedupKey = buildDedupKey(request);
        Optional<Incident> existingIncident = incidentRepository
                .findFirstByDedupKeyAndStatusInOrderByStartedAtAsc(dedupKey, ACTIVE_STATUSES);
        boolean deduped = existingIncident.isPresent();
        Incident incident = existingIncident.orElseGet(() -> createIncident(request, dedupKey));

        Alert alert = new Alert();
        alert.setIncident(incident);
        alert.setServiceName(request.serviceName());
        alert.setMetric(request.metric());
        alert.setValue(request.value());
        alert.setThreshold(request.threshold());
        alert.setSeverity(request.severity());
        alert.setRegion(request.region());
        alert.setFingerprint(request.fingerprint());
        alert.setRawPayload(request.rawPayload());
        Alert savedAlert = alertRepository.save(alert);

        auditLogService.record(
                "ALERT_RECEIVED",
                "ALERT",
                savedAlert.getId(),
                "{\"incidentId\":\"" + incident.getId() + "\"}"
        );

        if (deduped) {
            auditLogService.record(
                    "INCIDENT_DEDUPED",
                    "INCIDENT",
                    incident.getId(),
                    "{\"alertId\":\"" + savedAlert.getId() + "\",\"dedupKey\":\"" + dedupKey + "\"}"
            );
        } else {
            auditLogService.record(
                    "INCIDENT_CREATED",
                    "INCIDENT",
                    incident.getId(),
                    "{\"alertId\":\"" + savedAlert.getId() + "\",\"dedupKey\":\"" + dedupKey + "\"}"
            );
        }

        return new CreateAlertResponse(
                IncidentManagementMapper.toAlertResponse(savedAlert),
                incident.getId(),
                deduped
        );
    }

    @Transactional(readOnly = true)
    public List<AlertResponse> listAlerts() {
        return alertRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(IncidentManagementMapper::toAlertResponse)
                .toList();
    }

    private Incident createIncident(CreateAlertRequest request, String dedupKey) {
        Incident incident = new Incident();
        incident.setTitle("%s %s %s breach in %s".formatted(
                request.severity(),
                request.serviceName(),
                request.metric(),
                request.region()
        ));
        incident.setServiceName(request.serviceName());
        incident.setSeverity(request.severity());
        incident.setStatus(IncidentStatus.OPEN);
        incident.setDedupKey(dedupKey);
        return incidentRepository.save(incident);
    }

    private String buildDedupKey(CreateAlertRequest request) {
        return request.serviceName() + ":" + request.metric() + ":" + request.severity() + ":" + request.region();
    }
}
