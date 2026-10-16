# Feature Specification: Monolith Foundation

**Feature Branch**: `001-monolith-foundation`

**Created**: 2026-10-16

**Status**: Draft

**Input**: User description: "Prepare an independently executable TechStore modular monolith foundation with a web frontend, one backend application, PostgreSQL persistence, local startup, health checks, and testable module boundaries."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Start the local application (Priority: P1)

As a developer, I can follow the documented local setup from a clean checkout and start the TechStore frontend, monolith backend, and database together, without requiring services from the legacy microservices project.

**Why this priority**: A reproducible local environment is the foundation for developing and verifying all later features.

**Independent Test**: Follow the local setup instructions from a clean checkout; confirm all three components start, database migrations complete, and the backend health check succeeds.

**Acceptance Scenarios**:

1. **Given** a clean checkout and the documented local prerequisites, **When** a developer follows the setup instructions, **Then** the separately responsible frontend and one backend application become available with PostgreSQL, without separately starting legacy services. (FR-001, FR-002, FR-003, FR-015, FR-016)
2. **Given** the database is available, **When** the backend starts, **Then** it connects to PostgreSQL and applies pending versioned migrations without altering applied migrations. (FR-004)
3. **Given** the application has started, **When** a developer checks backend health, **Then** the health check reports success only when the application and required database connection are healthy. (FR-011)
4. **Given** a clean checkout, **When** a developer follows the setup and test instructions, **Then** required variables are documented, `.env.example` contains no secrets, and the documented automated tests pass. (FR-013, FR-014)

---

### User Story 2 - Use the frontend with the monolith (Priority: P2)

As a developer or visitor, I can open the frontend and verify that it can reach the monolith's public health endpoint, without routing through a separate gateway.

**Why this priority**: The frontend and backend must be demonstrably connected while remaining separately responsible parts of one repository.

**Independent Test**: Start the documented local environment, open the frontend, and verify its health/API connectivity indicator or documented health interaction reaches the monolith successfully.

**Acceptance Scenarios**:

1. **Given** the frontend and backend are running, **When** the frontend checks API availability, **Then** the request reaches the monolith directly, without a gateway, and receives a successful health response. (FR-012)
2. **Given** the backend returns an error, **When** a frontend request fails or submits invalid input, **Then** the response follows the DTO-based common error contract, does not expose internal details, and includes a correlation identifier usable to find the corresponding server log entry. (FR-009, FR-010)
3. **Given** a frontend element or flow from the legacy project is reused, **When** it is integrated with the new backend, **Then** pertinent visual identity and behavior are preserved, API access uses the monolith, and the legacy repository remains unchanged. (FR-017)

---

### User Story 3 - Extend business modules safely (Priority: P3)

As a developer, I can locate the initial identity, catalog, cart, and shared module boundaries and understand how one module may invoke another module's use cases, without requiring internal HTTP calls or implementing unapproved business functionality.

**Why this priority**: Explicit boundaries let future feature work proceed without recreating the legacy services as separate deployables.

**Independent Test**: Review the module structure and its documentation; verify each business module has domain, application, and adapter boundaries, and that the documented internal call mechanism uses Java interfaces/use cases rather than HTTP.

**Acceptance Scenarios**:

1. **Given** the initial application structure, **When** a developer inspects identity, catalog, cart, and shared, **Then** the modules are present with domain, application, and adapter boundaries and contain no speculative business behavior. (FR-005, FR-006)
2. **Given** one module needs another module's capability, **When** it invokes that capability, **Then** it does so through the documented Java interface/use-case boundary, not an internal HTTP request. (FR-007)
3. **Given** a proposed module dependency, **When** the dependency is reviewed, **Then** it does not create a circular dependency or bypass another module's persistence boundary. (FR-008)

### Edge Cases

- The database is unavailable during backend startup: startup must fail visibly rather than report a healthy application.
- A migration fails: startup must surface the migration failure and must not report successful readiness.
- The frontend cannot reach the backend: the connectivity failure must be visible and must not be presented as a successful health check.
- An incoming request has no correlation ID or supplies an unusable one: the request still receives a usable correlation ID, and that same ID is available in its response and server logs.
- A request contains invalid input: validation errors use the common error response format and do not expose internal implementation details.
- A future module dependency would be circular or require direct access to another module's tables: the dependency is rejected by the documented boundary rules.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The project MUST provide one independently executable backend application and one Maven build for the backend; business modules MUST NOT be separate Maven projects or independently deployed services.
- **FR-002**: The project MUST provide a frontend and backend in the same repository, with their responsibilities kept separate.
- **FR-003**: The local environment MUST provide the frontend, backend, and PostgreSQL database without requiring any service from the legacy microservices project.
- **FR-004**: The backend MUST connect to PostgreSQL and apply versioned database migrations at startup. Applied migrations MUST NOT be changed.
- **FR-005**: The initial structure MUST include identity, catalog, cart, and shared modules, each with domain, application, and adapter boundaries.
- **FR-006**: The initial modules MUST contain only the minimum foundation needed to demonstrate module boundaries; they MUST NOT implement catalog, authentication, or cart behavior ahead of approved specifications.
- **FR-007**: A module MUST invoke another module's application capability through an explicit Java interface/use-case boundary; modules MUST NOT call one another over HTTP.
- **FR-008**: Module dependencies MUST NOT be circular, and a module MUST NOT access another module's tables outside the defined persistence rules.
- **FR-009**: The backend MUST provide global error handling and input validation. HTTP error responses MUST use DTO-based contracts and MUST NOT expose internal implementation details.
- **FR-010**: Each request MUST have a correlation ID available in server logs and the corresponding response, including when the incoming request does not provide a usable ID.
- **FR-011**: The backend MUST provide a health check that reports success only when the application and its required database connection are healthy.
- **FR-012**: The frontend MUST be able to reach the monolith API directly; an API Gateway MUST NOT be introduced.
- **FR-013**: The project MUST include automated tests for the foundation, including backend behavior, database/migration integration, and frontend-to-backend connectivity.
- **FR-014**: The project MUST document reproducible local execution, required environment variables, and test commands. An `.env.example` MUST be supplied without secrets.
- **FR-015**: The required technologies are Java 21, Spring Boot, Maven, React, TypeScript, Vite, PostgreSQL, Flyway, and Docker Compose for local execution.
- **FR-016**: The project MUST NOT introduce checkout, payments, orders, tracking, Kafka, Redis, an API Gateway, or independently deployed business services as part of this foundation.
- **FR-017**: Where frontend elements or flows are reused from the legacy project, the project MUST preserve their pertinent visual identity and behavior while adapting API access to the monolith; the legacy repository MUST remain unchanged.

### Key Entities

- **Business module**: An initial bounded area named identity, catalog, cart, or shared, with explicit domain, application, and adapter responsibilities.
- **Runtime configuration**: Non-secret settings required to connect the application components in local execution.
- **Migration**: A versioned database change applied during startup and retained unchanged once applied.
- **Request correlation ID**: An identifier linking a request's HTTP response to its server-side log entries.
- **Health status**: The observable readiness state of the backend and its required database connection.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: From a clean checkout with documented prerequisites, a developer can start the frontend, backend, and database using the documented local procedure without starting any legacy project service.
- **SC-002**: On a successful startup, all pending migrations are applied and the backend health check reports success; with the database unavailable, the backend does not report healthy.
- **SC-003**: The frontend can make a successful health request directly to the monolith, with no gateway or legacy service in the request path.
- **SC-004**: All four initial modules have the documented three boundaries, and a test or reviewable architecture rule verifies that their internal calls do not use HTTP and their dependencies are acyclic.
- **SC-005**: The documented automated test commands complete successfully for the foundation, including a check of the frontend-to-backend connection.
- **SC-006**: A new developer can identify every required local environment variable and run the application and its tests using repository documentation and `.env.example`, without access to unpublished secrets.

## Assumptions

- This feature establishes runtime and structural foundations only; it does not add product catalog, identity/authentication, or cart use cases.
- The legacy repository is a source of pertinent product requirements, visual references, and existing contracts only. Its gateway and independently deployed service topology are not requirements for the new project.
- The original repository README documents a React/TypeScript/Vite frontend and API Gateway-based API access. If frontend assets or flows are reused, only pertinent presentation and user behavior carry forward; API calls must target the new monolith.
- Local execution uses Docker Compose to orchestrate the frontend, backend, and one PostgreSQL database. No additional infrastructure is required.
- Developers provide local environment values from `.env.example`; the example contains placeholders and no usable credentials or signing secrets.
- Email delivery, external authentication providers, production deployment, and production observability are not required to prove this foundation.
- Health requests are operational checks, not new catalog, identity, or cart product features.
