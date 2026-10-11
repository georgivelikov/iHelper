# Phase 6: Web hosting

**Status:** Planned, awaiting user approval of this plan (not started)

## Goal
iHelper reachable on the internet over HTTPS, with real accounts, running from the same Docker image as Phase 5. Prerequisite for the mobile app (Phase 7).

## Decisions (user, this phase)
- **Host:** a PaaS (Fly.io / Railway / Render). Provider-neutral work comes first; the provider is picked at step 6 (see "Open items").
- **Users:** **multiple users**, username + password. (This changes the earlier single-user assumption: every task and category gets an owner.)
- **Domain:** none yet. Use the provider's default hostname now; HTTPS and CORS origin are configuration, so adding a domain later is a settings change.

## Steps (in order; each leaves the app working)

### 1. Accounts and ownership (backend)
- Migration `V2`: `app_user` (`id` uuid, `username` unique case-insensitive, `password_hash`, `created_at`, `version`, `deleted_at`); `owner_id` (FK, not null) on `category` and `task`; category-name uniqueness becomes per owner.
- Existing rows (dev data) are assigned to a placeholder `local` user created in the migration. Hosted databases start empty.
- Every repository query, service and `/sync` is scoped to the authenticated user. A user asking for another user's id gets **404**, never 403.
- Seed categories (Personal, Work, Home, Shopping, Ideas) are created per user at registration instead of in the migration.
- Tests: cross-user isolation in every IT (list, get, patch, delete, sync, counts).

### 2. Authentication (Spring Security, stateless)
- `POST /api/v1/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`; `GET /api/v1/me`.
- Passwords hashed with BCrypt (or Argon2 if available in Boot's `PasswordEncoder` set); minimum length enforced.
- Access token: short-lived JWT (about 15 min) in the `Authorization: Bearer` header. Refresh token: opaque, random, stored hashed server-side (rotation + revocation on logout). Same flow serves browser and mobile.
- Signing secret from environment (`IHELPER_JWT_SECRET`); the app refuses to start in `prod` without it.
- Registration is gated by `IHELPER_REGISTRATION` = `closed` | `invite` | `open`, default `invite` (a shared invite code from the environment) so a public URL cannot be used by strangers.
- Basic brute-force protection: per-IP and per-username rate limit on login/register.
- OpenAPI declares the bearer scheme; the snapshot test is updated and `api-client` regenerated.
- Problem Details for 401/403; unauthenticated `/api/v1/**` returns 401. Public: auth endpoints, health, static frontend.

### 3. Frontend
- Login / register screen, in-memory access token with refresh on 401 (retry once), logout.
- Refresh token storage decision: `HttpOnly; Secure; SameSite=Strict` cookie scoped to `/api/v1/auth` for the web app (no JS access), while the same endpoints accept the token in the body for mobile. Decided at implementation, noted in the phase file.
- Component tests (MSW) for the auth flow; the e2e suite logs in through the API in a fixture per test (unique user per test keeps tests parallel-safe).

### 4. `prod` profile and operations
- `prod` profile: datasource from env, `ddl-auto: validate`, Flyway migrate only (never clean), CORS origins from env (same-origin by default since the jar serves the UI), no Swagger UI unless `IHELPER_SWAGGER=true`.
- Spring Actuator: `/actuator/health` (liveness/readiness, DB check) public and minimal; everything else off.
- Structured, non-sensitive logging (no tokens, no passwords, no request bodies).
- Security headers (HSTS behind HTTPS, `X-Content-Type-Options`, `frame-options`, a CSP that fits the Vite bundle).

### 5. Delivery
- CI builds the image and pushes it to GitHub Container Registry on `main` (tags: commit SHA + `latest`).
- Deploy workflow (manual trigger first, `workflow_dispatch`): deploys the SHA-tagged image to the chosen host; migrations run at app start.
- Secrets live in the host's secret store and GitHub Actions secrets, never in the repo.

### 6. Hosting and data safety
- Pick the provider (see "Open items"), create app + managed PostgreSQL, set env vars, deploy, check `/actuator/health`.
- HTTPS via the provider's certificate on its default hostname.
- Backups: provider-managed daily backups or scheduled `pg_dump`; **a restore is tested into a scratch database and recorded in `docs/`**.
- Runbook `docs/RUNBOOK.md`: deploy, rollback to a previous image, rotate JWT secret, restore a backup, add a custom domain.

## Done when
- A new user can register (with the invite code), log in on the hosted HTTPS URL, create tasks, reload, log out; a second user sees none of the first user's data.
- `mvn verify`, `npm test` and `npx playwright test` are green with authentication on; CI is green; the image deploys from CI.
- A database backup has been restored successfully into a scratch database.
- No secret appears in the repo or image.

## Open items to settle at the start of step 6 (do not block steps 1-5)
- **Provider.** Recommendation: whichever has a managed Postgres with automatic backups within budget at that moment; Fly.io and Render both deploy the Docker image directly. Free tiers often expire the database, which is unsuitable for real data.
- **Budget** per month.
- **Registration mode** in production (`invite` assumed).

## Risks / notes
- Multi-user turns a simple app into one holding other people's data: password hashing, rate limiting, 404-not-403, and per-user tests are not optional.
- The mobile client (Phase 7) will rely on the refresh-token-in-body flow and `/sync`; keep both stable.
- Email-based password reset is **out of scope** (no mail service); with `invite` registration the admin can reset by hand. Revisit if the user base grows.

Stop after this phase for user review.
