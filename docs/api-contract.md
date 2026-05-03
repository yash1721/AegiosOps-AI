# API Contract

All public APIs are versioned under `/api/v1`.

## Implemented

### GET /api/v1/health

Returns backend service health.

Response:

```json
{
  "status": "UP",
  "service": "aegisops-backend"
}
```

### POST /api/v1/runbooks

Uploads a runbook, chunks it, and attempts embedding/Qdrant indexing.

Request:

```json
{
  "serviceName": "checkout",
  "title": "Checkout CPU Remediation",
  "content": "Long runbook text..."
}
```

The response includes chunk metadata plus `indexed` and `indexingError` fields. Upload still succeeds if embedding or Qdrant indexing fails.

### GET /api/v1/runbooks

Returns runbooks ordered by creation time descending. List responses omit full content and chunks.

### GET /api/v1/runbooks/{runbookId}

Returns runbook detail with full content and chunk metadata.

## Planned V1 Endpoints

```text
GET    /api/v1/health
POST   /api/v1/alerts
GET    /api/v1/alerts
GET    /api/v1/incidents
GET    /api/v1/incidents/{incidentId}
POST   /api/v1/incidents/{incidentId}/analyze
POST   /api/v1/incidents/{incidentId}/approve
POST   /api/v1/incidents/{incidentId}/resolve
POST   /api/v1/runbooks
GET    /api/v1/runbooks
GET    /api/v1/runbooks/{runbookId}
GET    /api/v1/audit-logs
```

## API Principles

- Use stable request and response DTOs.
- Return validation errors as structured JSON.
- Do not expose stack traces or provider secrets.
- Keep AI-generated content advisory and clearly labeled.
- Require human approval for remediation actions.
