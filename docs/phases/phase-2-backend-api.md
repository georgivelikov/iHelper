# Phase 2: Backend API

**Status:** Not started (requires Phase 1)

## Goal
The full versioned REST API for categories, tasks and sync, with all backend tests.

## Tasks
- [ ] Flyway `V1__init.sql`: tables with UUID ids, `created_at`, `updated_at`, `version`, `deleted_at`; seed categories Personal, Work, Home, Shopping, Ideas
- [ ] Entities, repositories (soft-delete aware), services, DTOs, mappers for `category` and `task`
- [ ] Controllers under `/api/v1`: categories CRUD (delete moves tasks to Inbox); tasks CRUD + PATCH; filters (`categoryId`, `priority`, `status`, `q`), sort, pagination `{items,page,size,total}`
- [ ] Client-generated UUIDs; `POST` with an existing id is idempotent
- [ ] Optimistic locking: stale `version` → `409` with current server copy
- [ ] `GET /sync?since=` returning changed categories and tasks including soft-deleted
- [ ] Global `@RestControllerAdvice`: RFC 9457 Problem Details, field-level validation errors
- [ ] Configurable CORS allowed origins
- [ ] springdoc OpenAPI + Swagger UI at `/swagger-ui`
- [ ] Unit tests (JUnit 5, Mockito, AssertJ) for service rules
- [ ] Slice tests: `@WebMvcTest`, `@DataJpaTest`
- [ ] Integration tests `*IT.java` (Testcontainers Postgres, RestAssured): CRUD, filters, pagination, Problem Details, 409, idempotent POST, `/sync` deletions
- [ ] OpenAPI snapshot (contract) test
- [ ] JaCoCo coverage report
- [ ] Update `CLAUDE.md` status line and `docs/PLAN.md` phase table

## Done when
- `mvn verify` is green
- Swagger UI lists every endpoint
- `curl` "mobile client" checks pass: POST same UUID twice → one task; stale version → 409; delete → appears in `/sync?since=...`

## Notes
Stop after this phase for user review.
