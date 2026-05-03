# AegisOps

AegisOps is an AI-assisted operations platform for deduplicating alerts into incidents, retrieving relevant runbooks, generating explainable analysis, and tracking approved remediation through audit logs.

## Tech Stack

- Backend: Java 21, Spring Boot 3, Maven, Spring Web, Spring Data JPA, Spring Validation, Flyway, PostgreSQL, Redis
- Frontend: React, Vite, Tailwind CSS
- AI/RAG: Qdrant, mock LLM provider by default, optional Ollama and OpenAI-compatible providers
- Testing: JUnit 5, Mockito, Testcontainers

## Current Status

Foundation setup.

The repository currently includes:

- Spring Boot backend scaffold
- React Vite frontend scaffold with Tailwind
- Docker Compose infrastructure for PostgreSQL, Redis, and Qdrant
- Health endpoint at `GET /api/v1/health`
- Initial API, architecture, and demo-flow documentation
- Shared `AGENTS.md` engineering contract

## Environment Variables

Copy `.env.example` into your local shell, IDE run configuration, or a local `.env` file used by your tooling.

Required variables:

```text
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/aegisops
SPRING_DATASOURCE_USERNAME=postgres
SPRING_DATASOURCE_PASSWORD=postgres
REDIS_HOST=localhost
REDIS_PORT=6379
QDRANT_URL=http://localhost:6333
QDRANT_COLLECTION=aegisops_runbooks
LLM_PROVIDER=mock
OLLAMA_BASE_URL=http://localhost:11434
OLLAMA_CHAT_MODEL=llama3.1
OLLAMA_EMBEDDING_MODEL=nomic-embed-text
OPENAI_API_KEY=
VITE_API_BASE_URL=http://localhost:8080/api/v1
```

The `.env.example` file uses Docker service hostnames for container-oriented defaults. When running backend locally against Docker Compose infrastructure, use `localhost` hostnames as shown above.

## Start Infrastructure

Start PostgreSQL, Redis, and Qdrant:

```bash
make docker-up
```

Stop infrastructure:

```bash
make docker-down
```

## Run Backend Locally

Prerequisites:

- Java 21
- Infrastructure running with `make docker-up`

Run:

```bash
cd backend
./mvnw spring-boot:run
```

On Windows, use `mvnw.cmd spring-boot:run`.

Health check:

```bash
curl http://localhost:8080/api/v1/health
```

Expected response:

```json
{
  "status": "UP",
  "service": "aegisops-backend"
}
```

## Run Frontend Locally

Prerequisites:

- Node.js
- npm
- Backend running locally on port `8080`

Install dependencies and start the dev server:

```bash
cd frontend
npm install
npm run dev
```

## Validation

Backend:

```bash
cd backend
./mvnw test
```

On Windows, use `mvnw.cmd test`.

Frontend:

```bash
cd frontend
npm run build
```

## Planned V1 Capabilities

- Alert ingestion
- Alert deduplication into incidents
- Incident list and detail views
- Runbook upload and retrieval
- RAG-assisted incident analysis
- Human approval for remediation
- Incident resolution workflow
- Audit log visibility

## Branch Workflow

- `main` contains the shared project foundation.
- Feature work should happen on dedicated branches created from latest `main`.
- Do not force push shared branches.
- Keep README and docs accurate when setup or API behavior changes.
