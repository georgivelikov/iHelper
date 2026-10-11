# Phase 4: End-to-end tests

**Status:** Not started (requires Phase 3)

## Goal
Playwright tests that drive the real app in a real browser, on desktop and phone-sized screens, in CI.

## Tasks
- [ ] `e2e/` project with its own `package.json` and `playwright.config.ts`
- [ ] `e2e` Spring profile: separate `ihelper_e2e` database, Flyway clean + migrate at startup, seeded categories
- [ ] Playwright `webServer` config starting backend (e2e profile) and Vite frontend with one command
- [ ] Projects: desktop Chromium + mobile viewport (Pixel 7 / iPhone emulation)
- [ ] Page Object Model (`e2e/pages/TasksPage.ts`); selectors by `getByRole` / `getByLabel`
- [ ] Journeys: create, edit, complete, delete a task; manage categories; filter and sort; data survives reload
- [ ] Test isolation: each test creates uniquely named data through the API `request` fixture; parallel-safe
- [ ] Trace, screenshot and video retained on failure
- [ ] CI extended: run Playwright, upload report as artifact
- [ ] Update `CLAUDE.md` status line and `docs/PLAN.md` phase table

## Done when
`npx playwright test` is green locally (both viewports) and in GitHub Actions.

## Notes
Stop after this phase for user review.
