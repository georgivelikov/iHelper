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

Web UI (second terminal, backend must be running):
```
cd frontend
npm install
npm run dev                  # http://localhost:5173, proxies /api to :8080
```
API docs: http://localhost:8080/swagger-ui

Start/stop the database by hand:
```
docker compose up -d postgres
docker compose down          # keeps data; add -v to wipe it
```

## Run modes
| Mode | Command | URL |
|---|---|---|
| Dev (hot reload) | `cd backend && ./mvnw spring-boot:run`, then `cd frontend && npm run dev` | http://localhost:5173 |
| Single jar | `cd backend && ./mvnw -Pbundle-frontend -DskipTests package`, then `java -jar target/ihelper-0.0.1-SNAPSHOT.jar` (needs Postgres: `docker compose up -d postgres`) | http://localhost:8080 |
| Docker Compose | `docker compose --profile app up --build` (app + Postgres, data in the `ihelper-pgdata` volume) | http://localhost:8080 |

The jar and the image serve the website and the API from one origin; Swagger UI is at `/swagger-ui` (off in `prod`).

### Accounts
Every task and category belongs to a user. In dev and tests anyone can create an account on the sign-in screen
(`ihelper.auth.registration=open`). The hosted profile defaults to **invite-only**. Compose runs the real `prod`
profile: copy `.env.example` to `.env` and set `IHELPER_JWT_SECRET` and `IHELPER_INVITE_CODE`.
Configuration and operations are in [docs/RUNBOOK.md](docs/RUNBOOK.md).

## Test
```
cd backend
./mvnw verify
```
Integration tests start their own PostgreSQL via Testcontainers (Docker must be running).
```
cd frontend
npm test                     # Vitest + React Testing Library + MSW
```
```
cd e2e
npm install && npx playwright install chromium    # first time
npx playwright test                               # desktop + mobile viewport
npx playwright show-report
```
Playwright starts the backend (`e2e` profile, port 8081, database `ihelper_e2e`, wiped on every start)
and the Vite frontend (port 5174) itself. Postgres must be running: `docker compose up -d postgres`.

After an API change: `cd backend && ./mvnw verify -Dopenapi.update=true`, then
`cd api-client && npm run generate` and commit both.

## Repo layout
- `backend/` Spring Boot app
- `api-client/` TypeScript types + typed fetch client generated from the OpenAPI snapshot
- `frontend/` React + TypeScript + Vite UI
- `e2e/` Playwright end-to-end tests
- `docker-compose.yml`, `docker/` local PostgreSQL (also creates the `ihelper_e2e` database)
- `docs/` plan, architecture, per-phase checklists
