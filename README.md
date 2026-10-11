# iHelper

Personal TODO app: tasks in categories, with priorities. Java (Spring Boot) backend + TypeScript (React) frontend.
See `docs/PLAN.md` for the plan and phase status, `docs/ARCHITECTURE.md` for the architecture.

## Prerequisites
- Java 25, Maven 3.9+ (or use the included `mvnw`)
- Docker (PostgreSQL runs in a container)
- Node 22+ (from Phase 3)

## Run locally
```
cd backend
./mvnw spring-boot:run
```
Spring Boot starts PostgreSQL from `docker-compose.yml` automatically and connects to it.
Data lives in the Docker volume `ihelper-pgdata`. The app listens on http://localhost:8080.

Start/stop the database by hand:
```
docker compose up -d postgres
docker compose down          # keeps data; add -v to wipe it
```

## Test
```
cd backend
./mvnw verify
```
Integration tests start their own PostgreSQL via Testcontainers (Docker must be running).

## Repo layout
- `backend/` Spring Boot app
- `docker-compose.yml`, `docker/` local PostgreSQL (also creates the `ihelper_e2e` database)
- `docs/` plan, architecture, per-phase checklists
