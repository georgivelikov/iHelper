# Phase 4: End-to-end tests

**Status:** Done (awaiting review)

## Goal
Playwright tests that drive the real app in a real browser, on desktop and phone-sized screens, in CI.

## Tasks
- [x] `e2e/` project with its own `package.json` and `playwright.config.ts`
- [x] `e2e` Spring profile: separate `ihelper_e2e` database, Flyway clean + migrate at startup, seeded categories
- [x] Playwright `webServer` config starting backend (e2e profile) and Vite frontend with one command
- [x] Projects: desktop Chromium + mobile viewport (Pixel 7 / iPhone emulation)
- [x] Page Object Model (`e2e/pages/TasksPage.ts`); selectors by `getByRole` / `getByLabel`
- [x] Journeys: create, edit, complete, delete a task; manage categories; filter and sort; data survives reload
- [x] Test isolation: each test creates uniquely named data through the API `request` fixture; parallel-safe
- [x] Trace, screenshot and video retained on failure
- [x] CI extended: run Playwright, upload report as artifact
- [x] Update `CLAUDE.md` status line and `docs/PLAN.md` phase table

## Done when
`npx playwright test` is green locally (both viewports) and in GitHub Actions.

## Notes
Stop after this phase for user review.

## Implementation notes
- Ports: e2e backend on **8081** and Vite on **5174**, so a running dev stack (8080/5173) is untouched. `API_PROXY_TARGET` env var points the Vite proxy at the e2e backend.
- The e2e profile allows CORS origin `http://localhost:5174`: Vite's proxy forwards `Origin` unchanged, and the first run caught a 403 from this.
- `E2eDatabaseConfig` (profile `e2e` only) runs Flyway `clean` then `migrate`; `clean-disabled: false` is set only in that profile.
- `e2e/start-backend.mjs` launches `mvnw`/`mvnw.cmd` so the Playwright config has no per-OS shell quirks.
- Local prerequisite: `docker compose up -d postgres`. The `ihelper_e2e` database is created by `docker/postgres/init` on first volume start; if your volume predates it, run `docker exec ihelper-postgres psql -U ihelper -d ihelper -c "CREATE DATABASE ihelper_e2e"`.
- Desktop = Desktop Chrome, mobile = Pixel 7 (Chromium). WebKit/iPhone emulation is not included.
- 12 journeys x 2 viewports = 24 tests. Each seeds its own uniquely named data through the API; the UI is scoped with the search box.
- CI job `e2e` has a Postgres service container and uploads `playwright-report/` and `test-results/` as an artifact.
