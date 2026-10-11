# Phase 1: Foundation

**Status:** Not started

## Goal
A repo connected to GitHub, a booting Spring Boot app, PostgreSQL running in Docker, and a basic CI pipeline.

## Tasks
- [ ] `git init -b main` in `C:\WORK\PROJECTS\iHelper`; `git remote add origin https://github.com/georgivelikov/iHelper.git` (remote is currently empty, no branches)
- [ ] Root `.gitignore`: Maven `target/`, `node_modules/`, `.env`, Playwright reports, IDE files
- [ ] `README.md` with how to run locally
- [ ] Generate backend via Spring Initializr into `backend/`: Web, Data JPA, Validation, Flyway, PostgreSQL driver, `spring-boot-docker-compose`, Testcontainers (Java 25, Maven, package `com.ihelper`)
- [ ] `docker-compose.yml`: `postgres` service (pinned image tag, named volume `ihelper-pgdata`, port 5432) plus an init script that also creates the `ihelper_e2e` database
- [ ] `application.yml` for local Postgres via docker compose; placeholders for `e2e` and `prod` profiles
- [ ] Trivial context-load test (Testcontainers Postgres) passes
- [ ] `.github/workflows/ci.yml`: skeleton that runs `mvn verify` in `backend/`
- [ ] Initial commit including `CLAUDE.md` and `docs/`
- [ ] Update `CLAUDE.md` status line and `docs/PLAN.md` phase table

## Done when
`mvn spring-boot:run` starts Postgres automatically and the app boots, and the context-load test passes under `mvn verify`.

## Notes
- Do not push to `origin` until the user confirms.
- Stop after this phase for user review.
