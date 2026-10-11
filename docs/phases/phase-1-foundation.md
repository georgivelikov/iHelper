# Phase 1: Foundation

**Status:** Done (awaiting user review)

## Goal
A repo connected to GitHub, a booting Spring Boot app, PostgreSQL running in Docker, and a basic CI pipeline.

## Tasks
- [x] `git init -b main` in `C:\WORK\PROJECTS\iHelper`; `git remote add origin https://github.com/georgivelikov/iHelper.git` (remote is currently empty, no branches)
- [x] Root `.gitignore`: Maven `target/`, `node_modules/`, `.env`, Playwright reports, IDE files
- [x] Root `.gitattributes`: LF normalization, `git add --renormalize .`
- [x] `README.md` with how to run locally
- [x] Generate backend via Spring Initializr into `backend/`: Web, Data JPA, Validation, Flyway, PostgreSQL driver, `spring-boot-docker-compose`, Testcontainers (Java 25, Maven, package `com.ihelper`)
- [x] `docker-compose.yml`: `postgres` service (pinned image tag, named volume `ihelper-pgdata`, port 5432) plus an init script that also creates the `ihelper_e2e` database
- [x] `application.yml` for local Postgres via docker compose; placeholders for `e2e` and `prod` profiles
- [x] Trivial context-load test (Testcontainers Postgres) passes
- [x] `.github/workflows/ci.yml`: skeleton that runs `mvn verify` in `backend/`
- [x] Initial commit including `CLAUDE.md` and `docs/`
- [x] Update `CLAUDE.md` status line and `docs/PLAN.md` phase table

## Done when
`mvn spring-boot:run` starts Postgres automatically and the app boots, and the context-load test passes under `mvn verify`.

## Notes
- Do not push to `origin` until the user confirms.
- Stop after this phase for user review.
- Spring Initializr no longer offers Boot 3.x, so the backend uses Spring Boot **4.1.1** (Java 25, Maven).
- JVM runs with `-Duser.timezone=UTC` (set in `pom.xml` for `spring-boot:run` and Surefire): PostgreSQL 17.6 rejects the legacy `Europe/Kiev` zone alias.
- Boot 4 starters are renamed (`spring-boot-starter-webmvc`, `spring-boot-starter-flyway`, ...).
