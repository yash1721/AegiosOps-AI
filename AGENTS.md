# AGENTS.md

This file is the engineering contract for every Codex thread working on AegisOps.

## Project Mission

AegisOps is an AI-assisted operations platform for turning noisy alerts into deduplicated incidents, retrieving relevant runbooks, generating explainable analysis, and tracking approved remediation actions through audit logs.

The product goal is not to replace operators. The goal is to reduce repetitive investigation work while preserving human approval, traceability, and operational safety.

## Required Backend Stack

The backend must be implemented in Java 21 and Spring Boot 3.

Allowed backend technologies:

- Java 21
- Spring Boot 3
- Spring Web
- Spring Data JPA
- Spring Validation
- Flyway
- PostgreSQL
- Redis
- Maven
- Lombok
- JUnit 5
- Mockito
- Testcontainers

Do not use Python, FastAPI, SQLAlchemy, Alembic, Pydantic, or other Python backend frameworks in this repository.

## Branch Ownership Rules

- Work only on the branch assigned for the current task.
- Do not modify unrelated branches.
- Do not delete unrelated files.
- Do not rewrite branch history unless the user explicitly asks for it.
- If the working tree contains unrelated changes, leave them intact and work around them.
- If a task depends on another branch, fetch and merge or rebase only after the user confirms the intended flow.

## Git Push Rules

- Push only to the branch assigned for the current task.
- Do not push to `main` unless the task explicitly assigns `main`.
- Do not force push.
- Do not delete remote branches.
- Commit messages should be concise and describe the durable change.

## API Contract

All public backend APIs must be versioned under:

```text
/api/v1
```

API responses should be stable, explicit, and suitable for frontend consumption. Validation failures and application errors should return structured error responses. Avoid leaking stack traces, database internals, provider internals, or secrets.

Planned V1 endpoints:

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

## Domain Entities

Core domain concepts:

- Alert: raw event received from an external monitoring or incident source.
- Incident: deduplicated operational issue created from one or more related alerts.
- Runbook: operator-authored remediation or investigation guide.
- Analysis: AI-assisted explanation, likely cause, impact, confidence, and suggested next steps.
- Remediation Approval: human decision authorizing a proposed remediation action.
- Audit Log: immutable record of important system, user, AI, and remediation events.

Do not create database tables for these entities before their lifecycle and invariants are clear.

## Deduplication Contract

Alert deduplication must be deterministic, explainable, and safe.

Future implementations should consider:

- Stable deduplication keys derived from alert source, service, environment, severity, fingerprint, and time window.
- Redis for short-lived duplicate suppression and fast lookups.
- PostgreSQL as the durable source of truth.
- Clear behavior for duplicate alerts, reopened incidents, and resolved incidents.
- Audit logs for deduplication decisions that materially affect incident state.

Never silently discard alert information. If an alert is deduplicated, preserve the relationship to the incident.

## RAG Retrieval Contract

Runbook retrieval must be scoped, auditable, and explainable.

Future implementations should:

- Store runbook metadata in PostgreSQL.
- Store embeddings and searchable chunks in Qdrant.
- Return the retrieved chunk identifiers and scores used for analysis.
- Avoid sending unrelated or excessive context to the LLM provider.
- Support a mock retrieval path for local development and testing.

## AI Analysis Contract

AI output must be treated as advisory, not authoritative.

Future implementations must:

- Default to the mock LLM provider for local development.
- Support Ollama and OpenAI-compatible providers through configuration.
- Keep provider-specific code behind client interfaces.
- Persist prompts, retrieved context references, provider name, model name, and response metadata when analysis affects workflow.
- Require human approval before remediation.
- Return confidence and reasoning summaries without exposing secrets.

## Frontend Contract

The frontend is a React, Vite, and Tailwind CSS application.

Frontend code should:

- Read the backend base URL from `VITE_API_BASE_URL`.
- Treat `/api/v1` as the public API prefix.
- Keep operational screens dense, clear, and task-focused.
- Avoid hardcoded provider secrets, backend URLs, or environment-specific values.
- Surface backend validation and workflow errors clearly.

## Testing Contract

Backend testing expectations:

- Unit and web-slice tests for controller and service behavior.
- Integration tests with Testcontainers when persistence behavior exists.
- Tests must not require local PostgreSQL, Redis, or Qdrant unless explicitly marked as integration tests.
- Mock external LLM providers by default.

Frontend testing expectations:

- The app must compile with `npm run build`.
- Add component or workflow tests as UI behavior becomes meaningful.

## Local Development Contract

Infrastructure services run in Docker Compose:

- PostgreSQL
- Redis
- Qdrant

Backend and frontend run locally:

```bash
cd backend && mvn spring-boot:run
cd frontend && npm run dev
```

Use `.env.example` as the source of truth for required environment variables.

## README Accuracy Rule

Whenever setup commands, ports, environment variables, or workflows change, update `README.md` in the same change.

Do not leave README instructions aspirational. They must describe what actually works in the current repository state.
