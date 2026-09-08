# Q-ify Review Checklist

Use only the sections affected by the change. Report findings with severity,
exact file references, evidence, impact, and the smallest safe correction.

## Severity and Disposition

- Blocker: the task cannot be completed safely, or the change risks severe
  security, data, or repository damage. It must be resolved.
- High: a likely acceptance-criteria failure, core-invariant violation,
  security defect, data-integrity defect, or material regression. It must be
  resolved.
- Medium: a credible localized defect or verification gap. Fix it in the task,
  or obtain explicit user acceptance and record a follow-up.
- Low: a non-blocking improvement. Record it without delaying completion.

## Scope and Design

- Does every changed line trace to the task and project plan?
- Is the solution the smallest design that satisfies the acceptance criteria?
- Are new dependencies, abstractions, or product behavior justified?
- Does a lasting architecture choice need an ADR?

## Domain and Data Integrity

- Are lifecycle transitions enforced in the domain layer?
- Are terminal request and assignment states immutable?
- Are request, assignment, and audit changes atomic where required?
- Can a failed transaction leave only one side updated?
- Do PostgreSQL constraints remain the final authority for race-sensitive rules?
- Are timestamps based on injected `Clock` and stored as `Instant`?

## Authorization and Privacy

- Is every mutable operation scoped by the authenticated participant and
  `workspaceId`?
- Are role, ownership, participant, projection, and workspace checks enforced
  by the backend?
- Can an open-job response leak a customer identifier or private field?
- Do expired workspaces fail at request time even without cleanup?
- Are cookies, CSRF, invitation tokens, throttles, and SSE limits handled as the
  project plan requires?
- Are secrets, tokens, request bodies, cookies, and session identifiers absent
  from logs?

## Concurrency and Reliability

- Can two partners claim one request, or one partner claim two requests?
- Are concurrency claims proved with real PostgreSQL tests?
- Are retries and duplicate notifications idempotent where required?
- Does SSE failure trigger canonical refetch and bounded recovery?
- Can cancellation, release, abandonment, or expiry leave a stuck assignment?

## API and Frontend

- Is the OpenAPI contract accurate and the generated client untouched by hand?
- Are error codes stable and problem responses complete?
- Does the backend remain the authority when route guards or controls disagree?
- Are loading, empty, validation, error, forbidden, conflict, reconnecting,
  stale, and terminal states covered where relevant?
- Are keyboard focus, semantic labels, responsive layout, and recovery actions
  preserved?

## Verification and Operations

- Does each test protect a business rule, integration boundary, or visible
  recovery path?
- Were narrow tests and all affected checks run with exact results recorded?
- Do deep links work without rewriting API, actuator, or missing asset paths?
- Do health, readiness, structured logs, and correlation identifiers still work?
- Are deployment claims supported by measured evidence and dated provider docs?
- Is every unverified claim identified explicitly?

## Review Output

Order the response as follows:

1. Findings, highest severity first.
2. Questions or assumptions that block a judgment.
3. Verification gaps.
4. Short verdict: approve, approve with non-blocking notes, or changes required.

Do not add a summary before findings. Do not report style-only preferences as
defects.

Use `changes required` for any unresolved Blocker or High finding, or for a
Medium finding that has neither been fixed nor explicitly accepted and tracked.
