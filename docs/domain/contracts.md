# Q-ify Domain Contract Draft

- Status: draft
- Work item: DOMAIN-01
- Canonical source: [Q-ify project plan](../project-plan.md)
- Completion milestone: M2 - Identity and Contract Foundation

## How to Read This Draft

This document extracts the venue eligibility, lifecycle, read-projection, and
authorization rules already selected in the project plan. It does not add
product behavior.

- `Defined` means the project plan states the rule.
- `TBD` means the project plan does not yet decide the value or field. `TBD`
  is not permission to choose a default during implementation.
- Command labels below describe domain intent. They do not define HTTP paths,
  request DTOs, response DTOs, or public error codes.
- Unless a status is stated here, HTTP status and problem-response decisions
  remain M2 work.
- All mutable request, assignment, audit, reset, and event-subscription behavior
  is subject to the workspace and authorization guards in this document.

## Venue and Service Eligibility

| ID | Subject | Defined rule | Rejection or exclusion | Fields still TBD |
| --- | --- | --- | --- | --- |
| VE-01 | Demo data | All users, locations, fees, and domain events are synthetic. | Real personal, payment, identity, or venue-partnership data is outside the public demo. | Exact seed records and identifiers. |
| VE-02 | Venue | A venue is a fictional private venue that explicitly permits queue handoffs. | Any queue without explicit venue authorization is ineligible. | How permission is represented, reviewed, activated, or withdrawn. |
| VE-03 | Use category | The simulation covers a venue-authorized queue handoff only. | Government or fixer activity; transport boarding queues; tickets; limited-stock goods; identity checks; document processing; real payments; identity verification; background checks; and real venue partnerships are excluded. | None for the exclusion list. |
| VE-04 | Queue service | A customer may select only an eligible service at an eligible venue. | An ineligible venue or service rejects request creation. | Service eligibility fields, venue-service relationship fields, and activation rules. |
| VE-05 | Schedule | Request creation requires a valid schedule. | An invalid schedule rejects request creation. | Input fields, lead time, operating hours, accepted range, timezone input, and deadline derivation. |
| VE-06 | Simulated fee | Each `QueueService` stores one fixed simulated fee. | Dynamic pricing, duration charges, discounts, and rounding rules are outside the core release. | Currency, storage type, scale, and API representation. |
| VE-07 | Progress text | Progress uses validated structured fields. The application stores no free-form public notes or other free-form public text. | Free-form fields are outside the permitted domain data and must not be stored. | Exact field types, ranges, unexpected-input handling, and validation messages. |
| VE-08 | Check-in | Partner arrival is a clearly labeled simulation. | The application must not claim that browser coordinates prove presence or prevent fraud. | Exact confirmation field and user-facing wording. |
| VE-09 | Administration | The core release has no administrator interface. | Administrator venue management is a later extension. | Seed-data maintenance process for the core release. |
| VE-10 | Expansion | Expansion beyond fictional private venues requires written venue approval and Philippine legal and privacy review. | Unreviewed expansion remains excluded. | Review evidence and approval process. |

The public README and demo landing page must state these simulation limits.

The eligible-partner projection exposes only the venue and service information
listed in the [read-projection allowlist](#read-projection-allowlist). Unknown
entity fields must not be exposed merely because their contract is `TBD`.

## Domain States

### Request States

`New` is only the creation context used by RT-01. It is not a request status,
and this draft does not add a persisted state before `OPEN`.

| State | Terminal | Defined meaning | Still TBD |
| --- | --- | --- | --- |
| `OPEN` | No | Eligible for a partner claim before `claimBy`. | Exact public status representation. |
| `CLAIMED` | No | A partner has an active assignment. | Exact claim metadata. |
| `EN_ROUTE` | No | The assigned partner has started travel. | Exact travel metadata. |
| `WAITING` | No | Simulated check-in has been confirmed and the partner is waiting. | Exact check-in metadata. |
| `HANDOFF_READY` | No | Queue position or estimated wait has been recorded and handoff is ready. | Whether both progress fields may be required by a later rule. |
| `COMPLETED` | Yes | The owning customer confirmed the handoff before `handoffBy`. | Exact completion metadata. |
| `CANCELLED` | Yes | An allowed cancellation or abandonment ended the request. | Exact cancellation metadata and reason values. |
| `EXPIRED` | Yes | A request deadline passed without the required transition. | Exact expiry metadata and late-command behavior. |

`COMPLETED`, `CANCELLED`, and `EXPIRED` requests are immutable. A later dispute,
if added, has a separate lifecycle and must not overwrite completion history.

### Assignment States

| State | Terminal | Defined rule | Trigger mapping still TBD |
| --- | --- | --- | --- |
| `ACTIVE` | No | The only nonterminal assignment state. An active assignment has `endedAt IS NULL`. | Exact creation fields and start time. |
| `RELEASED` | Yes | The old assignment must be `RELEASED` before its request returns to `OPEN`. | End-reason enum value and the late-release mapping. |
| `COMPLETED` | Yes | A defined terminal assignment state. | Exact mapping from request completion and end-reason enum value. |
| `CANCELLED` | Yes | A defined terminal assignment state. | Exact mapping from customer cancellation and end-reason enum value. |
| `EXPIRED` | Yes | A defined terminal assignment state. | Exact mapping from request expiry and end-reason enum value. |
| `ABANDONED` | Yes | A defined terminal assignment state associated with abandonment behavior. | Exact mapping and end-reason enum value. |

Every terminal assignment stores `endedAt` and an enumerated end reason. A
partner becomes claim-eligible again after completion, cancellation, expiry,
release, or abandonment, subject to the participant-active rule and all other
claim guards.

### Deadlines and Time

| Field | Defined use | Boundary | Still TBD |
| --- | --- | --- | --- |
| `claimBy` | Bounds open cancellation, claiming, release-to-`OPEN`, and open-request expiry. | The plan uses both "before" and "has passed." | Calculation and exact equality behavior. |
| `arrivalBy` | Bounds travel, simulated check-in, and expiry from `CLAIMED` or `EN_ROUTE`. | Start travel and check-in require it not to have passed. | Calculation and exact equality behavior. |
| `handoffBy` | Bounds customer confirmation and expiry from `WAITING` or `HANDOFF_READY`. | Confirmation requires it not to have passed. | Calculation and exact equality behavior. |
| Workspace `expiresAt` | Rejects every workspace-scoped operation when reached. | Reject when `Clock.instant() >= expiresAt`. | Identifier and persistence schema only; duration is defined as 60 minutes. |

Store domain timestamps as `Instant` and display them in `Asia/Manila`. Inject
`Clock`; expiry tests must not sleep against the real clock.

## Request Lifecycle

`Reject` below means the domain transition must not occur. The exact exception,
HTTP status, stable problem code, and response body are `TBD` for M2 unless
this document states otherwise.

### Main Flow

| ID | Current | Domain command | Authorized actor | Required data and conditions | Next | Assignment effect | Rejected case | Audit event |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| RT-01 | New | Create request | Customer | Eligible venue and service; valid schedule; fixed simulated fee. Exact request fields are `TBD`. | `OPEN` | Exact representation before claim is `TBD`. | Reject an ineligible venue/service or invalid schedule. | Transition audit; event name and payload are `TBD`. |
| RT-02 | `OPEN` | Claim | Eligible partner | Participant is active, has no active assignment, and both database-backed atomic limits succeed. | `CLAIMED` | An `ACTIVE` assignment must result; exact creation fields are `TBD`. | Reject an ineligible partner, stale state, passed deadline, or failed atomic limit. A race loser returns HTTP 409. | Transition audit; event name and payload are `TBD`. |
| RT-03 | `CLAIMED` | Start travel | Assigned partner | Assignment is active and `arrivalBy` has not passed. | `EN_ROUTE` | Remains active. | Reject a different actor, inactive assignment, stale state, or passed deadline. | Transition audit; event name and payload are `TBD`. |
| RT-04 | `EN_ROUTE` | Check in | Assigned partner | Clearly simulated check-in is confirmed before `arrivalBy`. | `WAITING` | Remains active. | Reject a different actor, stale state, unconfirmed check-in, or passed deadline. | Transition audit; event name and payload are `TBD`. |
| RT-05 | `WAITING` | Ready for handoff | Assigned partner | At least queue position or estimated wait has been recorded. | `HANDOFF_READY` | Remains active. | Reject a different actor, stale state, or missing required progress. | Transition audit; event name and payload are `TBD`. |
| RT-06 | `HANDOFF_READY` | Confirm handoff | Owning customer | `handoffBy` has not passed. | `COMPLETED` | End any active assignment in the same transaction; exact terminal mapping is `TBD`. | Reject a different actor, stale state, or passed deadline. | Transition audit; event name and payload are `TBD`. |

### Cancellation, Release, Abandonment, and Expiry

| ID | Current | Domain command | Authorized actor | Required data and conditions | Next | Assignment effect | Rejected case | Audit event |
| --- | --- | --- | --- | --- | --- | --- | --- | --- |
| RT-07 | `OPEN` | Cancel | Owning customer | `claimBy` has not passed. | `CANCELLED` | An `OPEN` request may end without an assignment; if one exists, terminalize it atomically. Exact mapping is `TBD`. | Reject a different actor, stale state, or passed deadline. | Transition audit; event name and payload are `TBD`. |
| RT-08 | `CLAIMED` | Cancel | Owning customer | Structured cancellation reason. | `CANCELLED` | End the active assignment in the same transaction; exact terminal mapping is `TBD`. | Reject a different actor, stale state, or missing/invalid reason. | Transition audit; event name and payload are `TBD`. |
| RT-09 | `EN_ROUTE` | Cancel | Owning customer | Structured cancellation reason. | `CANCELLED` | End the active assignment in the same transaction; exact terminal mapping is `TBD`. | Reject a different actor, stale state, or missing/invalid reason. | Transition audit; event name and payload are `TBD`. |
| RT-10 | `WAITING` | Cancel | Owning customer | Customer acknowledges that waiting has started. | `CANCELLED` | End the active assignment in the same transaction; exact terminal mapping is `TBD`. | Reject a different actor, stale state, or missing acknowledgement. | Transition audit; event name and payload are `TBD`. |
| RT-11 | `HANDOFF_READY` | Cancel | Owning customer | Structured no-show or cancellation reason. | `CANCELLED` | End the active assignment in the same transaction; exact terminal mapping is `TBD`. | Reject a different actor, stale state, or missing/invalid reason. | Transition audit; event name and payload are `TBD`. |
| RT-12 | `CLAIMED` | Release assignment | Assigned partner | Before `claimBy`, return to `OPEN`; otherwise expire. | `OPEN` or `EXPIRED` | Terminalize the old assignment before any return to `OPEN`; `RELEASED` is explicit for that path. Other mapping is `TBD`. | Reject a different actor, stale state, or inactive assignment. | Transition audit; event name and payload are `TBD`. |
| RT-13 | `EN_ROUTE` | Release assignment | Assigned partner | Before `claimBy`, return to `OPEN`; otherwise expire. | `OPEN` or `EXPIRED` | Terminalize the old assignment before any return to `OPEN`; `RELEASED` is explicit for that path. Other mapping is `TBD`. | Reject a different actor, stale state, or inactive assignment. | Transition audit; event name and payload are `TBD`. |
| RT-14 | `WAITING` | Abandon assignment | Assigned partner | Structured abandonment reason. | `CANCELLED` | End the active assignment in the same transaction; exact terminal mapping is `TBD`. | Reject a different actor, stale state, or missing/invalid reason. | Transition audit; event name and payload are `TBD`. |
| RT-15 | `OPEN` | Expire | System | `claimBy` has passed. | `EXPIRED` | No assignment is required; if one exists, terminalize it atomically. Exact mapping is `TBD`. | Do not expire before the deadline or from a stale state. | Transition audit; event name and payload are `TBD`. |
| RT-16 | `CLAIMED` | Expire | System | `arrivalBy` has passed. | `EXPIRED` | End the active assignment in the same transaction; exact terminal mapping is `TBD`. | Do not expire before the deadline or from a stale state. | Transition audit; event name and payload are `TBD`. |
| RT-17 | `EN_ROUTE` | Expire | System | `arrivalBy` has passed. | `EXPIRED` | End the active assignment in the same transaction; exact terminal mapping is `TBD`. | Do not expire before the deadline or from a stale state. | Transition audit; event name and payload are `TBD`. |
| RT-18 | `WAITING` | Expire | System | `handoffBy` has passed. | `EXPIRED` | End the active assignment in the same transaction; exact terminal mapping is `TBD`. | Do not expire before the deadline or from a stale state. | Transition audit; event name and payload are `TBD`. |
| RT-19 | `HANDOFF_READY` | Expire | System | `handoffBy` has passed without confirmation. | `EXPIRED` | End the active assignment in the same transaction; exact terminal mapping is `TBD`. | Do not expire before the deadline or after valid confirmation. | Transition audit; event name and payload are `TBD`. |

### Lifecycle, Transaction, and Audit Invariants

| ID | Defined invariant | Required evidence | Still TBD |
| --- | --- | --- | --- |
| LI-01 | The domain model, not controllers or Angular controls, owns transition validity. | A domain unit test for every allowed and rejected transition. | Domain type and method names. |
| LI-02 | Terminal requests never transition again. | Terminal-state immutability tests. | Exact domain rejection type. |
| LI-03 | Check expiry before every command and through scheduled cleanup. Request-time checks remain authoritative. | Injected-`Clock` tests without real sleeping. | Whether a late command first persists expiry or only rejects. |
| LI-04 | Every successful transition stores actor, time, previous state, new state, and correlation identifier. | Audit-history integration tests. | Event names, identifiers, payload, ordering key, and retention. |
| LI-05 | Persist the new state and audit entry before publishing a live notification. | Transaction and live-delivery boundary tests. | Whether every nonterminal transition shares one transaction; the plan is explicit for terminal, release, and abandonment paths. |
| LI-06 | Completing, cancelling, expiring, releasing, or abandoning ends any active assignment in the same transaction as request state and audit changes. | A failed transaction cannot update only one side; no terminal request retains an active assignment. | Exact terminal assignment mapping except the release-to-`OPEN` rule. |
| LI-07 | Release terminalizes the old assignment before returning the request to `OPEN`. | A released request can be reclaimed without retaining the old active assignment. | Claim strategy and persistence operation names. |
| LI-08 | PostgreSQL enforces at most one active assignment per request and per partner with partial unique indexes using `endedAt IS NULL`. | Real PostgreSQL constraint and race tests. | Table, column, constraint, and index names. |
| LI-09 | Application checks may improve feedback but never replace the database assignment limits. | Two-partner/one-request and one-partner/two-request repeated race tests. | Atomic conditional update versus carefully tested optimistic locking. |
| LI-10 | A race for one request has exactly one winner and one HTTP 409 loser. A partner racing for two requests wins at most one. | Repeated PostgreSQL-backed concurrency tests. | Stable problem code and response body. |
| LI-11 | A partner can claim again after completion, cancellation, expiry, release, or abandonment. | PostgreSQL integration tests for every terminal assignment outcome. | Exact participant activation lifecycle. |
| LI-12 | Duplicate live notifications create neither duplicate stored events nor duplicate timeline entries. | Reconnect and duplicate-delivery tests. | Event identity, replay cursor, and deduplication key. |
| LI-13 | Request, assignment, and audit records and their repository reads remain scoped by `workspaceId`. | Cross-workspace and repository-query tests. | Identifier types and schema. |

## Structured Progress and Live Delivery

| Subject | Defined contract | Limit | Still TBD |
| --- | --- | --- | --- |
| Queue position | A validated structured progress field that may satisfy readiness for handoff. | Counts toward fifty progress events per request. | Type, range, and validation message. |
| Estimated wait | A validated number of minutes that may satisfy readiness for handoff. | Counts toward fifty progress events per request. | Type, range, and validation message. |
| Cancellation and abandonment reasons | Enumerated, not free-form. | State-changing command rate applies. | Enum members and state-specific subsets. |
| Stored timeline | Immutable, workspace-scoped, and projected only to authorized readers. | Fifty progress events per request. | Event schema, ordering key, and retention. |
| Live notification | Published only after state and audit persistence. | One SSE stream per participant, two per workspace, plus a measured global cap. | Event schema, heartbeat, replay, and reconnect timings. |
| Reconnection | Refetch canonical state after interruption; duplicates do not duplicate stored history. Eventually use polling after bounded reconnect. | No traffic when no visitor uses the demo. | Retry schedule, polling interval, and stale threshold. |

Close a live stream on terminal request state and Angular component destruction.
On an SSE error, close the stream and call the canonical request API. A 401
from that call selects the expired-session flow; otherwise use bounded reconnect
and eventually the documented polling fallback.

## Read-Projection Allowlist

| Reader | Eligibility and object relation | Allowed request data | Explicitly forbidden | Fields still TBD |
| --- | --- | --- | --- | --- |
| Owning customer | Authenticated customer participant; owns the request; same unexpired workspace. | Full customer view of owned requests and immutable event history. | Any request owned by another customer or stored in another workspace. | Complete customer field allowlist. |
| Eligible partner | Authenticated active partner; no active assignment; same unexpired workspace; request is open. | Venue, service, schedule, fixed simulated fee, and public status only. | Customer identifier, authentication data, private request fields, and cross-workspace data. | Exact venue/service/schedule/public-status field names and authoritative private-field list. |
| Assigned partner | Authenticated partner assigned to the request; same unexpired workspace. | Only fields needed to perform the assignment plus shared event history. | Unnecessary customer details, another partner's assignment, and cross-workspace data. | Complete assigned-partner field and shared-history allowlists. |
| Any other user | Does not satisfy one of the relations above. | No request data. | All request, assignment, and event-history fields. | Authorization failure mapping: 403 versus concealed 404. |

These projections are allowlists. Entity serialization must not fill a `TBD`
by exposing every stored field. API tests must prove that the open-job response
contains no customer identifier, authentication data, or private request field.

Every query that reads mutable demo data requires `workspaceId`. In particular,
an unscoped `listByStatus(OPEN)` repository operation is forbidden.

## Authorization Matrix

| Operation | Authorized actor | Required relation and guard | Required denial | Still TBD |
| --- | --- | --- | --- | --- |
| Browse eligible venues/services | Customer capability in the core workflow. | Show only the eligible fictional catalog. | Exclude ineligible uses and services. | Whether catalog browsing requires an established participant session. |
| Create request | Customer. | Authenticated participant and unexpired workspace; VE-01 through VE-08; workspace request limit. | Reject another role, expired workspace, invalid eligibility/schedule, or exceeded limit. | Exact command/API contract. |
| Read customer request/history | Owning customer. | Session participant owns the request in the same unexpired workspace. | Customer A cannot read Customer B's request. | Exact field allowlist and 403/404 mapping. |
| Read open-job board | Eligible partner. | Active participant, no active assignment, same unexpired workspace, open request. | Exclude customer/auth/private fields and all cross-workspace requests. | Exact query and projection fields. |
| Read assigned request/history | Assigned partner. | Session participant is the request's assigned partner in the same unexpired workspace. | Partner A cannot read Partner B's assignment data. | Whether access continues after assignment termination, plus the exact field allowlist. |
| Claim request | Eligible partner. | Same workspace; request `OPEN`; before applicable expiry; both atomic assignment limits. | A customer cannot claim; cross-workspace and stale claims fail; race loser returns 409. | Error code/body and claim strategy. |
| Start travel | Assigned partner. | RT-03 and same unexpired workspace. | A different partner, customer, stale state, or expired workspace fails. | API contract and failure mapping. |
| Check in | Assigned partner. | RT-04 and same unexpired workspace. | A different partner, customer, stale state, or expired workspace fails. | API contract and failure mapping. |
| Record progress / ready | Assigned partner. | RT-05, structured validation, same unexpired workspace, event and command limits. | A different partner, customer, invalid structured value, stale state, expired workspace, or exceeded limit fails. | Field schema, unexpected-input handling, and failure mapping. |
| Release assignment | Assigned partner. | RT-12 or RT-13 and same unexpired workspace. | A different partner, customer, stale state, or expired workspace fails. | Reason fields and failure mapping. |
| Abandon assignment | Assigned partner. | RT-14 and same unexpired workspace. | A different partner, customer, stale state, missing reason, or expired workspace fails. | Reason enum and failure mapping. |
| Cancel request | Owning customer. | RT-07 through RT-11 and same unexpired workspace. | Customer A cannot cancel Customer B's request; invalid state/deadline/data fails. | Reason/acknowledgement fields and failure mapping. |
| Confirm handoff | Owning customer. | RT-06 and same unexpired workspace. | Another actor, stale state, passed deadline, or expired workspace fails. | API contract and failure mapping. |
| Expire request | System. | RT-15 through RT-19, using injected `Clock`. | Do not transition early or from a stale/terminal state. | Scheduling cadence and late-command interaction. |
| Reset demo | Customer participant of that workspace only. | Workspace is unexpired; operation is atomic; creation/reset throttle applies. | Another role, another workspace, or expired workspace fails. | Exact data retained/deleted and session/invitation result. |
| Redeem partner invitation | Holder of a valid one-time invitation. | Token is unexpired, unused, and belongs to an unexpired workspace. | Reuse, expiry, invalid token, cross-workspace use, or throttled attempts fail. | Endpoint, request field, encoding, hash algorithm, and failure mapping. |
| Subscribe to events | Authenticated participant. | Same unexpired workspace; authorized projection; participant/workspace/global stream limits. | Cross-workspace, expired workspace, or exceeded limit fails. Replacement closes the participant's old stream. | Endpoint, event projection, and cap measured by hosting spike. |

Roles establish broad capability. Ownership, assignment, workspace, lifecycle,
and projection checks decide access to a particular object and field. Backend
checks are authoritative; route guards and hidden controls are navigation aids.

## Session, Workspace, and Invitation Guards

| ID | Defined guard | Still TBD |
| --- | --- | --- |
| AW-01 | `DemoParticipant` contains `participantId`, `workspaceId`, and role. Every authenticated session binds to exactly one participant. | Identifier types, exact role enum names, and participant activation lifecycle. |
| AW-02 | Ownership comes from the session participant, never an owner identifier supplied by Angular. | Principal type and session serialization details. |
| AW-03 | `DemoWorkspace` is the tenant boundary for all mutable demo data. Every request, assignment, audit event, reset command, and event subscription carries its `workspaceId` scope. | Persistence schema and foreign-key names. |
| AW-04 | `Start demo` creates a workspace and customer participant, then establishes the customer session. | Transaction boundary, rollback and partial-failure behavior, API contract, and failure mapping. |
| AW-05 | A workspace has immutable `expiresAt` set 60 minutes after creation. At `Clock.instant() >= expiresAt`, reads, commands, invitation redemption, reset, and SSE subscription fail at request time. | Exact expired-session problem response. |
| AW-06 | Scheduled cleanup only reclaims storage. It runs as a bounded batch after startup and on schedule; correctness never depends on it. | Cadence, batch size, retention, deletion order, and cleanup indexes. |
| AW-07 | `Start demo` returns a one-time partner invitation URL for a private window or separate browser profile. | Response field and API shape. |
| AW-08 | The invitation token has at least 128 bits of cryptographic randomness, expires after ten minutes, is stored only as a hash, and is unusable after redemption. | Encoding, hash algorithm, record fields, and attempt-throttle values. |
| AW-09 | The token is placed in the URL fragment. Angular submits it in a request body excluded from application logs. Successful redemption creates the partner participant and session in the originating workspace. | Client route, endpoint, request field, and success response. |
| AW-10 | Session cookie is `Secure`, `HttpOnly`, and `SameSite=Lax`. The readable `XSRF-TOKEN` cookie is `Secure` and `SameSite=Lax`; SPA CSRF handling protects relative mutations. | Session lifetime and termination endpoint. |
| AW-11 | Session creation and termination rotate or reload a CSRF token usable by the next mutation. A mutation without a valid token fails. | Bootstrap/refresh API shape and exact failure response. |
| AW-12 | The core release has customer and partner sessions, no registration/password/external identity flow, and no administrator interface. | Session-start and rejoin details. |

## Public-Demo Limits and Privacy

| Control | Defined limit or behavior | Still TBD |
| --- | --- | --- |
| Requests | Five requests per workspace. | Whether reset or terminal rows count toward the limit. |
| Progress | Fifty progress events per request. | Whether rejected events count and how reset affects the count. |
| Commands | Thirty state-changing commands per participant per minute. | Rate-limit algorithm and `Retry-After` behavior. |
| Participant SSE | One stream per participant; a replacement closes the earlier stream. | Replacement protocol and failure body. |
| Workspace SSE | Two streams per workspace. | Failure body. |
| Global SSE | A global cap is required. | Numeric cap from the M1 hosting measurement. |
| Request size | A limit is required. | Size and route-specific treatment. |
| Active workspaces | A global quota is required. | Numeric quota and reclamation behavior. |
| Public entry points | Minimal throttling is required for workspace creation, invitation redemption, and reset. | Numbers, windows, and algorithm. |
| Limit response | A reached public-demo limit returns HTTP 429 with a clear retry message. | Stable problem code, response mapping, and `Retry-After` behavior. |
| Analytics | No analytics. | None. |
| Network metadata | Domain data is synthetic, but the hosting platform may still process real network metadata. | Exact provider processing and retention, to be documented after the hosting spike. |
| Application logs | Exclude request bodies, cookies, tokens, credentials, session identifiers, and demo codes. | Hosting log retention, to be measured and documented after the spike. |
| Secrets | Keep deployment secrets in the hosting secret store; commit only a redacted `.env.example`. | Provider-specific configuration. |

## Required Contract Tests

These are future implementation obligations, not claims that DOMAIN-01 runs
product tests.

| Area | Required proof |
| --- | --- |
| Domain lifecycle | Every allowed and rejected transition; fixed-fee retrieval; terminal request immutability; expiry through injected `Clock`. |
| Assignment terminalization | Completion, cancellation, expiry, release, and abandonment end the active assignment correctly; a failed transaction updates neither side; no terminal request retains an active assignment. |
| Reclaiming | A partner can claim again after every assignment terminal outcome; a released request can be reclaimed. |
| PostgreSQL limits | Partial unique constraints enforce one active assignment per request and per partner using `endedAt IS NULL`. |
| Claim races | Two partners/one request produces one winner and one HTTP 409 loser; one partner/two requests produces at most one winner. Run both repeatedly against PostgreSQL. |
| Projection | Open-job responses omit customer identifiers, authentication data, and private request fields; customer and assigned-partner views expose only their allowlists. |
| Object authorization | Customer A cannot read or cancel Customer B's request. Partner A cannot update Partner B's assignment. A customer cannot invoke partner commands. Use real Spring Security and PostgreSQL. |
| Workspace isolation | A participant cannot read, claim, update, reset, or subscribe outside its workspace. Repository integration tests cover mandatory `workspaceId` filters. |
| Workspace expiry | Reads, commands, invitation redemption, reset, and SSE subscription fail at request time after expiry even when cleanup is delayed. |
| Session and CSRF | Session creation and termination provide a usable next CSRF token; a mutation without a valid token fails. |
| Invitation | A token cannot be reused, redeemed after expiry, or guessed through repeated attempts; attempt throttling reaches HTTP 429 at its documented limit. |
| Public-demo limits | Request, progress-event, command-rate, workspace-SSE, and participant-SSE limits return HTTP 429 when exceeded. |
| Audit and live delivery | History is immutable and correctly projected; state and audit persist before notification; duplicate notifications do not duplicate stored events or timeline entries. |

## Explicit TBD Register

| ID | Unresolved contract | Resolution point |
| --- | --- | --- |
| TBD-01 | Venue permission evidence, venue/service fields, relationship, and activation policy. | M2 domain completion. |
| TBD-02 | Schedule input, validation, deadline derivation, and equality semantics. | M2 domain completion. |
| TBD-03 | Fee currency, storage type, scale, and wire representation. | M2 domain/API completion. |
| TBD-04 | Request, assignment, participant, workspace, and audit identifiers and field schemas. | M2 domain/data completion. |
| TBD-05 | Cancellation, no-show, abandonment, and assignment-end reason enum values. | M2 domain completion. |
| TBD-06 | Exact request-terminal to assignment-terminal mappings, except release-to-`OPEN`. | M2 lifecycle completion. |
| TBD-07 | Progress types, ranges, one-versus-both field rules, and update cadence. | M2 domain/API completion. |
| TBD-08 | Audit event names, schema, ordering, retention, and rejected-command policy. | M2 domain/API completion. |
| TBD-09 | Full customer, assigned-partner, private-field, history, and SSE payload allowlists. | M2 projection/API completion. |
| TBD-10 | Authorization concealment policy and exact 401/403/404 mappings. | M2 problem-response completion. |
| TBD-11 | Atomic reset effects, retained data, session outcome, and replacement invitation behavior. | M2 domain/API completion. |
| TBD-12 | Invitation encoding, hash algorithm, persistence fields, API shape, and throttle values. | M2 identity/API completion. |
| TBD-13 | Cleanup cadence, batch size, retention, deletion order, and indexes. | M2 data design. |
| TBD-14 | Request-size limit, active-workspace quota, public-entry throttles, and rate-limit algorithm. | M2 security completion. |
| TBD-15 | Global SSE cap. | M1 hosting measurement, then M2 contract completion. |
| TBD-16 | SSE endpoint, event schema, heartbeat, replay cursor, reconnect schedule, polling interval, and stale threshold. | M2 API draft; later live-tracking completion. |
| TBD-17 | Session lifetime, termination endpoint, and CSRF bootstrap/refresh API. | M2 identity/API completion. |
| TBD-18 | PostgreSQL schema names and atomic claim strategy. | M2 data design; preserve both database invariants. |
| TBD-19 | HTTP paths, methods, DTOs, success responses, validation errors, stable problem codes, and field-error mappings. | M2 API-command and problem-response tables. |

Disputes, dynamic pricing, administrator venue management, idempotency, and
performance work are later-version exclusions, not `TBD` core behavior.

## Contract Work Deferred to M2

| Deferred artifact | M1 facts that must be preserved |
| --- | --- |
| API-command table | Domain actors, ownership, states, guards, required inputs, workspace scope, known 409 claim conflict, and known 429 limit behavior. |
| Problem-response table | Stable error code, human-readable message, HTTP status, optional field errors, and correlation identifier. Exact codes and mappings remain `TBD`. |
| Completed field allowlists | The open-job view remains minimized; unknown fields are not exposed while allowlists are completed. |
| Completed lifecycle table | No new transition may weaken terminal immutability, expiry, assignment terminalization, atomic limits, or audit ordering. |
| OpenAPI contract | Controller DTOs become code-first source; generated client work must not redefine these domain rules. |

## Source Coverage

| Project-plan source | Represented here |
| --- | --- |
| [Product Boundary](../project-plan.md#product-boundary) and [Core Users](../project-plan.md#core-users) | Venue/service eligibility, exclusions, roles, and permitted workflow. |
| [Lifecycle and Business Rules](../project-plan.md#lifecycle-and-business-rules) | Request/assignment states, every transition, invariants, projections, progress, and simulated check-in. |
| [Backend Modules](../project-plan.md#backend-modules) and [Database Enforcement](../project-plan.md#database-enforcement) | Workspace-scoped ownership boundaries, transactional terminalization, partial unique constraints, and unresolved data design. |
| [API Contract](../project-plan.md#api-contract) | Explicit M2 deferral and known problem-response fields. |
| [Security Model](../project-plan.md#security-model) and [Isolated Demo Workspaces](../project-plan.md#isolated-demo-workspaces) | Session-derived ownership, CSRF, tenant isolation, invitations, expiry, reset, limits, and privacy. |
| [Required Security Tests](../project-plan.md#required-security-tests) | Authorization, projection, workspace, invitation, CSRF, and limit test obligations. |
| [Primary Engineering Evidence](../project-plan.md#primary-engineering-evidence) | Domain authority, claim races, object authorization, audit ordering, and live-recovery rules. |
| [Test Contract](../project-plan.md#test-contract) | Future domain, PostgreSQL, concurrency, security, and integration proof. |
| [M1](../project-plan.md#m1-deployable-foundation-and-hosting-validation) and [M2](../project-plan.md#m2-identity-and-contract-foundation) | Draft status now; lifecycle, projection, authorization, API-command, and problem-response completion in M2. |
