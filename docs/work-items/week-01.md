# Week 1: Deployable Skeleton and Hosting Spike

## Outcome

Produce one exact application skeleton that builds locally and runs on Render,
uses Neon PostgreSQL safely, serves Angular and Spring Boot under one origin,
and yields a written decision on SSE, memory, startup, and database behavior.

This week removes deployment uncertainty. It does not implement the product
workflow.

## Backlog

### W1-01: Verify and Pin the Toolchain

- Owner: project lead
- Depends on: none
- Owned paths: root version files and `docs/architecture/toolchain.md`
- Outcome: verify the plan's declared Java, Spring Boot, Springdoc, Angular,
  TypeScript, Node, JUnit, and Testcontainers compatibility against current
  primary documentation; pin exact versions and record the verification date.
- Done when: installed tools report the pinned versions and any mismatch with
  the plan is resolved explicitly before scaffolding.

### W1-02: Establish the Repository Skeleton

- Owner: project lead
- Depends on: W1-01
- Owned paths: repository root and empty top-level boundaries
- Outcome: create `backend/`, `frontend/`, `docs/architecture/`,
  `docs/decisions/`, and `infrastructure/`, plus secret-safe environment
  examples and a documented bootstrap command.
- Done when: a fresh clone has the intended layout, no credentials, and clear
  setup entry points.

### W1-03: Build the Backend Risk Probe

- Owner: backend implementation
- Depends on: W1-02
- Owned paths: `backend/**`
- Outcome: minimal Spring Boot application with Maven Wrapper, Flyway smoke
  migration, PostgreSQL integration test, health/readiness, OpenAPI, session and
  CSRF probe, and SSE heartbeat.
- Done when: backend verification passes against real PostgreSQL; Hikari is
  capped at three connections; connection timeouts are bounded; CSRF succeeds
  with a token and fails without one; SSE emits heartbeats.
- Review: fresh review required for security, database, and SSE behavior.

Keep Flyway, security, and SSE with the same backend owner. These concerns
share build, application, security, and integration-test files.

### W1-04: Build the Angular Risk Probe

- Owner: frontend implementation
- Depends on: W1-02
- Owned paths: `frontend/**`
- Outcome: minimal standalone Angular application with one lazy deep route,
  relative `/api` access, a development proxy, and minimal loading, error, and
  reconnecting states.
- Done when: install, unit tests, and production build pass; no NgRx, SSR, or
  workspace library is added.

### W1-05: Draft the Domain Contracts

- Owner: documentation
- Depends on: W1-01
- Owned paths: `docs/domain/**`
- Outcome: draft venue eligibility, lifecycle, read projection, and
  authorization tables from the project plan without inventing missing rules.
- Done when: every existing plan rule is represented and unknown fields are
  marked `TBD`; API-command and problem-response completion remains Week 2.

### W1-06: Integrate One Deployable Image

- Owner: integration
- Depends on: W1-03 and W1-04
- Owned paths: root build and deployment files; coordinated changes in both
  application boundaries
- Outcome: a multi-stage Docker build packages Angular into Spring Boot and
  serves the SPA, `/api`, actuator, assets, and SSE from one container.
- Done when: `/` and a direct deep route return the SPA, while API, actuator,
  and missing asset requests are never rewritten to `index.html`.
- Review: fresh cross-stack review required.

### W1-07: Pass the Local Acceptance Gate

- Owner: integration with an independent reviewer
- Depends on: W1-05 and W1-06
- Outcome: verify the exact image from a clean checkout, including bounded
  database failure and recovery.
- Done when: all recorded local checks pass; the review finds no feature creep,
  secrets, production CORS path, or incorrect SPA fallback.

### W1-08: Run the Hosting Spike

- Owner: approved operations
- Depends on: W1-07
- Outcome: deploy the exact image to one Render service with direct TLS Neon
  PostgreSQL and the `qify` Cloudflare CNAME, then measure the plan's hosting
  risks.
- Done when: dated evidence covers HTTPS, secure cookies, migration,
  persistence, restart, Neon resume, bounded timeout and recovery, cold start,
  JVM memory, a 20-minute idle SSE stream, reconnect behavior, and a conservative
  SSE cap. Root-domain DNS records remain unchanged.
- Review: provider and DNS mutations require explicit user approval and one
  operator. Serialize the experiments so one test does not invalidate another.

### W1-09: Record the Hosting Decision

- Owner: project lead with an independent reviewer
- Depends on: W1-08
- Outcome: record proceed or revise decisions for hosting, database, memory,
  live updates, quotas, billing controls, suspension behavior, log retention,
  and deployment triggers.
- Done when: every failed assumption has a Week 2 task or an explicit polling
  fallback decision, and the exact Week 1 skeleton remains the promoted code.

## Safe Parallelism

W1-05 may start after W1-01. The backend and frontend lanes may start after
W1-02. They may run in parallel only when their owned paths remain disjoint:

```text
W1-01
  |-> W1-02
  |     |-> W1-03 backend
  |     `-> W1-04 frontend
  `-> W1-05 domain documentation

W1-03 + W1-04 -> W1-06 -> W1-07 -> W1-08 -> W1-09
W1-05 ------------------------------------------^
```

The backend and frontend lanes may proceed in parallel after a baseline commit,
but only on disjoint paths. Use separate task branches or worktrees when needed.
The integration owner alone changes shared root and Docker files. Do not
parallelize deploy, restart, outage, SSE soak, or capacity measurements.

## Scope Guard

Reject these additions during Week 1:

- full request or assignment lifecycle;
- demo workspace expiry and invitation redemption;
- generated Angular REST client;
- full local seed data and Week 2 Compose environment;
- full CI contract;
- product styling or extra routes;
- `v1.1` idempotency and performance work;
- `v1.2` Floci, SQS, S3, or Terraform work.
