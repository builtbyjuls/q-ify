# Q-ify Full-Stack Portfolio Project Plan

**Owner:** Julius Cessar Lapugot

**Primary goal:** Demonstrate current senior Java engineering and end-to-end product delivery

**Core release target:** Eight focused weeks at about 20 hours per week

**Frontend:** Angular 21 LTS and TypeScript

**Public application:** `qify.builtbyjuls.com`
**AWS-compatible integration and IaC evidence:** Floci and Terraform in the planned `v1.2` local cloud lab

## Decision

Build Q-ify as a deployed full-stack simulation of a venue-authorized queue-handoff marketplace.

The core release will demonstrate one complete customer-to-partner workflow. The main engineering evidence will be explicit domain rules, atomic job claiming, object-level authorization, auditable live updates, and measurable tests.

Java and Spring Boot remain the primary technical signal. Angular provides a polished responsive interface and proves that the system can be delivered end to end. If weekly availability is below 20 focused hours, extend the calendar without adding features. Week 8 is reserved for stabilization and presentation rather than new product behavior.

## Portfolio Objective

Q-ify should show that I can design, secure, test, deploy, and explain a production-style full-stack system. My main signal remains senior Java engineering. Angular demonstrates that I can own the user-facing workflow and integrate it with the backend.

A hiring reviewer should find four concrete engineering decisions:

1. Guarded lifecycle transitions.
2. Race-safe partner assignment.
3. Role and object-level access control.
4. Live updates that recover after an interrupted connection.

The repository should favor depth over feature count.

## Product Boundary

Q-ify is a portfolio simulation for fictional private venues that explicitly permit queue handoffs. A customer creates a scheduled request, a Queue Partner claims it, the partner waits and reports progress, and the customer confirms the handoff.

All users, locations, fees, and domain events in the public demo are synthetic.

### Excluded Uses

- Government transactions and any activity that could be interpreted as fixer services.
- Transport boarding queues, tickets, limited-stock goods, identity checks, and document processing.
- Real payments, identity verification, background checks, and venue partnerships.
- Precise location verification or claims that browser geolocation proves physical presence.
- Any queue without explicit venue authorization.

The public README and demo landing page must state these limits. Expansion beyond fictional private venues would require written venue approval and Philippine legal and privacy review.

## Core Release Success Criteria

| Area | Required result |
| --- | --- |
| Product | A customer and partner complete the full queue lifecycle in separate browser sessions. |
| Correctness | Invalid transitions fail at the domain layer and terminal states remain immutable. |
| Concurrency | The database preserves both request and partner assignment limits during races. |
| Security | Role, projection, participant, and workspace tests block unauthorized access and data leakage. |
| Frontend | Five responsive route groups include validation, loading, empty, error, forbidden, and reconnecting states. |
| Operations | The public demo runs under one origin and exposes health checks and structured logs. |
| Evidence | The repository includes focused documentation, reproducible tests, screenshots, and a short demonstration video. |

## Core Users

### Customer

- Browse fictional venues and eligible services.
- Create a scheduled queue request with a fixed simulated fee.
- Monitor status and an immutable event timeline.
- Cancel while the lifecycle permits it.
- Confirm completion after handoff.

### Queue Partner

- View sanitized open requests.
- Claim one request and receive clear feedback if another partner wins.
- Use a simulated venue check-in.
- Submit structured progress values.
- Release or abandon an assignment under documented rules.

There is no administrator interface in the core release. Authorized customer and partner views show the event history needed for the portfolio demonstration.

## Angular Route Groups

| Route group | Purpose | Required states |
| --- | --- | --- |
| Public demo | Explain the simulation, start an isolated workspace, and redeem a partner invitation. | Normal, creation limited, invalid invite, unavailable demo |
| Customer request | Browse venues and submit a scheduled request. | Loading, empty, validation, API failure |
| Customer active request | Show status, event timeline, cancel, and confirm controls. | Connected, reconnecting, stale, terminal |
| Partner job board | List sanitized open work and support an atomic claim. | Loading, empty, conflict, refresh |
| Partner assignment | Record simulated check-in and progress commands. | Allowed, forbidden, released, expired, completed |

### Frontend Quality Rules

- Use standalone components and lazy-loaded feature routes.
- Use typed reactive forms for user input.
- Use Angular Material with a small custom theme rather than building a component library.
- Use Angular services and signals for feature state, with RxJS at HTTP and event-stream boundaries.
- Do not add NgRx to the core release. The application does not have enough shared client state to justify it.
- Provide mobile and desktop layouts, semantic labels, visible keyboard focus, and clear recovery actions.
- Treat route guards as navigation help. The backend remains the authority for access control.
- Use the generated Angular REST client for normal API calls and a small handwritten service for live events.

## Lifecycle and Business Rules

The main happy path is:

```text
OPEN
  -> CLAIMED
  -> EN_ROUTE
  -> WAITING
  -> HANDOFF_READY
  -> COMPLETED
```

`CANCELLED` and `EXPIRED` are terminal request states. A released assignment is terminal, but its request may return to `OPEN` before the claim deadline.

An assignment has a separate lifecycle:

```text
ACTIVE
  |-- RELEASED
  |-- COMPLETED
  |-- CANCELLED
  |-- EXPIRED
  `-- ABANDONED
```

Only `ACTIVE` is nonterminal. Each terminal assignment stores `endedAt` and an enumerated end reason.

### Main Transitions

| Current state | Command | Actor | Next state | Required condition |
| --- | --- | --- | --- | --- |
| New | Create request | Customer | OPEN | Venue and service are eligible; schedule is valid. |
| OPEN | Claim | Partner | CLAIMED | Partner is active, has no active assignment, and both atomic limits succeed. |
| CLAIMED | Start travel | Assigned partner | EN_ROUTE | Assignment is active and `arrivalBy` has not passed. |
| EN_ROUTE | Check in | Assigned partner | WAITING | Simulated check-in is confirmed before `arrivalBy`. |
| WAITING | Ready for handoff | Assigned partner | HANDOFF_READY | Queue position or estimated wait has been recorded. |
| HANDOFF_READY | Confirm handoff | Owning customer | COMPLETED | `handoffBy` has not passed. |

### Cancellation Release and Expiry

| Current state | Command | Actor | Next state | Required condition |
| --- | --- | --- | --- | --- |
| OPEN | Cancel | Owning customer | CANCELLED | `claimBy` has not passed. |
| CLAIMED | Cancel | Owning customer | CANCELLED | A structured cancellation reason is supplied. |
| EN_ROUTE | Cancel | Owning customer | CANCELLED | A structured cancellation reason is supplied. |
| WAITING | Cancel | Owning customer | CANCELLED | The customer acknowledges that waiting has started. |
| HANDOFF_READY | Cancel | Owning customer | CANCELLED | A structured no-show or cancellation reason is supplied. |
| CLAIMED | Release assignment | Assigned partner | OPEN or EXPIRED | Return to `OPEN` before `claimBy`; otherwise expire. |
| EN_ROUTE | Release assignment | Assigned partner | OPEN or EXPIRED | Return to `OPEN` before `claimBy`; otherwise expire. |
| WAITING | Abandon assignment | Assigned partner | CANCELLED | A structured reason is supplied. |
| OPEN | Expire | System | EXPIRED | `claimBy` has passed. |
| CLAIMED | Expire | System | EXPIRED | `arrivalBy` has passed. |
| EN_ROUTE | Expire | System | EXPIRED | `arrivalBy` has passed. |
| WAITING | Expire | System | EXPIRED | `handoffBy` has passed. |
| HANDOFF_READY | Expire | System | EXPIRED | `handoffBy` has passed without confirmation. |

### Lifecycle Invariants

- The owning customer can read the full customer projection of an owned request.
- An eligible partner can read only the minimized open-job projection.
- The assigned partner can read only the fields required to perform the assignment.
- Only the assigned partner can update an assignment.
- At most one active assignment can exist for a request.
- At most one active assignment can exist for a partner.
- Partner eligibility means that the participant is active and has no active assignment.
- Completing, cancelling, expiring, releasing, or abandoning a request must end its active assignment, if one exists, in the same transaction as the request update and audit event.
- Returning a request to `OPEN` must mark its old assignment `RELEASED` before another partner can claim it.
- Every successful transition stores actor, time, previous state, new state, and correlation identifier.
- Completed, cancelled, and expired requests cannot transition again.
- A dispute, if added later, has its own lifecycle and does not overwrite completion history.
- Store timestamps as `Instant` and display them in `Asia/Manila`.
- Inject `Clock` so expiry tests do not use real sleeping.
- Check expiry before every command and through a scheduled cleanup task.

Before implementation, expand these tables with required fields, emitted audit events, authorization checks, and rejected cases. The expanded table becomes the source for domain and API tests.

### Read Projections

| Reader | Permitted projection |
| --- | --- |
| Owning customer | Full customer view of owned requests and their event history. |
| Eligible partner | Venue, service, schedule, fixed simulated fee, and public status only. No customer identifier or private field. |
| Assigned partner | Fields needed to perform the assignment and the shared event history. No unnecessary customer details. |
| Any other user | No request data. |

API tests must prove that the open job board never exposes customer identifiers, authentication data, or private request fields.

Every request, assignment, audit event, reset command, and event subscription is scoped by `workspaceId`. Repository queries that read mutable demo data require the workspace identifier. Do not create an unscoped `listByStatus(OPEN)` operation.

### Structured Progress

Use validated fields such as queue position and estimated wait minutes. Use enumerated cancellation and abandonment reasons. Do not store free-form public notes.

Each `QueueService` stores one fixed simulated fee. Dynamic pricing, duration charges, discounts, and rounding rules are outside the core release.

### Simulated Check In

The partner confirms arrival through a clearly labeled simulation. The application will not claim that browser coordinates prove location or prevent fraud.

## Architecture

Use a modular monolith in one repository. This keeps local setup, transactions, tests, deployment, and review straightforward while preserving clear domain boundaries.

Microservices would add operational work without improving the core hiring evidence.

```text
qify/
  backend/
  frontend/
  docs/
    architecture/
    decisions/
  infrastructure/
  README.md
```

### Free-Tier Portfolio Deployment

The public demo targets zero additional compute and database cost while it remains within current provider allowances. The existing domain still has a separate renewal cost. The deployment uses one deployable application and one external database:

```text
Browser
  |
qify.builtbyjuls.com
  |
Cloudflare DNS lookup (DNS only)
  |
One Render web service
  |- Compiled Angular application
  |- Spring Boot REST API
  |- Spring Boot SSE endpoints
  |- Scheduled workspace cleanup
  |
Neon PostgreSQL
```

Hostinger remains the domain registrar and renewal provider. The existing Cloudflare nameservers remain configured at Hostinger. Cloudflare is the authoritative DNS provider. Render hosts the executable application, and Neon hosts PostgreSQL.

Add `qify.builtbyjuls.com` as a custom domain on the Render service. In Cloudflare, create a `CNAME` record named `qify` that targets the service's assigned `onrender.com` hostname. Keep the record set to `DNS only` during verification and retain Render-managed HTTPS. Adding this subdomain must not modify the existing root-domain records for `builtbyjuls.com`.

Build Angular into static assets and package those assets inside the Spring Boot application. Spring Boot serves the Angular application, relative `/api` requests, and SSE connections from the same origin. Configure an Angular route fallback to `index.html` without rewriting API, actuator, or static-asset requests.

Deploy one JVM, one Docker image, and one Render web service. Backend modules remain modules inside the Spring Boot modular monolith; they are not separately deployed microservices.

The free Render instance is a portfolio environment rather than a production environment. The deployment spike must account for its 512 MB memory limit, monthly allowances, and idle shutdown behavior. Keep the database connection pool small and set a measured JVM memory budget. Document Render's provider-controlled loading response while the service wakes, show a post-start notice explaining free-tier cold starts, and retain the recorded demonstration as a fallback. Do not add an uptime-pinging service.

Use a direct Neon JDBC URL with TLS required for both Flyway and the runtime datasource. Set `SPRING_FLYWAY_URL` explicitly so migrations never use a pooled PgBouncer URL. Cap Hikari at three connections and define bounded connection and validation timeouts. Week 1 must test first startup, migration, Neon resume after scale-to-zero, Render restart, and recovery from a database connection timeout.

Record the Render and Neon free-plan quotas in the deployment runbook. Do not add a billable payment method solely for Q-ify; if one already exists, configure every available spend limit and alert. Disable preview environments. Deploy only after protected-main CI passes by using Render's `checksPass` trigger or a manual release. If a quota is exhausted, allow suspension and direct reviewers to the recorded demonstration and local startup instructions.

### Local and CI Cloud Environment

Floci provides locally emulated AWS-compatible services for development and automated tests without requiring a paid cloud account:

```text
Local development and GitHub Actions
  |- Spring Boot
  |- Angular
  |- PostgreSQL
  |- Floci
     |- SQS with a dead-letter queue
     |- S3
```

Floci is not part of the public Render runtime. The public demo must remain usable when the developer computer and Floci are offline. Add the local cloud lab during `v1.2`, after the public `v1.0.0` workflow and focused `v1.1` evidence release are complete.

Use one pinned Floci container per acceptance run. Local development starts it through Docker Compose on `localhost:4566`. GitHub Actions starts the same pinned image on the same port. Terraform provisions resources into that instance before the Spring integration tests run, and destroys them afterward. The Spring adapters and Terraform AWS provider share only the same endpoint. Terraform authenticates with the seeded deployer credentials, creates the restricted runtime principal, and passes its access key and secret to the Spring test process without logging them. CI must not start a second Floci container or claim Terraform-backed coverage unless both provisioning and tests use the same instance.

Keep Floci fail closed:

- The Terraform root uses explicit SQS, S3, IAM, and STS endpoint overrides, static local credentials, path-style S3, disabled metadata and account lookups, and local state.
- The `local-cloud` Spring profile accepts only configured `http://localhost:4566` or Compose-network `http://floci:4566` endpoints and refuses startup for any other host.
- The public `demo` profile does not create AWS clients. A future real-AWS profile and Terraform root must be separate and require explicit activation and real credentials.
- Enable Floci IAM enforcement and its seeded deployer principal. Terraform creates a distinct runtime principal with only the required SQS and S3 actions. The application must not use Floci's default `test` access key because it bypasses policy enforcement.
- Run positive and negative authorization tests with the restricted runtime credentials created by Terraform. These demonstrate Floci's emulated policy behavior and intended configuration, not operation in real AWS.

The `v1.2` cloud lab is one cohesive audit-export workflow. An export request writes a transactional outbox record. A publisher sends the job to SQS. An idempotent consumer generates a synthetic audit artifact, stores it in S3, and updates a visible export status. Repeated failures route the message to a dead-letter queue. Do not use S3 for identity documents, customer uploads, or real personal data.

Keep the cloud boundary explicit through narrow application ports such as `AuditExportQueue` and `AuditArtifactStore`. The feature is enabled only by the `local-cloud` profile and is demonstrated locally, in CI, and in the recorded walkthrough. Floci proves AWS-compatible SDK integration, Terraform definitions, failure handling, and emulated policy tests; it does not prove that Q-ify operates in AWS.

### Backend Modules

| Module | Responsibility |
| --- | --- |
| `identity` | Demo workspaces, participants, invitations, authenticated sessions, and roles. |
| `venue` | Fictional venues, services, fixed fees, and eligibility policy. |
| `queue_request` | Customer request lifecycle, deadlines, and cancellation. |
| `assignment` | Partner eligibility, claim limits, release, and abandonment. |
| `tracking` | Persisted progress and live event delivery. |
| `audit` | Immutable event history exposed through authorized projections. |
| `shared` | Narrow technical primitives with no cross-domain business logic. |

### Database Enforcement

Use Flyway migrations to create PostgreSQL constraints and partial unique indexes for active assignments. The database must enforce:

- At most one active assignment per request.
- At most one active assignment per partner.

Define an active assignment as `endedAt IS NULL` and use that predicate in both partial unique indexes. Application checks improve error messages but do not replace the database guarantees.

Every request transition to `COMPLETED`, `CANCELLED`, or `EXPIRED` must set the active assignment's terminal status, end reason, and `endedAt` in the same transaction when an active assignment exists. An `OPEN` request may end without an assignment. Release must terminalize the old assignment before returning the request to `OPEN`. Integration tests must prove that a failed transaction cannot update only one side.

## Technology Choices

### Backend

- Java 21.
- Spring Boot 4.1.1, or the latest stable 4.1.x patch available when scaffolding.
- Maven Wrapper.
- PostgreSQL and Flyway.
- Spring Data JPA.
- Spring Security.
- Bean Validation.
- Springdoc OpenAPI 3.1.x, pinned to the exact patch verified with the selected Spring Boot version.
- Server-Sent Events with polling fallback.
- JUnit Jupiter 6 and Testcontainers 2, using the versions managed by Spring Boot.
- Docker Compose.
- AWS SDK for Java v2, a pinned Floci container, and Terraform in the `v1.2` local cloud lab.

Spring Boot 4.1 is the current supported stable line and supports Java 21. Use the Spring Boot 4.1 dependency management baseline for its managed JUnit and Testcontainers versions instead of overriding them independently. Pin the Spring Boot parent and Springdoc patches in `pom.xml`, verify `/v3/api-docs` and the PostgreSQL Testcontainers suite during Week 1, and record the resolved versions and rationale in the repository.

### Frontend

- Latest Angular 21 LTS patch.
- Angular CLI, Angular Material, and Angular Component Dev Kit on the same major.
- TypeScript 5.9 as required by Angular 21.
- Node 24 LTS with a compatible patch pinned in `.nvmrc`.
- Angular Router and `HttpClient` with functional interceptors.
- Typed reactive forms.
- Signals and computed signals for local and feature state.
- RxJS for HTTP composition and event-stream boundaries.
- Vitest, Angular TestBed, and Material component harnesses.
- Playwright for end-to-end tests.

Angular 21 is a deliberate enterprise-oriented choice. It is in long-term support, works with Node 24 and TypeScript 5.9, and avoids adopting a new major solely to appear current.

Do not add NgRx, server-side rendering, or a separate Angular workspace library during the core release.

### Angular Application Structure

```text
frontend/src/app/
  core/
    auth/
    http/
    errors/
    layout/
  shared/
    components/
    pipes/
  features/
    customer/
    partner/
```

Use `core` for authentication, interceptors, global error mapping, and the application shell. Keep `shared` limited to reusable presentation code. Each feature owns its routes, components, API facade, and feature state.

Signals hold view state. Computed signals derive display state. RxJS remains at asynchronous boundaries. Do not copy server data into a global store without a demonstrated need.

## API Contract

Spring controller annotations and DTOs are the code-first source for the OpenAPI contract. A repeatable backend task starts the application in a controlled profile and writes `openapi.json` from `/v3/api-docs`. A pinned OpenAPI Generator version creates the Angular REST client from that file.

Commit `openapi.json` and the generated client so a normal frontend build does not require a running backend. CI regenerates both and fails if the working tree differs. Never edit generated client files. Configure the client with a relative `/api` base path.

Server-Sent Events remain behind one handwritten Angular service because they are outside the generated REST client.

Use consistent problem responses containing a stable error code, human-readable message, HTTP status, field errors when relevant, and correlation identifier.

## Security Model

Deploy Angular and Spring Boot under one origin. Use relative `/api` URLs and an Angular development proxy so normal local requests remain relative.

Use:

- A `Secure`, `HttpOnly`, `SameSite=Lax` session cookie.
- A separate `Secure`, `SameSite=Lax` `XSRF-TOKEN` cookie that is intentionally readable by Angular.
- Spring Security `CookieCsrfTokenRepository.withHttpOnlyFalse()` and the SPA CSRF request handler.
- Angular's same-origin XSRF header behavior for relative mutating requests.
- CSRF token rotation or reload after session creation and termination.
- Request validation.
- Backend role, ownership, and projection checks.
- No normal development CORS path. The Angular proxy handles local API routing.

### Isolated Demo Workspaces

`DemoWorkspace` is the tenant boundary for all mutable demo data. `DemoParticipant` contains `participantId`, `workspaceId`, and role. Every authenticated session binds to exactly one participant. Ownership uses the session participant, never an identifier supplied by Angular.

Selecting `Start demo` creates a workspace and customer participant, then establishes the customer's authenticated session. The response provides a one-time partner invitation URL for a private window or separate browser profile. The token contains at least 128 bits of cryptographic randomness, expires after ten minutes, is stored only as a hash, and becomes unusable after redemption. Put the token in the URL fragment so it is not sent in the initial HTTP request or ordinary access log. Angular redeems it through a request body that application logging excludes. Redemption creates the partner participant and session in the same workspace.

Each workspace stores an immutable `expiresAt` set to 60 minutes after creation. Every workspace-scoped read, command, invitation redemption, reset, and SSE subscription rejects the workspace when `Clock.instant()` reaches `expiresAt`. This request-time rule is authoritative even when scheduled cleanup has not run. Only the unexpired workspace's customer participant can invoke the atomic `Reset my demo` action.

Scheduled cleanup performs storage reclamation only. Run a bounded cleanup batch after application startup and on the normal schedule, without making correctness depend on either run.

The application stores no free-form public text. Domain data is synthetic, but the hosting platform may still process real network metadata.

The public demo must:

- Use no analytics.
- Exclude request bodies, cookies, tokens, credentials, session identifiers, and demo codes from application logs.
- Document the hosting provider's log retention after the deployment spike.
- Keep deployment secrets in the hosting secret store.
- Commit only a redacted `.env.example`.
- Enforce request-size limits, a global active-workspace quota, cleanup indexes, and minimal throttling for workspace creation, invitation redemption, and reset.
- Limit each workspace to five requests and each request to fifty progress events.
- Limit each participant to thirty state-changing commands per minute.
- Permit one SSE stream per participant and two per workspace. Opening a replacement closes the participant's earlier stream.
- Set a global SSE connection cap from the measured Week 1 hosting result.
- Return HTTP 429 with a clear retry message when a public-demo limit is reached.

### Required Security Tests

- Customer A cannot read or cancel Customer B's request.
- Partner A cannot update Partner B's assignment.
- A customer cannot call partner commands.
- A mutating request without a valid CSRF token fails.
- An open-job response contains no customer identifier or private request field.
- Session creation and termination issue or refresh a usable CSRF token for the next mutating request.
- A participant cannot read, claim, update, reset, or subscribe to events outside its workspace.
- An expired workspace rejects reads, commands, invitation redemption, reset, and SSE subscription even when scheduled cleanup is delayed.
- An invitation cannot be reused, redeemed after expiry, or guessed through repeated attempts.
- The invitation-attempt throttle returns HTTP 429 after its documented limit.
- Request, progress-event, command-rate, workspace SSE, and participant SSE limits return HTTP 429 when exceeded.

## Primary Engineering Evidence

### State Correctness

The domain model owns transition rules. Controllers and Angular controls guide the user but do not define valid transitions. Every allowed and rejected transition has a unit test.

### Atomic Partner Claiming

Run two database-backed race tests:

1. Two partners claim one request. Exactly one succeeds and one returns HTTP 409.
2. One partner claims two requests concurrently. At most one claim succeeds.

Use PostgreSQL constraints plus an atomic conditional update or carefully tested optimistic locking. Repeat both tests enough times to expose race failures.

Integration tests must also prove that a partner can claim again after completion, cancellation, expiry, release, or abandonment; a released request can be reclaimed; and no terminal request retains an active assignment.

### Object Level Authorization

Roles establish broad capabilities. Ownership and projection checks decide which request and which fields a specific user can access. Tests execute against the real Spring Security configuration and PostgreSQL.

### Auditable Live Tracking

Persist the new state and audit entry before publishing a live notification.

Server-Sent Events are the preferred transport because status traffic is primarily server to browser. The Angular event service must reconnect and refetch canonical state after interruption. Duplicate notifications must not create duplicate stored events or duplicate timeline entries.

Close the stream on terminal state and Angular component destruction. Native `EventSource` does not expose the response status through its error event. On an SSE error, close the stream and call the canonical request API. If that normal HTTP call returns 401, show the expired-session screen and let the visitor start or rejoin a demo. Otherwise use a bounded reconnect policy and eventually switch to polling. Use polling as the documented fallback if the selected host cannot support reliable streaming.

## Operational Visibility

- Structured application logs.
- Correlation identifiers propagated through API and audit events.
- Spring Boot health and readiness endpoints.
- Angular error mapping that displays actionable recovery steps.
- No sensitive request or authentication data in logs.

## Test Contract

| Level | Required checks |
| --- | --- |
| Domain unit | Every valid and invalid lifecycle transition, fixed-fee retrieval, and terminal-state immutability. |
| PostgreSQL integration | Assignment terminalization, transaction atomicity, constraints, projections, ownership, workspace scoping, audit history, and expiry. |
| Concurrency | Both assignment races with database-enforced limits. |
| Security API | Sessions, CSRF, roles, participant ownership, workspace isolation, invitation redemption, field projection, public-demo limits, and validation. |
| Angular component | Request form, claim feedback, progress controls, and failure-state rendering. |
| Accessibility | Automated scan plus a documented manual keyboard and focus pass. |
| End to end | One successful lifecycle and one rejected or conflicting action. |

Do not target a headline coverage percentage. Each test must protect a business rule, integration boundary, or visible recovery path.

## Eight Week Delivery Plan

The schedule uses vertical slices so Angular and Spring integrate early. A local happy path works by Week 4. The first public release appears in Week 5. Job applications can reference Q-ify at that point.

### Week 1 Deployable Skeleton and Hosting Spike

**Objective:** Create the exact deployable skeleton and test the riskiest hosting assumptions before feature work.

- Create the monorepo, minimal Spring Boot application, minimal Angular application with one deep route, wrappers, and multi-stage Docker build.
- Add a Flyway smoke migration, Neon JDBC connection, health endpoint, minimal session and CSRF-protected probe, and SSE heartbeat endpoint.
- Draft the venue eligibility, lifecycle, projection, and authorization tables; complete them in Week 2.
- Deploy this exact skeleton to one free Render web service with Neon PostgreSQL. Promote it into Week 2 rather than replacing it with a throwaway probe.
- Verify the `qify.builtbyjuls.com` custom-domain path through Cloudflare DNS without changing the root-domain records.
- Verify one-origin routing, Angular deep links, Render-managed HTTPS, secure cookies, and Neon PostgreSQL persistence.
- Verify an SSE connection for at least twenty minutes with no REST requests or other client-originated traffic. Record proxy buffering, heartbeat delivery, whether the connection survives Render's idle window, reconnect behavior, and incremental deployment cost.
- If SSE does not survive, test active-browser polling as the free-tier fallback and document any reconnecting cold start. Do not generate traffic when no visitor is using the demo.
- Measure a safe global SSE connection cap for the selected host.
- Measure JVM memory and startup time within the Render free-instance limit, and document the cold-start user experience.
- Verify the direct TLS Neon connection, Flyway migration, database resume, Render restart, bounded connection timeout, and Hikari maximum of three connections.
- Record current Render and Neon quotas, billing controls, suspension behavior, preview-environment setting, and protected-main deployment trigger.

**Verification:** The exact application skeleton builds and runs locally and on Render, and a written spike result proves or rejects the intended hosting design.

### Week 2 Foundation Identity and Contract

**Objective:** Create a reproducible base with one authoritative API contract.

- Complete the venue eligibility, lifecycle, projection, authorization, API-command, and problem-response tables.
- Expand the Week 1 skeleton with local PostgreSQL, Docker Compose, seed data, and the production migration structure.
- Implement the state machine and transition tests.
- Add the Angular shell and route placeholders.
- Establish Spring Security sessions, CSRF bootstrap, demo workspace membership, participant principals, and Angular authentication state.
- Build every endpoint against the authenticated participant. Never accept a browser-supplied owner identifier.
- Implement OpenAPI export and pinned Angular client generation.
- Add backend, frontend, and contract-drift checks to GitHub Actions.

**Verification:** One command starts the stack, migrations succeed, a participant session and CSRF-protected command work, the generated client is reproducible, and CI passes.

### Week 3 Customer Slice

**Objective:** Complete the customer path through the real Angular application.

- Implement venue browsing and fixed-fee display.
- Implement request creation and customer request history.
- Connect typed Angular forms to the generated client.
- Add customer loading, empty, validation, and API error states.

**Verification:** An authenticated customer participant creates and reviews a workspace-scoped request through Angular using PostgreSQL.

### Week 4 Partner Slice and Happy Path

**Objective:** Complete the local customer-to-partner workflow.

- Implement the sanitized partner job board.
- Implement atomic claim and both race tests.
- Implement travel, simulated check-in, waiting, handoff-ready, and completion commands.
- Add partner conflict, forbidden, released, and terminal states.

**Verification:** Separate customer and partner browser sessions complete the happy path, and both assignment limits survive races.

### Week 5 Security and Public Release

**Objective:** Turn the local workflow into a credible public release.

- Harden role, participant ownership, workspace scope, and read projections.
- Add one-time invitation redemption, request-time workspace expiry, customer-owned reset, request and event limits, command throttling, SSE caps, the active-workspace quota, and bounded cleanup after startup.
- Add the cross-workspace, invitation, CSRF, projection, and throttle integration tests.
- Deploy one Docker image containing Angular and Spring Boot to one Render web service.
- Connect the service to Neon PostgreSQL and map `qify.builtbyjuls.com` through Cloudflare DNS.
- Confirm secret handling and document host log retention.
- Tag `v0.1.0`.

**Verification:** The public demo works, cross-participant and cross-workspace access fails, and a fresh demo workspace can be reset safely.

### Week 6 Live Tracking and Failure Paths

**Objective:** Make the workflow recover from delays, cancellation, release, and connection interruption.

- Persist event history and add SSE with heartbeat and bounded reconnect.
- Implement canonical-state refetch and polling fallback.
- Add customer cancellation, partner release or abandonment, and expiry for every nonterminal state.
- Complete mobile layouts and all reconnecting, stale, conflict, forbidden, and terminal states.

**Verification:** Both roles receive live updates, recover after forced interruption, and cannot leave a request stuck indefinitely.

### Week 7 Hardening

**Objective:** Complete the required quality and operational evidence.

- Complete the test contract.
- Add structured logs, correlation identifiers, and health endpoints.
- Run the automated accessibility scan and manual keyboard pass.
- Verify sensitive data is absent from logs.
- Test deployment recovery and demo workspace cleanup.

**Verification:** CI is reliable, the accessibility results are documented, and the deployed demo returns to a known state.

### Week 8 Stabilization and Presentation

**Objective:** Finish and explain the existing work without adding product behavior.

- Fix defects found during a complete release rehearsal.
- Finish the README, architecture diagram, lifecycle diagram, and decision records.
- Add screenshots and concise setup instructions.
- Record a three-minute demonstration.
- Tag `v1.0.0`.

**Verification:** A reviewer can understand, run, test, and assess Q-ify without asking for missing setup or context.

## Application Timing

Begin linking Q-ify in applications after the Week 5 deployment. Continue improving the same public repository while applying and interviewing.

## Required Portfolio Material

- Public demo URL and isolated demo instructions.
- `qify.builtbyjuls.com` custom-domain and free-tier deployment documentation, including domain renewal, quotas, billing controls, cold starts, and suspension behavior.
- Screenshots or one short animated demonstration.
- Three-minute walkthrough focused on the lifecycle and concurrent claim.
- One-command local startup.
- Green GitHub Actions status.
- One system architecture diagram.
- One lifecycle diagram.
- Generated OpenAPI document.
- Three substantive architecture decision records linked to code and tests.
- Test commands, security limits, and known limitations.
- Small commits with descriptive messages and issues tied to vertical slices.

Recommended architecture decisions:

1. Modular monolith instead of microservices.
2. PostgreSQL assignment constraints and claim strategy.
3. Server-Sent Events, reconnection, and polling fallback.
4. One Render service with bundled Angular assets and Neon PostgreSQL.
5. Locally emulated AWS-compatible integration and Terraform tests without claiming real AWS operation.

## Suggested Portfolio Description

> Q-ify is a full-stack queue-handoff marketplace simulation built with Java 21, Spring Boot, PostgreSQL, Angular, and TypeScript. It demonstrates guarded workflow transitions, database-enforced assignment limits, object-level authorization, auditable live updates, and full-stack integration testing.

## Version 1.1 Focused Evidence Release

Start this work only after `v1.0.0` is public and job applications are active.

- Add idempotency to request creation and partner progress commands.
- Expand production-grade rate limits only if deployment evidence shows the core public-demo limits are insufficient.
- Produce one measured PostgreSQL query improvement with generated data, query plans, and before-and-after results.

## Version 1.2 Local Cloud Lab

Start this work only after `v1.1` is complete. Treat it as one audit-export vertical slice rather than a collection of cloud demonstrations.

- Add the audit-export request, visible status, and transactional outbox.
- Provision Floci SQS, its dead-letter queue, S3, the deployer and runtime principals, and narrow runtime policies with Terraform.
- Publish export jobs to SQS and process them with an idempotent consumer.
- Store only generated synthetic audit artifacts in S3.
- Demonstrate bounded retry and a poison message reaching the dead-letter queue.
- Enable Floci IAM enforcement and prove one allowed and one denied runtime action.
- Run Terraform and the Spring integration tests against the same pinned Floci instance in GitHub Actions.
- Document the explicit local endpoints, dummy credentials, local Terraform state, and fail-closed Spring profile.
- State that this release demonstrates AWS-compatible integration and IaC against an emulator, not a workload operating in AWS.

## Later Product Extensions

- Administrator venue management.
- Ratings and a separate dispute lifecycle.
- Dynamic pricing.
- OAuth or OpenID Connect.
- Production infrastructure automation beyond the local Floci resources.
- Real AWS deployment. Floci remains the default cloud development and CI environment until a real deployment has a justified budget.

## Permanent Exclusions

- Real government workflows.
- Real payments and identity documents.
- Precise location tracking presented as fraud prevention.
- Native mobile applications before the web release is complete.
- Microservices and Kubernetes without a demonstrated operational need.

## Delivery Risks

| Risk | Control |
| --- | --- |
| Frontend work expands. | Keep five route groups and integrate controls into them instead of adding pages. |
| Angular architecture becomes elaborate. | Use standalone components, feature services, signals, and RxJS. Keep NgRx out of the core. |
| Security consumes the schedule. | Use one-origin, workspace-bound participant sessions. Exclude registration, passwords, and external identity providers. |
| Live updates fail behind the host. | Test the host in Week 1 and keep polling as a documented fallback. |
| The free Render instance starts slowly, exceeds memory, or is suspended after a quota. | Bundle Angular into the Spring Boot service, cap Hikari at three connections, measure the JVM budget, explain cold starts and quotas, and retain a demonstration video. |
| Render sleeps before scheduled cleanup runs. | Enforce `expiresAt` on every workspace operation and use scheduled cleanup only for storage reclamation. |
| Cloud work expands the core release. | Complete `v1.0.0` and `v1.1` first, then implement one Floci-backed audit-export slice in `v1.2`. |
| Demo visitors collide. | Bind participants and all mutable data to isolated workspaces, expire them, and provide a customer-owned atomic reset. |
| Documentation delays shipping. | Write only documents tied to real decisions and reserve Week 8 for presentation. |

## Sources

- [Spring Boot 4.1 system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot managed dependency versions](https://docs.spring.io/spring-boot/appendix/dependency-versions/)
- [Springdoc compatibility matrix](https://github.com/springdoc/springdoc.github.io/blob/master/src/docs/asciidoc/faq.adoc)
- [Spring Security CSRF guidance](https://docs.spring.io/spring-security/reference/servlet/exploits/csrf.html)
- [Angular version compatibility](https://angular.dev/reference/versions)
- [Angular release and support policy](https://angular.dev/reference/releases)
- [Angular signals guide](https://angular.dev/guide/signals)
- [Angular testing guide](https://angular.dev/guide/testing)
- [Render free service limits](https://render.com/docs/free)
- [Render Cloudflare DNS configuration](https://render.com/docs/configure-cloudflare-dns)
- [Render build pipeline](https://render.com/docs/build-pipeline)
- [Render Blueprint deployment triggers](https://render.com/docs/blueprint-spec)
- [Neon pricing and free plan](https://neon.com/pricing)
- [Neon connection pooling and migration guidance](https://neon.com/docs/connect/connection-pooling)
- [Floci AWS emulator](https://github.com/floci-io/floci)
- [Floci IAM enforcement](https://github.com/floci-io/floci/blob/main/docs/services/iam.md)
- [GitHub Actions billing and usage](https://docs.github.com/en/actions/concepts/billing-and-usage)
- [Republic Act No. 11032](https://lawphil.net/statutes/repacts/ra2018/ra_11032_2018.html)
- [National Privacy Commission Data Privacy Act guidance](https://privacy.gov.ph/data-privacy-act/)

## Final Release Decision

Q-ify is ready for feature implementation when the Week 1 deployable skeleton and hosting-spike results are complete. Week 2 then completes the lifecycle, projection, and authorization contract before the vertical slices expand. Keep `v1.0.0` limited to one polished workflow. Add later features only when they create visible engineering evidence or improve the portfolio demonstration.
