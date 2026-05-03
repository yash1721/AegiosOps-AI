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
