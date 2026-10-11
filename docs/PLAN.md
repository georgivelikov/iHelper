# iHelper: Plan

## Context
A personal TODO app that records tasks in **categories** with **priorities**. Roadmap: **run locally first → deploy as a web app → mobile app**. Preferred tech: **Java + TypeScript**.

Design principle: a clean REST API in Java and a separate TypeScript frontend. The same API later serves the hosted web app and a mobile client, so later stages add to v1 instead of rewriting it.

## Phases

| # | Phase | Outcome | Status |
|---|---|---|---|
| 1 | [Foundation](phases/phase-1-foundation.md) | Repo linked, Spring Boot boots, Postgres in Docker, basic CI | Done (awaiting review) |
| 2 | [Backend API](phases/phase-2-backend-api.md) | Categories/tasks/sync API, Swagger UI, all backend tests | Done (awaiting review) |
| 3 | [Frontend](phases/phase-3-frontend.md) | `api-client` + React UI + component tests | Done (awaiting review) |
| 4 | [E2E tests](phases/phase-4-e2e.md) | Playwright (desktop + mobile viewport) in CI | Not started |
| 5 | [Packaging](phases/phase-5-packaging.md) | Single jar + Dockerfile + compose; runs as hosted | Not started |
| 6 | [Web hosting](phases/phase-6-web-hosting.md) | Login (JWT), prod profile, host, HTTPS, backups | Not planned in detail |
| 7 | [Mobile](phases/phase-7-mobile.md) | Expo/React Native app with offline sync | Not planned in detail |

**v1 complete = Phases 1–5.** Backlog (any time after v1): tags, recurring tasks, reminders/notifications, search, archive.

## Stack
- **Backend:** Java 25, Spring Boot 4.1 (Web, Data JPA, Validation), Maven, Flyway
- **Database:** PostgreSQL everywhere (local dev, tests, e2e, hosted), in Docker. No H2. `spring-boot-docker-compose` starts the `postgres` service from `docker-compose.yml` on `mvn spring-boot:run`. Named volume `ihelper-pgdata`. Flyway manages the schema.
- **Frontend:** React + TypeScript + Vite, TanStack Query, plain CSS modules (no heavy UI kit)
- **Tests:** JUnit 5, Testcontainers, Vitest, Playwright

## Repository layout
```
iHelper/
  CLAUDE.md
  docs/               PLAN.md, ARCHITECTURE.md, phases/
  backend/            Spring Boot app (Maven)
    src/main/java/com/ihelper/
      task/           Task entity, repo, service, controller, DTOs
      category/       Category entity, repo, service, controller, DTOs
      sync/           /sync endpoint
      common/         error handling (@RestControllerAdvice), CORS config
    src/main/resources/
      application.yml           (local Postgres via docker compose; `e2e` and `prod` profiles)
      db/migration/V1__init.sql
  api-client/         TS package: types + fetch client generated from openapi.json
                      (shared by frontend now and mobile later)
  frontend/           Vite React-TS app
    src/api/          TanStack Query hooks built on api-client
    src/components/   TaskList, TaskForm, CategorySidebar, PriorityBadge, Filters
  e2e/                Playwright project
  docker-compose.yml
  Dockerfile
  README.md
  .gitignore
```

## Domain model (v1)
- All entities: `id` (UUID), `createdAt`, `updatedAt`, `version`, `deletedAt`
- **Category:** `name` (unique), `color`
- **Task:** `title` (required), `description` (optional), `categoryId` (FK, nullable = "Inbox"), `priority` enum `LOW | MEDIUM | HIGH | URGENT`, `status` enum `TODO | IN_PROGRESS | DONE`, `dueDate` (optional), `completedAt`
- Seed categories in V1 migration: Personal, Work, Home, Shopping, Ideas

## REST API (`/api/v1`): one API for web and mobile
The backend has no knowledge of any particular client. The web frontend and the mobile app are equal consumers of the same versioned JSON API.

**Endpoints**
- `GET/POST /categories`, `GET/PUT/DELETE /categories/{id}` (deleting a category moves its tasks to Inbox)
- `GET /tasks?categoryId=&priority=&status=&q=&sort=priority,-dueDate&page=&size=`
- `POST /tasks`, `GET/PUT/PATCH/DELETE /tasks/{id}` (PATCH does partial updates, e.g. `{"status":"DONE"}`)
- `GET /sync?since=<timestamp>`: every category and task changed since that time, including deletions. Mobile uses it for offline sync; web can ignore it.

**Client-neutral design rules**
- **Versioned path** `/api/v1`. Breaking changes go to `/api/v2`.
- **Contract-first via OpenAPI:** springdoc generates `openapi.json`, Swagger UI at `/swagger-ui`. A TypeScript client is generated from the spec (`openapi-typescript`) into the shared `api-client` package. No hand-written duplicate types.
- **Stateless:** no sessions or cookies. Auth later = bearer tokens (JWT access + refresh) in the `Authorization` header.
- **Client-generated UUIDs.** An offline phone can create a task with its own id and send it later without duplicates. `POST` with an existing id is idempotent.
- **Sync-ready fields** on every entity: `createdAt`, `updatedAt` (UTC ISO-8601), `version` (optimistic locking), `deletedAt` (soft delete, so deletions reach other devices via `/sync`).
- **Conflict handling:** updates send `version` (or `If-Match` ETag). A stale update gets `409 Conflict` with the current server copy.
- **Pagination** on lists (`page`/`size`, response `{items, page, size, total}`).
- **Uniform errors:** RFC 9457 Problem Details (`application/problem+json`) with field-level validation errors.
- **Dates:** UTC ISO-8601 on the wire. `dueDate` is a plain `LocalDate`.
- **CORS:** configurable allowed origins (Vite dev server now, web domain later).
- **Lean payloads:** DTOs, never JPA entities. A task has `categoryId`, not a nested category.

## Frontend (v1 UI)
- Left sidebar: categories with task counts, plus "All" and "Inbox". Add, rename, recolor categories.
- Main pane: task list sorted/grouped by priority, color-coded priority badges, overdue highlight, checkbox to complete
- Quick-add bar (title + priority + category), edit dialog for full details
- Filters: status (open/done/all) and priority; sort selector
- Vite dev proxy `/api` → `http://localhost:8080`

## Testing strategy

| Layer | Tools | What it covers | Command |
|---|---|---|---|
| Backend unit | JUnit 5, Mockito, AssertJ | Service rules, no Spring context: DONE sets `completedAt`, category delete moves tasks to Inbox, soft delete, version conflict, sync logic | `mvn test` |
| Backend slice | `@WebMvcTest` (+ MockMvc), `@DataJpaTest` | Controllers: validation, Problem Details, status codes, pagination. Repositories: filter/sort queries, soft-delete exclusion | `mvn test` |
| Backend integration | `@SpringBootTest(RANDOM_PORT)`, RestAssured, **Testcontainers PostgreSQL**, Flyway | Full HTTP → DB flows, incl. mobile scenarios: idempotent POST with client UUID, 409 on stale `version`, `/sync` returning deletions | `mvn verify` (Failsafe, `*IT.java`) |
| API contract | Snapshot of `/v3/api-docs` | Fails on unexpected OpenAPI changes (protects the mobile app); approve by updating the snapshot | `mvn verify` |
| Frontend component | Vitest, React Testing Library, **MSW** | Components and hooks: quick-add, filters, priority badge, overdue highlight, error display | `npm test` |
| End-to-end | **Playwright** (TS) | Real browser vs real backend + frontend: create/edit/complete/delete, categories, filter/sort, persistence. Chromium + mobile viewport (Pixel 7 / iPhone) | `npx playwright test` |

**Playwright details**
- `e2e/` at repo root with its own `package.json` and `playwright.config.ts`
- `webServer` starts the backend with the `e2e` Spring profile (separate `ihelper_e2e` Postgres database in the same container, Flyway clean + migrate at startup, seeded categories) and the Vite frontend
- Each test creates uniquely named data through the API (`request` fixture), so tests are independent and parallel-safe
- Page Object Model (`e2e/pages/TasksPage.ts`); selectors use `getByRole`/`getByLabel`
- On failure: trace, screenshot and video kept; view with `npx playwright show-report`

**Conventions**
- `*Test.java` = Surefire (fast, `mvn test`); `*IT.java` = Failsafe (`mvn verify`)
- Coverage: JaCoCo (backend), Vitest v8 (frontend); reported, no hard gate at first
- **CI:** `.github/workflows/ci.yml` runs backend `mvn verify`, frontend `npm test` + build, and Playwright on every push/PR; uploads the Playwright report

## Docker
- **Testcontainers PostgreSQL** for all `*IT.java`. Needs Docker running; tests skip with a clear message otherwise.
- Testcontainers **reuse** (`testcontainers.reuse.enable=true` in `~/.testcontainers.properties`) keeps the container between local runs
- Pin images to exact tags (e.g. `postgres:17.6`)
- CI: GitHub Actions `ubuntu-latest` already has Docker
- **`Dockerfile`:** multi-stage (Maven + Node build → slim JRE 25 runtime), one image for the whole app
- **`docker-compose.yml`:** `postgres` service (dev + e2e; published on `localhost:5432`) plus an `app` service behind compose profile `app` (`docker compose --profile app up` runs the packaged app as hosted)

## Verification (full v1, after Phase 5)
- `cd backend && mvn verify`: unit, slice, Testcontainers integration, contract tests pass
- `cd frontend && npm test`: component tests pass
- `cd e2e && npx playwright test`: journeys pass in desktop Chromium + mobile viewport
- CI workflow is green on GitHub after the first push
- `mvn spring-boot:run`, then `curl http://localhost:8080/api/v1/categories` returns the seeded categories; Swagger UI at `/swagger-ui` lists every endpoint
- Simulate a "mobile" client with `curl`: POST a task with your own UUID twice (one task, not two), update with a stale `version` (409), delete it, confirm `GET /api/v1/sync?since=...` reports the deletion
- `cd frontend && npm run dev`, open http://localhost:5173: create a task, change priority/category, filter, mark done, reload, confirm persistence
- Restart the backend and confirm tasks are still there
- `mvn package` with the frontend bundled, `java -jar backend/target/ihelper-*.jar`, confirm the full app works on :8080
