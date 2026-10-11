# Phase 3: Frontend

**Status:** Done (awaiting review)

## Goal
A usable React UI for managing categories and tasks, built on a generated, shared API client.

## Tasks
- [x] `api-client/` package: npm script generating types and fetch client from `http://localhost:8080/v3/api-docs` (`openapi-typescript`)
- [x] `frontend/` via `npm create vite@latest frontend -- --template react-ts`; add TanStack Query; plain CSS modules
- [x] Vite dev proxy `/api` → `http://localhost:8080`
- [x] `src/api/` TanStack Query hooks built on `api-client`
- [x] Category sidebar: categories with task counts, "All", "Inbox"; add/rename/recolor
- [x] Task list: sort/group by priority, color-coded `PriorityBadge`, overdue highlight, complete checkbox
- [x] Quick-add bar (title + priority + category) and edit dialog (full details)
- [x] Filters (status open/done/all, priority) and sort selector
- [x] Error display from Problem Details; handle 409 conflict gracefully
- [x] Responsive layout that works at phone width
- [x] Component tests: Vitest + React Testing Library + MSW; v8 coverage
- [x] CI extended with `npm test` + build
- [x] Update `CLAUDE.md` status line and `docs/PLAN.md` phase table

## Done when
You can manage tasks in the browser at `http://localhost:5173`, data survives a backend restart, and `npm test` is green.

## Notes
Stop after this phase for user review.

## Implementation notes
- `api-client` generates `src/schema.d.ts` from the committed OpenAPI snapshot (`npm run generate`; `generate:live` reads a running backend). The generated file is committed so the frontend builds without the backend; CI fails if it drifts from the snapshot. Frontend depends on it as `file:../api-client`.
- Two small backend additions were needed by the UI: `GET /api/v1/tasks/counts` (open task counts for the sidebar) and a repeatable `status` filter (`status=TODO&status=IN_PROGRESS` for "Open"). Response DTO fields are now marked required/nullable in OpenAPI so generated types are not all optional.
- PATCH sends the task `version`; a 409 in the edit dialog offers "Load latest" and re-applies on the server copy's version. Elsewhere a 409 shows a banner and the lists refetch.
- Lists request `size=200` (API maximum) and show a note when there are more.
- Run locally: `cd backend && ./mvnw spring-boot:run`, then `cd frontend && npm install && npm run dev` (http://localhost:5173).
