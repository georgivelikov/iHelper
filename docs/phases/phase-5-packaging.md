# Phase 5: Packaging

**Status:** Not started (requires Phase 4)

## Goal
The whole app (website + API) ships as one jar and one Docker image, runnable exactly as it will be hosted.

## Tasks
- [ ] frontend-maven-plugin builds the frontend and copies `frontend/dist` into `backend/src/main/resources/static`
- [ ] SPA fallback routing so deep links serve `index.html`
- [ ] Multi-stage `Dockerfile` (Maven + Node build stage → slim JRE 25 runtime)
- [ ] `app` service in `docker-compose.yml` behind compose profile `app`, wired to the `postgres` service
- [ ] README updated: run modes (dev, jar, compose)
- [ ] Update `CLAUDE.md` status line and `docs/PLAN.md` phase table; mark **v1 complete**

## Done when
- `java -jar backend/target/ihelper-*.jar` serves the full app on `:8080`
- `docker compose --profile app up` runs the full app with Postgres, data persisted in the volume

## Notes
Stop after this phase for user review. This completes v1.
