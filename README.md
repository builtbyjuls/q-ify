# Q-ify

Q-ify is a learning and portfolio project for an on-demand marketplace where
customers delegate permitted private-sector queues to vetted runners.

The intended product flow is:

```text
Customer request -> runner assignment -> progress tracking -> handoff -> completion
```

## Safety Boundary

Q-ify supports queue delegation only when the venue or service provider allows
it. It does not support queues that require the customer's physical presence,
identity verification, biometrics, or a non-transferable appointment.

The project currently stores deterministic demo identities and catalog data. It
does not store real personal or sensitive information.

## Current State

The backend currently provides foundations rather than an HTTP API:

- A controlled queue-request lifecycle implemented as plain Java domain rules.
- Actor and runner-profile persistence with demo customer, runner, and admin
  records.
- Venue and service-offering persistence with approved, unapproved, active, and
  inactive demo offerings.
- Flyway-managed PostgreSQL migrations.
- JPA mappings with Hibernate schema validation only.
- PostgreSQL integration tests through Testcontainers.
- A Docker Compose PostgreSQL service for local application runs.

No customer, runner, or admin HTTP endpoints exist yet.

## Technology

- Java 21
- Spring Boot 4.1.1
- Maven
- PostgreSQL 18.6
- Spring Data JPA and Hibernate
- Flyway
- JUnit, Mockito, and Testcontainers
- Docker Compose

## Prerequisites

Install:

- JDK 21
- Maven
- Docker with Docker Compose

Confirm the tools are available:

```bash
java -version
mvn -version
docker compose version
```

## Run Locally

Start PostgreSQL:

```bash
docker compose up -d postgres
docker compose ps
```

Run the application:

```bash
mvn spring-boot:run
```

On a clean database, Flyway applies migrations V1 through V4. Hibernate then
checks that the JPA mappings match the migrated schema.

This is not a web application yet. A successful run initializes and validates
the application context, but there is no HTTP port or endpoint to open.

Stop PostgreSQL while preserving its named data volume:

```bash
docker compose down
```

To intentionally remove the local database data as well:

```bash
docker compose down -v
```

## Database Configuration

Local defaults are:

| Setting | Default |
| --- | --- |
| Database | `qify` |
| User | `qify` |
| Password | `qify-local` |
| Host port | `5432` |
| JDBC URL | `jdbc:postgresql://localhost:5432/qify` |

The password is a local development default, not a production credential.

Compose accepts:

- `QIFY_DB_NAME`
- `QIFY_DB_USER`
- `QIFY_DB_PASSWORD`
- `QIFY_DB_PORT`

Spring accepts:

- `QIFY_DB_URL`
- `QIFY_DB_USER`
- `QIFY_DB_PASSWORD`

If the Compose port changes, the Spring JDBC URL must use the same port. For
example:

```bash
QIFY_DB_PORT=55432 docker compose up -d postgres
QIFY_DB_URL=jdbc:postgresql://localhost:55432/qify mvn spring-boot:run
```

## Run Tests

Docker must be running because the integration tests start real PostgreSQL
containers:

```bash
mvn test
```

The tests do not use the Compose database. Testcontainers creates isolated
temporary databases and supplies their connection details to Spring.

The current suite covers:

- Every allowed and rejected queue-request status transition.
- Flyway migration and JPA mapping compatibility.
- Identity and catalog seed data.
- PostgreSQL check, uniqueness, and foreign-key constraints.

Concurrency, assignment, idempotency, and HTTP contract tests will be added with
the use cases they protect.

## Project Structure

```text
src/main/java/com/qify/
  catalog/       Venue and service-offering model
  fulfillment/   Queue-request lifecycle model
  identity/      Actor and runner model

src/main/resources/
  application.properties
  db/migration/  Versioned Flyway migrations

src/test/java/com/qify/
  catalog/       Catalog PostgreSQL tests
  fulfillment/   Plain Java lifecycle tests
  identity/      Identity PostgreSQL tests
```

The backend starts as a modular monolith with package-by-feature boundaries.
It does not use microservices, CQRS, Event Sourcing, RabbitMQ, a transactional
outbox, or SAGA orchestration.

## Version 1 Direction

Version 1 will add:

- Customer request creation and tracking.
- Manual runner assignment and acceptance.
- Controlled runner progress updates.
- An auditable request timeline.
- A customer notification when handoff is ready.
- Concurrency and idempotency protection using PostgreSQL.

Real-time maps, automated dispatch, payment processing, chat, real identity
verification, and the Angular interface remain outside the current backend
foundation.

The next implementation milestone is the customer request vertical slice.
