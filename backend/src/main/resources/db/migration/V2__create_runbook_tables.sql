CREATE TABLE runbooks (
    id UUID PRIMARY KEY,
    service_name VARCHAR(255) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE runbook_chunks (
    id UUID PRIMARY KEY,
    runbook_id UUID NOT NULL REFERENCES runbooks(id),
    service_name VARCHAR(255) NOT NULL,
    chunk_text TEXT NOT NULL,
    chunk_index INTEGER NOT NULL,
    qdrant_point_id UUID,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_runbooks_service_name ON runbooks(service_name);
CREATE INDEX idx_runbook_chunks_service_name ON runbook_chunks(service_name);
CREATE INDEX idx_runbook_chunks_runbook_id ON runbook_chunks(runbook_id);
