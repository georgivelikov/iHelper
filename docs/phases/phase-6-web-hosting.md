# Phase 6: Web hosting

**Status:** Outline only. To be planned in detail when reached (after v1).

## Goal
iHelper reachable on the internet, securely, for the user only. Prerequisite for the mobile app (Phase 7).

## Outline
- `prod` Spring profile (PostgreSQL via environment variables, no secrets in the repo)
- Authentication: Spring Security, JWT access + refresh tokens (stateless, works for browser and mobile)
- Choose a host (small VPS / PaaS) and deployment method (Docker image from CI)
- HTTPS and domain, CORS locked to the web origin
- Database backups and restore test
- Monitoring/health endpoint (Spring Actuator), basic logging

## Open questions (decide when planning this phase)
- Hosting provider and budget
- Single user only, or accounts for others later?
- Social login or username/password?
