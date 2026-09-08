# Toolchain Baseline

- Verified: 2026-09-08
- Scope: ENV-01
- Status: accepted for scaffolding

## Decision

Use Java 21 and Spring Boot 4.1 for the backend. Use Angular 21 LTS with
TypeScript 5.9 and Node 24 LTS for the frontend. Pin direct tools and
dependencies exactly, while allowing the Spring Boot dependency-management
baseline to own JUnit and Testcontainers versions.

Node 24 replaces the plan's original Node 22 choice. Both are compatible with
Angular 21, but Node 24 is Active LTS, remains supported longer, and matches the
installed runtime. Node 22 is already in Maintenance LTS.

## Version Matrix

| Tool or dependency | Exact version | Pin location |
| --- | --- | --- |
| Eclipse Temurin JDK | 21.0.12.1+1 (`21.0.12+1.1-tem`) | `.sdkmanrc` |
| Apache Maven distribution | 3.9.16 | Maven Wrapper in `backend/` |
| Spring Boot | 4.1.1 | `backend/pom.xml` parent |
| Springdoc OpenAPI | 3.1.1 | `backend/pom.xml` property |
| JUnit Jupiter | 6.0.3 | Managed by Spring Boot 4.1.1 |
| JUnit Platform | 6.0.3 | Managed by Spring Boot 4.1.1 |
| Testcontainers | 2.0.5 | Managed by Spring Boot 4.1.1 |
| Node.js | 24.20.0 | `.nvmrc` |
| npm | 11.19.0 | Future `frontend/package.json` `packageManager` |
| Angular framework | 21.2.22 | Future `frontend/package.json` |
| Angular CLI and build | 21.2.23 | Future `frontend/package.json` |
| Angular Material and CDK | 21.2.14 | Future `frontend/package.json` |
| TypeScript | 5.9.3 | Future `frontend/package.json` |

Angular framework, CLI, and Material patch numbers intentionally differ. Their
current Angular 21 LTS release tags do not resolve to one shared patch. Material
and CDK remain exactly aligned with each other.

## Compatibility Evidence

- Spring Boot 4.1.1 requires Java 17 or newer, supports Java through 26, and
  requires Maven 3.6.3 or newer. Java 21 and Maven 3.9.16 satisfy that range.
- Spring Boot 4.1.1 manages JUnit 6.0.3 and Testcontainers 2.0.5. Do not override
  those versions in child dependencies.
- Springdoc 3.x supports Spring Boot 4. Springdoc 3.1.0 moved its own baseline
  to Spring Boot 4.1.0, and 3.1.1 is the current stable fix release.
- Angular 21.2 supports Node `^20.19.0 || ^22.12.0 || ^24.0.0` and TypeScript
  `>=5.9.0 <6.0.0`. Node 24.20.0 and TypeScript 5.9.3 satisfy those ranges.
- Angular 21 is in LTS on the verification date. Node 24 is Active LTS; Node 22
  is Maintenance LTS and reaches end of life earlier.
- Angular 21.2.22 includes the fix for the August 2026 Angular security advisory.

## Local Verification

| Command | Observed result | Resolution |
| --- | --- | --- |
| `java -version` | Eclipse Temurin 21.0.12.1+1 | Matches `.sdkmanrc` |
| `javac -version` | 21.0.12.1 | Matches `.sdkmanrc` |
| `mvn -version` | Apache Maven 3.8.7 on Java 21.0.12.1 | Compatible; the wrapper will pin 3.9.16 |
| `node --version` | 24.20.0 | Matches `.nvmrc` |
| `npm --version` | 11.19.0 | Record in `packageManager` when the frontend exists |
| `npx --yes @angular/cli@21.2.23 version` | Angular CLI 21.2.23 on Node 24.20.0 | Exact CLI pin executes successfully |
| `docker --version` | 29.1.3 | Available for later M1 tasks |
| `docker compose version` | 2.37.1 | Use `docker compose`, not legacy `docker-compose` |

The SDKMAN default and the Java candidate in `.sdkmanrc` both resolve to
`21.0.12+1.1-tem`. Existing SDKMAN Java installations remain available. `nvm`
is not installed, but the active Node runtime already matches `.nvmrc`. The
installed global Angular CLI remains at 20.3.5 and is not used. The Maven
Wrapper and project-local Angular CLI do not exist yet; their exact versions
are fixed above for the tasks that create `backend/` and `frontend/`.

## Scaffolding Contract

- Generate the Maven Wrapper with Maven distribution 3.9.16.
- Set the Spring Boot parent to 4.1.1 and Springdoc to 3.1.1.
- Omit versions from JUnit and Testcontainers dependencies so Spring Boot owns
  them.
- Use Testcontainers 2 artifact names:
  `org.testcontainers:testcontainers-postgresql` and
  `org.testcontainers:testcontainers-junit-jupiter`.
- Invoke Angular CLI 21.2.23 explicitly. Do not use the installed global CLI.
- Pin package versions exactly and commit the generated npm lockfile.
- Keep Angular Material and CDK on exactly 21.2.14.
- Standardize container commands on Docker Compose v2 syntax.

## Primary Sources

Accessed 2026-09-08:

- [Spring Boot 4.1.1 release](https://github.com/spring-projects/spring-boot/releases/tag/v4.1.1)
- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Boot managed dependencies](https://docs.spring.io/spring-boot/appendix/dependency-versions/coordinates.html)
- [Springdoc compatibility](https://springdoc.org/#what-is-the-compatibility-matrix-of-springdoc-openapi-with-spring-boot)
- [Springdoc 3.1.1 release](https://github.com/springdoc/springdoc-openapi/releases/tag/v3.1.1)
- [JUnit 6.0.3 overview](https://docs.junit.org/6.0.3/overview.html)
- [Testcontainers 2.0.5 release](https://github.com/testcontainers/testcontainers-java/releases/tag/2.0.5)
- [Angular version compatibility](https://angular.dev/reference/versions)
- [Angular release support](https://angular.dev/reference/releases)
- [Angular 21 security advisory](https://github.com/angular/angular/security/advisories/GHSA-f6mr-pjwc-34m4)
- [Angular framework releases](https://www.npmjs.com/package/@angular/core?activeTab=versions)
- [Angular CLI releases](https://www.npmjs.com/package/@angular/cli?activeTab=versions)
- [Angular Material releases](https://www.npmjs.com/package/@angular/material?activeTab=versions)
- [TypeScript 5.9.3 release](https://github.com/microsoft/TypeScript/releases/tag/v5.9.3)
- [Node.js 24.20.0 release](https://nodejs.org/en/blog/release/v24.20.0)
- [Node.js release schedule](https://nodejs.org/en/about/previous-releases)
- [Apache Maven downloads](https://maven.apache.org/download.cgi)
- [Eclipse Temurin 21.0.12.1 release](https://github.com/adoptium/temurin21-binaries/releases/tag/jdk-21.0.12.1%2B1)
