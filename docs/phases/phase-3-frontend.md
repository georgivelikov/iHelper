# Phase 3: Frontend

**Status:** Not started (requires Phase 2)

## Goal
A usable React UI for managing categories and tasks, built on a generated, shared API client.

## Tasks
- [ ] `api-client/` package: npm script generating types and fetch client from `http://localhost:8080/v3/api-docs` (`openapi-typescript`)
- [ ] `frontend/` via `npm create vite@latest frontend -- --template react-ts`; add TanStack Query; plain CSS modules
- [ ] Vite dev proxy `/api` → `http://localhost:8080`
- [ ] `src/api/` TanStack Query hooks built on `api-client`
- [ ] Category sidebar: categories with task counts, "All", "Inbox"; add/rename/recolor
- [ ] Task list: sort/group by priority, color-coded `PriorityBadge`, overdue highlight, complete checkbox
- [ ] Quick-add bar (title + priority + category) and edit dialog (full details)
- [ ] Filters (status open/done/all, priority) and sort selector
- [ ] Error display from Problem Details; handle 409 conflict gracefully
- [ ] Responsive layout that works at phone width
- [ ] Component tests: Vitest + React Testing Library + MSW; v8 coverage
- [ ] CI extended with `npm test` + build
- [ ] Update `CLAUDE.md` status line and `docs/PLAN.md` phase table

## Done when
You can manage tasks in the browser at `http://localhost:5173`, data survives a backend restart, and `npm test` is green.

## Notes
Stop after this phase for user review.
