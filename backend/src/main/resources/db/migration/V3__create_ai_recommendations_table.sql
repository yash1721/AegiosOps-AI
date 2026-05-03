CREATE TABLE ai_recommendations (
    id UUID PRIMARY KEY,
    incident_id UUID NOT NULL REFERENCES incidents(id),
    summary TEXT NOT NULL,
    probable_root_cause TEXT NOT NULL,
    remediation_steps TEXT NOT NULL,
    confidence DOUBLE PRECISION NOT NULL,
    needs_human_approval BOOLEAN NOT NULL,
    model_used VARCHAR(255) NOT NULL,
    raw_response TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ai_recommendations_incident_id ON ai_recommendations(incident_id);
CREATE INDEX idx_ai_recommendations_created_at ON ai_recommendations(created_at);
