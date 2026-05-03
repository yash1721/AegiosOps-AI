CREATE TABLE service_entities (
    id UUID PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    owner_team VARCHAR(255) NOT NULL,
    tier VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE incidents (
    id UUID PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    service_name VARCHAR(255) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    dedup_key VARCHAR(1024) NOT NULL,
    started_at TIMESTAMP WITH TIME ZONE NOT NULL,
    resolved_at TIMESTAMP WITH TIME ZONE
);

CREATE TABLE alerts (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id),
    service_name VARCHAR(255) NOT NULL,
    metric VARCHAR(255) NOT NULL,
    value NUMERIC(19, 4) NOT NULL,
    threshold NUMERIC(19, 4) NOT NULL,
    severity VARCHAR(32) NOT NULL,
    region VARCHAR(255) NOT NULL,
    fingerprint VARCHAR(255),
    raw_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE approvals (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id),
    action_type VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    approved_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    entity_type VARCHAR(255) NOT NULL,
    entity_id UUID NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_incidents_dedup_key_status ON incidents(dedup_key, status);
CREATE INDEX idx_alerts_fingerprint ON alerts(fingerprint);
CREATE INDEX idx_alerts_created_at ON alerts(created_at);
CREATE INDEX idx_audit_logs_created_at ON audit_logs(created_at);
