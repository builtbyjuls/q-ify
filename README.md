# Q-ify

Q-ify is a portfolio simulation of a venue-authorized queue-handoff
marketplace. The repository skeleton is established; the backend and frontend
applications have not been scaffolded yet.

## Simulation Limits

All public demo users, venues, services, fees, and events will be synthetic.
Q-ify excludes government and document-processing workflows, transport and
ticket queues, limited-stock goods, real payments, identity verification, and
claims that browser location proves physical presence. The simulation applies
only to fictional private venues that explicitly permit queue handoffs.

## Start Here

- [Full project plan](docs/project-plan.md)
- [Verified toolchain](docs/architecture/toolchain.md)
- [Review checklist](docs/review-checklist.md)
- [Week 1 backlog](docs/work-items/week-01.md)

## Repository Layout

- `backend/` is reserved for the Spring Boot modular monolith.
- `frontend/` is reserved for the Angular application.
- `docs/architecture/` contains verified architecture guidance.
- `docs/decisions/` is reserved for architecture decision records.
- `infrastructure/` is reserved for deployment assets added by later work items.

## Local Bootstrap

Use the exact runtimes in the [verified toolchain](docs/architecture/toolchain.md).
With SDKMAN loaded, `sdk env` selects the repository JDK. Select the Node.js
version from `.nvmrc` with the available Node version manager.

From the repository root, prepare the ignored local environment file:

```bash
(umask 077; test -e .env || test -L .env || cp .env.example .env)
```

The command creates `.env` with owner-only permissions when it does not exist.
It leaves an existing file or symbolic link unchanged. Fill the blank values
locally and keep hosted secrets in the hosting provider's secret store.

This bootstrap step only prepares local configuration. Backend and frontend
build, test, and run commands will be added with their application scaffolds.

## Development Approach

- Deliver one reviewable outcome at a time.
- Keep changes small, test observable behavior, and record lasting decisions.
- Use short-lived task branches or worktrees when work can proceed independently.
- Require independent review for security, authorization, concurrency,
  transaction, migration, API-contract, and deployment changes.
