# Architecture

```text
React Ops Console
        |
        v
Spring Boot Backend
        |
        v
PostgreSQL / Redis / Qdrant
        |
        v
Mock / Ollama / OpenAI-compatible LLM Provider
```

## Components

React Ops Console:

- Operator-facing UI for alerts, incidents, runbooks, analysis, approvals, resolution, and audit logs.

Spring Boot Backend:

- Public REST API under `/api/v1`.
- Owns validation, workflow state, persistence, deduplication, RAG orchestration, AI provider integration, and audit logging.

PostgreSQL:

- Durable source of truth for alerts, incidents, runbooks, analysis records, approvals, and audit logs.

Redis:

- Fast ephemeral store for deduplication windows, locks, and future async coordination.

Qdrant:

- Vector store for runbook chunks and retrieval metadata.

LLM Providers:

- Mock provider is the default for local development and deterministic tests.
- Ollama can support local models.
- OpenAI-compatible providers can be configured later for hosted inference.
