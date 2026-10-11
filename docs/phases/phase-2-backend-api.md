# Phase 2: Backend API

**Status:** Done (awaiting review)

## Goal
The full versioned REST API for categories, tasks and sync, with all backend tests.

## Tasks
- [x] Flyway `V1__init.sql`: tables with UUID ids, `created_at`, `updated_at`, `version`, `deleted_at`; seed categories Personal, Work, Home, Shopping, Ideas
- [x] Entities, repositories (soft-delete aware), services, DTOs, mappers for `category` and `task`
- [x] Controllers under `/api/v1`: categories CRUD (delete moves tasks to Inbox); tasks CRUD + PATCH; filters (`categoryId`, `priority`, `status`, `q`), sort, pagination `{items,page,size,total}`
- [x] Client-generated UUIDs; `POST` with an existing id is idempotent
- [x] Optimistic locking: stale `version` → `409` with current server copy
- [x] `GET /sync?since=` returning changed categories and tasks including soft-deleted
- [x] Global `@RestControllerAdvice`: RFC 9457 Problem Details, field-level validation errors
- [x] Configurable CORS allowed origins
- [x] springdoc OpenAPI + Swagger UI at `/swagger-ui`
- [x] Unit tests (JUnit 5, Mockito, AssertJ) for service rules
- [x] Slice tests: `@WebMvcTest`, `@DataJpaTest`
- [x] Integration tests `*IT.java` (Testcontainers Postgres, RestAssured): CRUD, filters, pagination, Problem Details, 409, idempotent POST, `/sync` deletions
- [x] OpenAPI snapshot (contract) test
- [x] JaCoCo coverage report
- [x] Update `CLAUDE.md` status line and `docs/PLAN.md` phase table

## Done when
- `mvn verify` is green
- Swagger UI lists every endpoint
- `curl` "mobile client" checks pass: POST same UUID twice → one task; stale version → 409; delete → appears in `/sync?since=...`

## Notes
Stop after this phase for user review.

## Implementation notes (decisions made while building)
- `GET /categories` returns a plain array (small bounded set); `GET /tasks` and everything else that can grow is paginated, zero-based `page`.
- `GET /tasks?inbox=true` selects tasks without a category (a missing `categoryId` means "no filter").
- Priority is stored as its enum ordinal (smallint) so DB sorting is by rank; never reorder `Priority`.
- `POST` with an existing id returns `200` with the existing row (even if it was since soft-deleted); new rows return `201` + `Location`.
- Missing `version` on `PUT` is `400`; `PATCH`/`DELETE` treat version as an optional guard. `If-Match: "<n>"` is accepted wherever `version` is.
- `DELETE` is idempotent (`204` when already deleted); soft-deleted rows are `404` for GET/PUT/PATCH.
- `/sync` without `since` is a first full sync and omits already-deleted rows; with `since` it includes them. Response carries `serverTime` to use as the next `since`.
- Refresh the OpenAPI snapshot deliberately with `mvn verify -Dopenapi.update=true`.
