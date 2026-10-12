# Phase 6: Web hosting

**Status:** Steps 1-5 and the provider-neutral part of 6 done (awaiting review). Actual hosting is blocked on the user: provider account, budget, domain.

## Goal
iHelper reachable on the internet over HTTPS, with real accounts, running from the same Docker image as Phase 5. Prerequisite for the mobile app (Phase 7).

## Decisions (user, this phase)
- **Host:** a PaaS (Fly.io / Railway / Render). Provider-neutral work comes first; the provider is picked at step 6 (see "Open items").
- **Users:** **multiple users**, username + password. (This changes the earlier single-user assumption: every task and category gets an owner.)
- **Domain:** none yet. Use the provider's default hostname now; HTTPS and CORS origin are configuration, so adding a domain later is a settings change.

## Steps (in order; each leaves the app working)

### 1. Accounts and ownership (backend)  -- DONE
- Migration `V2`: `app_user` (`id` uuid, `username` unique case-insensitive, `password_hash`, `created_at`, `version`, `deleted_at`); `owner_id` (FK, not null) on `category` and `task`; category-name uniqueness becomes per owner.
- Existing rows (dev data) are assigned to a placeholder `local` user created in the migration. Hosted databases start empty.
- Every repository query, service and `/sync` is scoped to the authenticated user. A user asking for another user's id gets **404**, never 403.
- Starter categories are created per user at registration instead of in the migration (originally Personal, Work, Home, Shopping, Ideas; now Personal, Ideas, Work, see "Follow-up changes").
- Tests: cross-user isolation in every IT (list, get, patch, delete, sync, counts).

### 2. Authentication (Spring Security, stateless)  -- DONE
- `POST /api/v1/auth/register`, `/auth/login`, `/auth/refresh`, `/auth/logout`; `GET /api/v1/me`.
- Passwords hashed with BCrypt (or Argon2 if available in Boot's `PasswordEncoder` set); minimum length enforced.
- Access token: short-lived JWT (about 15 min) in the `Authorization: Bearer` header. Refresh token: opaque, random, stored hashed server-side (rotation + revocation on logout). Same flow serves browser and mobile.
- Signing secret from environment (`IHELPER_JWT_SECRET`); the app refuses to start in `prod` without it.
- Registration is gated by `IHELPER_REGISTRATION` = `closed` | `invite` | `open`, default `invite` (a shared invite code from the environment) so a public URL cannot be used by strangers.
- Basic brute-force protection: per-IP and per-username rate limit on login/register.
- OpenAPI declares the bearer scheme; the snapshot test is updated and `api-client` regenerated.
- Problem Details for 401/403; unauthenticated `/api/v1/**` returns 401. Public: auth endpoints, health, static frontend.

### 3. Frontend  -- DONE
- Login / register screen, in-memory access token with refresh on 401 (retry once), logout.
- Refresh token storage decision: `HttpOnly; Secure; SameSite=Strict` cookie scoped to `/api/v1/auth` for the web app (no JS access), while the same endpoints accept the token in the body for mobile. Decided at implementation, noted in the phase file.
- Component tests (MSW) for the auth flow; the e2e suite logs in through the API in a fixture per test (unique user per test keeps tests parallel-safe).

### 4. `prod` profile and operations  -- DONE
- `prod` profile: datasource from env, `ddl-auto: validate`, Flyway migrate only (never clean), CORS origins from env (same-origin by default since the jar serves the UI), no Swagger UI unless `IHELPER_SWAGGER=true`.
- Spring Actuator: `/actuator/health` (liveness/readiness, DB check) public and minimal; everything else off.
- Structured, non-sensitive logging (no tokens, no passwords, no request bodies).
- Security headers (HSTS behind HTTPS, `X-Content-Type-Options`, `frame-options`, a CSP that fits the Vite bundle).

### 5. Delivery  -- DONE (image publish); deploy step waits for the provider
- CI builds the image and pushes it to GitHub Container Registry on `main` (tags: commit SHA + `latest`).
- Deploy workflow (manual trigger first, `workflow_dispatch`): deploys the SHA-tagged image to the chosen host; migrations run at app start.
- Secrets live in the host's secret store and GitHub Actions secrets, never in the repo.

### 6. Hosting and data safety  -- PARTLY DONE (runbook, backup + restore drill locally; hosting itself pending)
- Pick the provider (see "Open items"), create app + managed PostgreSQL, set env vars, deploy, check `/actuator/health`.
- HTTPS via the provider's certificate on its default hostname.
- Backups: provider-managed daily backups or scheduled `pg_dump`; **a restore is tested into a scratch database and recorded in `docs/`**.
- Runbook `docs/RUNBOOK.md`: deploy, rollback to a previous image, rotate JWT secret, restore a backup, add a custom domain.

## Implementation notes
- **Ownership:** `owner_id` on `category` and `task`; `CurrentUser` (from the JWT subject) scopes every repository call. Legacy rows belong to a placeholder user `local` that cannot sign in. Seed categories are now created per user at registration (the V1 seeds belong to `local`).
- **Auth:** HS256 JWT access tokens (15 min) via Spring Security's resource server; refresh tokens are random, stored as SHA-256, rotated, and reuse revokes all sessions. Login/registration failures are rate limited in memory (8 per 15 min per address+username).
- **Refresh token transport (decided):** web gets it only as an `HttpOnly; SameSite=Strict; Path=/api/v1/auth` cookie (`Secure` in prod); mobile sends `X-Token-Transport: body` and receives it in the JSON.
- **Registration:** `open` (dev/tests default), `invite`, `closed`; `prod` defaults to `invite`, and an unset invite code means nobody can register.
- **Secrets:** the base profile has a dev-only JWT secret; `prod` has none, so the app will not start without `IHELPER_JWT_SECRET`.
- **Ids across users:** `POST` with an id owned by someone else answers a plain 409 (no data, no hint of the owner); everything else answers 404.
- **SPA fallback** no longer swallows `/actuator/**` (found by `ProdProfileIT`).
- **Tests:** backend 68 unit/slice + 62 integration (cross-user isolation, auth flows, registration modes, prod profile); frontend 40; e2e 34 (every test registers its own user).
- **Known limits:** rate limiter is per instance (run one instance); no email, so password reset is a manual SQL step in `docs/RUNBOOK.md`; dev data from before this phase sits under the `local` user and is not visible to new accounts.

## Follow-up changes (after the Phase 6 review)
Requested by the user while using the app; implemented as one change (migration `V3`).
- **Starter categories:** only Personal, Ideas, Work for new accounts. Existing accounts keep what they have (decided: leave them, no data is removed).
- **Category order:** `category.position`; `PUT /api/v1/categories/order`; drag handle in the sidebar (`@dnd-kit`, pointer + keyboard). New categories are appended. `position` is part of the category DTO and `/sync`.
- **Home:** the old "All" entry is now called Home and shows every task of every category (Inbox included). Inbox stays as its own entry.
- **Whole row clickable:** clicking anywhere on a task row expands it and shows the notes ("No notes." if empty); a second click collapses it. The pencil icon opens the editor; the checkbox and trash icon do their own thing. The Done label is a green pill.
- **Done stays visible:** completing a task only sets its status to DONE. The default status filter is now "All" and finished tasks sort below the open ones of their priority group. Counts still show open tasks only.
- **Delete icon and Bin:** the trash icon on a row (and Delete in the editor) moves the task to the bin (the existing soft delete). Bin view: `GET /tasks/bin`, restore with `POST /tasks/{id}/restore`, empty with `DELETE /tasks/bin` (UI asks to confirm). `GET /tasks/counts` also returns `bin`.
- **Selective delete in the Bin (replaces "Empty bin"):** tick tasks (or Select all, which is the same as emptying the bin) and "Delete selected (N)" erases only those, after a confirmation, through `POST /api/v1/tasks/bin/purge {ids}`. Ids that are not in the caller's bin are ignored, so it is safe to repeat and cannot touch live tasks or other users' tasks.
- **Final deletion keeps a tombstone:** emptying the bin sets `purged_at` and wipes title, notes, category, due date and completion time; the row (id, priority, status, timestamps, `deletedAt`) remains so `/sync` can still tell an offline device about the deletion. Purged tasks cannot be restored. Soft deletes that happened before this change appear in the bin the first time it is opened.
- **Tests:** backend adds `TaskBinIT`, reorder cases in `CategoryApiIT`, service unit tests (75 unit/slice + 79 integration); frontend 54; e2e 49 (keyboard and pointer drag, bin, Home, expand row, edit icon).

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
