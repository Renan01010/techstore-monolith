# Implementation Plan: Monolith Foundation

**Branch**: `001-monolith-foundation` | **Date**: 2026-10-16 | **Spec**: [spec.md](./spec.md)

**Input**: Feature specification from `/specs/001-monolith-foundation/spec.md`

## Summary

Establish a runnable TechStore repository with a single Spring Boot backend and
single Maven project, a separate React/Vite frontend, and one PostgreSQL database.
Use package-by-module with hexagonal layers and enforce module boundaries through
architecture tests. Use Flyway for the database baseline, a shared HTTP error
contract and correlation IDs, and backend readiness for the frontend smoke check.
Docker Compose starts all three local components without legacy services.

## Technical Context

**Language/Version**: Java 21; TypeScript; Node.js 24 LTS for the frontend container;
Maven 3.9+.

**Primary Dependencies**: Spring Boot 4.1.x, Spring Security with JWT resource-server
support available for future protected routes, Spring Boot validation, Spring
Modulith verification for module boundaries, Spring Boot Actuator, Flyway, React,
Vite, TypeScript, and PostgreSQL. Pin versions through the Spring Boot and Spring
Modulith BOMs and the frontend lockfile. Do not implement token issuance or
authentication use cases in this foundation.

**Storage**: One PostgreSQL database; one schema for each data-owning business module
(identity, catalog, cart); one Flyway migration history for the application. The
foundation creates the module schemas only; it adds no business tables.

**Testing**: JUnit 5 and Spring Boot test support; PostgreSQL integration tests using
Testcontainers; architecture verification with Spring Modulith; Vitest and
Testing Library for frontend checks; an HTTP smoke test through the Vite development
server to verify its connection to backend readiness.

**Target Platform**: Docker Compose local development on Windows, macOS, and Linux;
browser-accessible frontend at `localhost:5173`, backend at `localhost:8080`, and
PostgreSQL on the Compose network.

**Project Type**: Web application with one frontend app and one backend application.

**Performance Goals**: No throughput or latency target is defined for this foundation.
Local startup and readiness are functional checks, not performance SLAs.

**Constraints**: One Spring Boot process and one backend Maven project; no Maven
subprojects per business module; no gateway, internal HTTP calls, Redis, Kafka,
checkout, payments, orders, tracking, or speculative identity/catalog/cart use cases.
Only the frontend development server may proxy the health request for local browser
access; this is not a separately deployed gateway or a business API layer.

**Scale/Scope**: Four initial packages (identity, catalog, cart, shared), their
domain/application/adapter boundaries, one health endpoint, one migration baseline,
and foundation-level tests and local execution documentation.

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Constitution rule | Plan result |
|---|---|
| One Spring Boot modular monolith, independent of the microservices project | PASS — one backend application; legacy is read-only product/UI reference |
| Explicit hexagonal module boundaries; in-process module calls only | PASS — package-by-module, layers, Java interfaces/use cases; no internal HTTP |
| PostgreSQL, Flyway, data ownership, immutable applied migrations | PASS — one DB and migration history; module schemas; baseline only |
| Approved product scope only; no checkout/payments/orders/tracking | PASS — health and structure only; no product/auth/cart behavior |
| Required stack and security constraints | PASS — requested stack; Spring Security support is included without implementing auth use cases or JWT issuance |
| Specification before implementation; tests and updated run instructions | PASS — artifacts planned; unit, architecture, database, frontend, and smoke checks |
| No gateway, separate Maven module projects, Redis, Kafka, or unneeded infrastructure | PASS — no such services or infrastructure; Vite proxy is local development-only |

No constitution violations or exceptions are requested.

## Phase 0: Research

Research decisions and references are recorded in [research.md](./research.md). The
required technologies and scope were specified; research resolved implementation
patterns without adding product behavior:

- Use a single Maven project and package-by-module; verify module dependencies in tests.
- Keep the domain/application packages framework-independent; add adapter packages
  only for inbound HTTP/health and outbound persistence needs.
- Use direct Java interfaces/use cases for synchronous module communication. Do not
  introduce events when no business behavior needs them.
- Isolate future module-owned data by PostgreSQL schema and keep migrations in one
  Flyway history.
- Correlate HTTP requests with `X-Correlation-Id`; return RFC 9457-style errors.
- Use Actuator readiness with database health for readiness, and keep liveness separate.
- Run a root Compose stack for frontend, backend, and PostgreSQL; use Vite proxy only
  to make the backend health endpoint reachable from the local browser.

## Phase 1: Design & Contracts

- [data-model.md](./data-model.md) defines only the structural/runtime entities needed
  for the foundation; it does not define business data for identity, catalog, or cart.
- [contracts/health-and-errors.md](./contracts/health-and-errors.md) defines readiness,
  correlation-ID, and error-response contracts. No business endpoints are introduced.
- [contracts/module-boundaries.md](./contracts/module-boundaries.md) defines the
  package visibility and in-process call rules.
- [quickstart.md](./quickstart.md) describes reproducible local startup and validation.

## Project Structure

### Documentation (this feature)

```text
specs/001-monolith-foundation/
├── plan.md
├── research.md
├── data-model.md
├── quickstart.md
├── contracts/
│   ├── health-and-errors.md
│   └── module-boundaries.md
└── tasks.md                  # Generated by /speckit-tasks
```

### Source Code (repository root)

```text
backend/
├── pom.xml                   # The one backend Maven project
├── Dockerfile
└── src/
    ├── main/
    │   ├── java/com/techstore/
    │   │   ├── TechStoreApplication.java
    │   │   ├── shared/
    │   │   │   ├── application/
    │   │   │   ├── domain/
    │   │   │   └── adapter/in/web/
    │   │   ├── identity/
    │   │   │   ├── application/
    │   │   │   ├── domain/
    │   │   │   └── adapter/
    │   │   │       ├── in/web/
    │   │   │       └── out/persistence/
    │   │   ├── catalog/       # Same domain/application/adapter layers
    │   │   └── cart/          # Same domain/application/adapter layers
    │   └── resources/
    │       ├── application.yml
    │       └── db/migration/V1__create_module_schemas.sql
    └── test/
        └── java/com/techstore/
            ├── architecture/
            ├── shared/
            └── foundation/

frontend/
├── Dockerfile
├── package.json
├── package-lock.json
├── vite.config.ts
├── scripts/smoke-readiness.mjs
└── src/
    ├── api/health.ts
    ├── components/ApiStatus.tsx
    ├── App.tsx
    └── __tests__/

docker-compose.yml
scripts/verify-local-stack.mjs
.env.example
README.md
```

**Structure Decision**: Keep frontend and backend as sibling applications in one
repository. `backend/pom.xml` is the sole Maven project; modules are Java packages in
that application, not Maven artifacts. `frontend/` is the independent React/Vite
application. A root Compose file coordinates only those applications and PostgreSQL.
The frontend is a minimal branded foundation page with an API-readiness indicator;
it does not copy catalog, login, product-detail, or cart workflows into this feature.
If visual assets/styles are reused, preserve the TechStore identity and leave the
legacy repository unchanged.
Module internals remain package-private where Java permits; each module root is its
public API, and named interfaces are exposed only when a use case requires a
cross-module call. `shared` contains cross-cutting foundation code, not business
entities or repositories.

## Constitution Check — Post-Design

| Design decision | Re-check |
|---|---|
| Single backend Maven project and single Spring Boot runtime | PASS — structure contains only `backend/pom.xml`; modules are packages |
| Hexagonal boundaries and internal Java calls | PASS — contracts permit only explicit Java interfaces/use cases and acyclic dependencies |
| Data ownership and immutable migrations | PASS — one database/history with module-owned schemas and no business tables in the baseline |
| No unapproved functionality | PASS — the frontend health indicator and Actuator endpoints are foundation operations; no identity/catalog/cart behavior is added |
| Required local operation and tests | PASS — Compose includes only frontend/backend/PostgreSQL; Maven, Vitest, architecture, database, and HTTP smoke tests are planned |
| No forbidden infrastructure or topology | PASS — no API Gateway, independent service, Redis, Kafka, or separate Maven project |

No post-design constitution violations or unresolved design clarifications remain.

## Complexity Tracking

No constitution violations require justification.
