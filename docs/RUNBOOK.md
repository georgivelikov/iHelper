# iHelper runbook

Operations for the hosted app. Provider-neutral: the app is one Docker image plus a PostgreSQL database.
Commands that depend on the hosting provider are marked **(provider)**; fill them in when the host is chosen.

## Configuration (environment variables of the `prod` profile)

| Variable | Required | Meaning |
|---|---|---|
| `SPRING_PROFILES_ACTIVE` | yes | `prod` |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | yes | JDBC URL (`jdbc:postgresql://host:5432/db`) and credentials |
| `IHELPER_JWT_SECRET` | yes | at least 32 bytes, random. The app **refuses to start** without it |
| `IHELPER_REGISTRATION` | no | `invite` (default), `closed` or `open` |
| `IHELPER_INVITE_CODE` | when `invite` | shared code new users must enter. Empty = nobody can register |
| `IHELPER_SECURE_COOKIE` | no | `true` (default). Only set `false` for plain-HTTP local testing |
| `IHELPER_CORS_ORIGINS` | no | comma-separated; empty by default because the bundled UI is same-origin |
| `IHELPER_SWAGGER` | no | `true` exposes `/v3/api-docs` and `/swagger-ui`; off by default |

Generate a secret: `openssl rand -base64 48`.
Secrets live in the host's secret store and in GitHub Actions secrets, never in the repo or image.

## Health

`GET /actuator/health` is public and returns only `{"status":"UP"}` (plus liveness/readiness groups:
`/actuator/health/liveness`, `/actuator/health/readiness`). Use it as the platform health check.
Nothing else under `/actuator` is exposed.

## Deploy

1. Merge to `main`. CI runs backend, frontend and e2e jobs; when all pass it publishes
   `ghcr.io/<owner>/ihelper:<commit-sha>` and `:latest`.
2. Roll the host to the SHA tag **(provider)**. Flyway runs pending migrations at startup.
3. Check `/actuator/health`, then sign in.

Prefer deploying a SHA tag over `latest`, so every deploy is reproducible and rollback is a tag change.

## Roll back

1. Redeploy the previous SHA tag **(provider)**.
2. Migrations are forward-only. A rollback across a migration needs a **restore** (below) or a compensating
   migration, so keep migrations backwards compatible for one release (add columns before using them).

## Rotate the JWT secret

Changing `IHELPER_JWT_SECRET` invalidates every access token immediately; refresh tokens are opaque and stored
hashed, so they keep working and clients obtain new access tokens transparently.
To force everyone to sign in again, also run: `UPDATE refresh_token SET revoked_at = now() WHERE revoked_at IS NULL;`

## Manage users and invitations

- Change the invite code: update `IHELPER_INVITE_CODE`, redeploy. Existing users are unaffected.
- Stop all sign-ups: `IHELPER_REGISTRATION=closed`.
- Reset a forgotten password (no email flow exists): generate a BCrypt hash and set it, then tell the user:
  `UPDATE app_user SET password_hash = '<bcrypt hash>' WHERE lower(username) = lower('<name>');`
  and revoke their sessions: `UPDATE refresh_token SET revoked_at = now() WHERE user_id = (SELECT id FROM app_user WHERE lower(username)=lower('<name>'));`
- Login attempts are rate limited per client address and username (8 failures / 15 min), in memory per instance.
  Run a single instance, or move the limiter to shared storage before scaling out.

## Backups

Production needs **daily** backups with at least 7 days of retention, kept off the database host.
Use the provider's managed backups where available **(provider)**, and/or a scheduled logical dump:

```
pg_dump --format=custom --no-owner --file=ihelper-$(date +%F).dump "$DB_URL_WITHOUT_JDBC_PREFIX"
```

### Restore (tested)

A restore was exercised against the local PostgreSQL 17.6 container; row counts matched exactly
(users, categories, tasks, migration history):

```
docker exec ihelper-postgres pg_dump -U ihelper -Fc -d ihelper -f /tmp/ihelper.dump
docker exec ihelper-postgres psql -U ihelper -d postgres -c "CREATE DATABASE ihelper_restore_test"
docker exec ihelper-postgres pg_restore -U ihelper -d ihelper_restore_test --no-owner /tmp/ihelper.dump
# compare counts, then clean up
docker exec ihelper-postgres psql -U ihelper -d postgres -c "DROP DATABASE ihelper_restore_test"
```

(On Windows Git Bash prefix the commands with `MSYS_NO_PATHCONV=1`.)
Repeat this drill against the **hosted** database's backups once the provider is chosen, and record the date here:

- Local drill: 2026-10-11, passed.
- Hosted drill: _not yet done_.

To restore for real: stop the app, restore into a new database, point `DB_URL` at it, start the app, check
`/actuator/health`, and keep the old database until you are sure.

## Add a custom domain

1. Add the domain at the provider and create the DNS records it asks for **(provider)**; HTTPS certificates are
   issued by the platform.
2. Nothing in the app needs to change: the UI and API share one origin, cookies are host-only.
3. Only if a **separate** web origin is ever used, set `IHELPER_CORS_ORIGINS` to it.

## Security notes

- Access tokens: JWT (HS256), 15 minutes. Refresh tokens: random, 30 days, stored as SHA-256, rotated on every
  use; presenting an already used token ends all of that user's sessions.
- Web app keeps the refresh token in an `HttpOnly; Secure; SameSite=Strict` cookie scoped to `/api/v1/auth`.
  Mobile clients send `X-Token-Transport: body` to receive it in the response body instead.
- Another user's data answers **404**, never 403, and a taken id answers a plain 409.
- Passwords: BCrypt, 10-72 characters. Logs contain no tokens, passwords or request bodies.
