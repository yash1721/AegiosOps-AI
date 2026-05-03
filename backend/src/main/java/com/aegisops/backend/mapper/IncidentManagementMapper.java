package com.aegisops.backend.mapper;

import com.aegisops.backend.dto.AlertResponse;
import com.aegisops.backend.dto.ApprovalResponse;
import com.aegisops.backend.dto.AuditLogResponse;
import com.aegisops.backend.dto.IncidentDetailResponse;
import com.aegisops.backend.dto.IncidentResponse;
import com.aegisops.backend.entity.Alert;
import com.aegisops.backend.entity.Approval;
import com.aegisops.backend.entity.AuditLog;
import com.aegisops.backend.entity.Incident;

import java.util.List;

public final class IncidentManagementMapper {

    private IncidentManagementMapper() {
    }

    public static AlertResponse toAlertResponse(Alert alert) {
        return new AlertResponse(
                alert.getId(),
                alert.getIncident().getId(),
                alert.getServiceName(),
                alert.getMetric(),
                alert.getValue(),
                alert.getThreshold(),
                alert.getSeverity(),
                alert.getRegion(),
                alert.getFingerprint(),
                alert.getRawPayload(),
                alert.getCreatedAt()
        );
    }

    public static IncidentResponse toIncidentResponse(Incident incident) {
        return new IncidentResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getServiceName(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getDedupKey(),
                incident.getStartedAt(),
                incident.getResolvedAt()
        );
    }

    public static IncidentDetailResponse toIncidentDetailResponse(
            Incident incident,
            List<AlertResponse> alerts,
            List<ApprovalResponse> approvals
    ) {
        return new IncidentDetailResponse(
                incident.getId(),
                incident.getTitle(),
                incident.getServiceName(),
                incident.getSeverity(),
                incident.getStatus(),
                incident.getDedupKey(),
                incident.getStartedAt(),
                incident.getResolvedAt(),
                alerts,
                approvals
        );
    }

    public static ApprovalResponse toApprovalResponse(Approval approval) {
        return new ApprovalResponse(
                approval.getId(),
                approval.getIncident().getId(),
                approval.getActionType(),
                approval.getStatus(),
                approval.getApprovedBy(),
                approval.getCreatedAt()
        );
    }

    public static AuditLogResponse toAuditLogResponse(AuditLog auditLog) {
        return new AuditLogResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getMetadata(),
                auditLog.getCreatedAt()
        );
    }
}
