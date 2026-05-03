package com.aegisops.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "ai_recommendations")
public class AIRecommendation {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "incident_id", nullable = false)
    private Incident incident;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Column(name = "probable_root_cause", nullable = false, columnDefinition = "TEXT")
    private String probableRootCause;

    @Column(name = "remediation_steps", nullable = false, columnDefinition = "TEXT")
    private String remediationSteps;

    @Column(nullable = false)
    private Double confidence;

    @Column(name = "needs_human_approval", nullable = false)
    private Boolean needsHumanApproval;

    @Column(name = "model_used", nullable = false)
    private String modelUsed;

    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    public AIRecommendation() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Incident getIncident() {
        return incident;
    }

    public void setIncident(Incident incident) {
        this.incident = incident;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getProbableRootCause() {
        return probableRootCause;
    }

    public void setProbableRootCause(String probableRootCause) {
        this.probableRootCause = probableRootCause;
    }

    public String getRemediationSteps() {
        return remediationSteps;
    }

    public void setRemediationSteps(String remediationSteps) {
        this.remediationSteps = remediationSteps;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Boolean getNeedsHumanApproval() {
        return needsHumanApproval;
    }

    public void setNeedsHumanApproval(Boolean needsHumanApproval) {
        this.needsHumanApproval = needsHumanApproval;
    }

    public String getModelUsed() {
        return modelUsed;
    }

    public void setModelUsed(String modelUsed) {
        this.modelUsed = modelUsed;
    }

    public String getRawResponse() {
        return rawResponse;
    }

    public void setRawResponse(String rawResponse) {
        this.rawResponse = rawResponse;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @PrePersist
    void prePersist() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = OffsetDateTime.now();
        }
    }
}
