# Demo Flow

## Start Local Stack

```bash
make docker-up
```

This starts PostgreSQL, Redis, Qdrant, the Spring Boot backend, and the Vite frontend.

Frontend:

```text
http://localhost:5173
```

Backend:

```text
http://localhost:8080/api/v1
```

## Seed Data

```bash
make seed
```

The seed endpoint is available only when the backend runs with the `local` or `dev` Spring profile. Docker Compose starts the backend with `SPRING_PROFILES_ACTIVE=local`.

Seeded services:

- payment-service
- notification-service
- order-service

Seeded runbooks:

- Payment Service Latency Runbook
- Payment Error Rate Runbook
- Notification Queue Lag Runbook
- Order DB Saturation Runbook
- Generic Rollback Runbook

## Run Demo Script

```bash
make demo
```

The script runs:

1. Health check.
2. Seed baseline data.
3. Upload payment runbook.
4. Send p99 latency alert.
5. Send duplicate p99 latency alert.
6. List incidents.
7. Analyze incident.
8. Approve remediation.
9. Resolve incident.
10. Print audit logs.

## Manual Commands

Health check:

```bash
curl http://localhost:8080/api/v1/health
```

Upload payment runbook:

```bash
curl -X POST http://localhost:8080/api/v1/runbooks \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "payment-service",
    "title": "Demo Payment p99 Latency Runbook",
    "content": "For payment-service p99_latency incidents, inspect recent deployments, payment gateway latency, database response time, connection pool saturation, and downstream processor health. If a recent deployment aligns with the incident start, prepare rollback after human approval."
  }'
```

Send alert:

```bash
curl -X POST http://localhost:8080/api/v1/alerts \
  -H "Content-Type: application/json" \
  -d '{
    "serviceName": "payment-service",
    "metric": "p99_latency",
    "value": 1850,
    "threshold": 900,
    "severity": "SEV1",
    "region": "us-east-1",
    "fingerprint": "demo-payment-p99-latency-1",
    "rawPayload": "{\"source\":\"demo\"}"
  }'
```

After capturing the returned `incidentId`, run:

```bash
curl -X POST http://localhost:8080/api/v1/incidents/{incidentId}/analyze
curl -X POST http://localhost:8080/api/v1/incidents/{incidentId}/approve \
  -H "Content-Type: application/json" \
  -d '{"approvedBy":"demo-operator","actionType":"REMEDIATION"}'
curl -X POST http://localhost:8080/api/v1/incidents/{incidentId}/resolve
curl http://localhost:8080/api/v1/audit-logs
```

## Stop Local Stack

```bash
make docker-down
```
