# Phase 7: Mobile

**Status:** Outline only. To be planned in detail when reached (after Phase 6).

## Goal
A mobile app (TypeScript) that uses the same `/api/v1` as the web app, works offline, and syncs.

## Why after Phase 6
A phone needs a reachable server and authentication. The API groundwork (client UUIDs, `version`, `deletedAt`, `/sync`, versioned path, generated `api-client`) is already built in Phases 2-3.

## Outline
- `mobile/` Expo + React Native + TypeScript in the same repo, reusing `api-client`
- Login using the Phase 6 JWT flow; tokens in secure storage
- Local SQLite store; offline create/edit using client-generated UUIDs
- Sync via `GET /sync?since=` plus pushing local changes; handle `409` conflicts
- Optional: push notifications for due dates (may require a notifications module in the backend)
- Test on a real phone with Expo Go, then build Android/iOS app

## Open questions (decide when planning this phase)
- Android only, or iOS too (affects store accounts and cost)?
- Conflict policy (server wins / last write wins / ask user)
- Notifications scope
