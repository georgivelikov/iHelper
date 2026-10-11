# iHelper: Architecture

iHelper is a **modular monolith**: one backend application, split internally into clean feature modules, with a separate TypeScript frontend.

```
 ┌──────────────────┐        ┌──────────────────────┐
 │  React web app   │        │ Mobile app (Phase 7) │
 │  (TypeScript)    │        │ React Native / Expo  │
 └────────┬─────────┘        └──────────┬───────────┘
          │    both use the shared      │
          │    `api-client` (generated  │
          │    from OpenAPI)            │
          └──────────────┬──────────────┘
                         │  HTTPS + JSON   /api/v1/...
                         ▼
 ┌─────────────────────────────────────────────────┐
 │        Spring Boot backend (ONE Java app)       │
 │                                                 │
 │  Controllers   ← HTTP, validation, DTOs, errors │
 │       │                                         │
 │  Services      ← business rules                 │
 │       │          (DONE → completedAt, sync...)  │
 │  Repositories  ← Spring Data JPA                │
 │                                                 │
 │  Modules:  task/   category/   sync/   common/  │
 └────────────────────────┬────────────────────────┘
                          │ JDBC
                          ▼
                 ┌──────────────────┐
                 │   PostgreSQL     │  (Docker container,
                 │   (one database) │   schema via Flyway)
                 └──────────────────┘
```

## Runtime by stage
- **Local dev:** React on Vite (`:5173`) proxies `/api` to Spring Boot (`:8080`), which talks to PostgreSQL in Docker.
- **Packaged / hosted:** the React build is bundled inside the Spring Boot jar. One container serves website and API, with one PostgreSQL next to it.
- **Mobile:** the phone app calls the same `/api/v1`. No server change needed.

## Backend layers
1. **Controller:** HTTP, input validation, DTO conversion, no business logic.
2. **Service:** business rules ("deleting a category moves tasks to Inbox", "stale version → conflict"). Target of unit tests.
3. **Repository:** database access. Target of slice and Testcontainers tests.

Each feature (`task`, `category`, `sync`) is its own package containing all three layers: organised by feature, not by technical type.

## Why not microservices
Microservices help when many teams deploy independently, parts need very different scaling, or the system is too large to reason about. None applies: one developer, one user, two entities, one database. They would add network calls that can fail, distributed transactions, service discovery, a gateway, and a much harder local setup and test story.

Because the modules are cleanly separated, a module (e.g. reminders/notifications once mobile push arrives) can be extracted into its own service later **if a real need appears**. Start with a monolith and split only with a reason.

## Cross-client API principles
See "REST API" in [PLAN.md](PLAN.md): versioned path, OpenAPI contract + generated client, stateless, client-generated UUIDs, sync-ready fields, optimistic-lock conflicts, pagination, Problem Details errors, UTC dates.
