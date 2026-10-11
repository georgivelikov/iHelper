# Phase 5: Packaging

**Status:** Done (awaiting review)

## Goal
The whole app (website + API) ships as one jar and one Docker image, runnable exactly as it will be hosted.

## Tasks
- [x] frontend-maven-plugin builds the frontend and copies `frontend/dist` into `backend/src/main/resources/static`
- [x] SPA fallback routing so deep links serve `index.html`
- [x] Multi-stage `Dockerfile` (Maven + Node build stage → slim JRE 25 runtime)
- [x] `app` service in `docker-compose.yml` behind compose profile `app`, wired to the `postgres` service
- [x] README updated: run modes (dev, jar, compose)
- [x] Update `CLAUDE.md` status line and `docs/PLAN.md` phase table; mark **v1 complete**

## Done when
- `java -jar backend/target/ihelper-*.jar` serves the full app on `:8080`
- `docker compose --profile app up` runs the full app with Postgres, data persisted in the volume

## Notes
Stop after this phase for user review. This completes v1.

## Implementation notes
- Bundling is the Maven profile `bundle-frontend` (`./mvnw -Pbundle-frontend package`), off by default so `mvn verify` needs no Node. The dist is copied into `target/classes/static`, not `src/`, so nothing generated is tracked.
- `SpaConfig` serves `classpath:/static` and falls back to `index.html` for extensionless non-API paths; `/api/**`, `/v3/**`, `/swagger-ui*` and missing assets stay 404. `spring.web.resources.add-mappings=false` so it is the only static handler. Covered by `SpaFallbackIT` (stub `index.html` in test resources).
- `IhelperApplication` forces the default time zone to UTC: `java -jar` failed on this Windows host because its zone name (`Europe/Kiev`) is rejected by PostgreSQL. Docker also sets `-Duser.timezone=UTC`.
- Image: `eclipse-temurin:25-jdk` build stage (Maven wrapper + frontend-maven-plugin), `eclipse-temurin:25-jre` runtime, non-root user, about 600 MB.
- `docker compose --profile app up --build` starts `app` on :8080 against the compose `postgres`; a task created through it survived an app restart (volume `ihelper-pgdata`).
- CI gets a `docker` job that builds the image.
- Same-origin hosting means no CORS config is needed for the bundled app.
