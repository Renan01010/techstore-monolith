---
description: "Tasks for implementing the Monolith Foundation feature"
---

# Tasks: Monolith Foundation

**Input**: Design documents from `/specs/001-monolith-foundation/`

**Prerequisites**: plan.md and spec.md; research.md, data-model.md, contracts/, and quickstart.md

**Tests**: Automated tests are required by the specification. Include backend unit/integration/architecture tests and frontend component/API smoke tests.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel because it affects different files and has no unfinished task dependency.
- **[Story]**: User story served by the task: US1, US2, or US3.
- Every task includes a concrete repository path.

## Path Conventions

- Backend: `backend/src/main/` and `backend/src/test/`
- Frontend: `frontend/src/` and `frontend/scripts/`
- Root local environment: `docker-compose.yml`, `.env.example`, and `README.md`

## Phase 1: Setup

**Purpose**: Create the single backend build, frontend app, and local runtime scaffolding.

- [X] T001 Create the sole Maven project in `backend/pom.xml` with Java 21, Spring Boot 4.1.x, Spring Security resource-server/JWT support, validation, Actuator, Flyway, PostgreSQL, Spring Modulith test support, JUnit, and Testcontainers dependencies; configure one executable Boot application and no Maven subprojects.
- [X] T002 [P] Initialize the React, TypeScript, and Vite app, scripts, and locked dependencies with a minimal non-business placeholder view in `frontend/package.json`, `frontend/package-lock.json`, `frontend/tsconfig.json`, `frontend/vite.config.ts`, `frontend/index.html`, `frontend/src/main.tsx`, and `frontend/src/App.tsx`.
- [X] T003 [P] Add the backend container build in `backend/Dockerfile` and frontend development container build in `frontend/Dockerfile`, using supported pinned Java 21/Maven and Node.js 24 LTS images.
- [X] T004 Create root `.env.example` with placeholder-only PostgreSQL/database and local frontend settings, and root `docker-compose.yml` with only PostgreSQL, backend, and frontend services on a private Compose network.
- [X] T005 [P] Add ignore rules for local `.env`, build outputs, node modules, and IDE files in `.gitignore` without removing existing project ignore rules.

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Implement shared backend runtime foundations required before story completion.

**⚠️ CRITICAL**: Complete this phase before user-story implementation.

- [X] T006 Create the single Spring Boot entry point and base configuration in `backend/src/main/java/com/techstore/TechStoreApplication.java` and `backend/src/main/resources/application.yml`, loading database connection values from environment variables.
- [X] T007 Add Flyway configuration and the initial versioned migration in `backend/src/main/resources/application.yml` and `backend/src/main/resources/db/migration/V1__create_module_schemas.sql`; create only the `identity`, `catalog`, and `cart` schemas, with no business tables.
- [X] T008 Add centralized request validation and RFC 9457 `ProblemDetail` handling in `backend/src/main/java/com/techstore/shared/adapter/in/web/GlobalExceptionHandler.java` and supporting error DTO/types under `backend/src/main/java/com/techstore/shared/`.
- [X] T009 Implement the `X-Correlation-Id` request filter in `backend/src/main/java/com/techstore/shared/adapter/in/web/CorrelationIdFilter.java`; accept only a non-empty value of at most 128 ASCII characters matching `[A-Za-z0-9._-]+`, otherwise generate a UUID, echo the selected value in the response, add it to request-scoped logging context, and always clear that context.
- [X] T010 Configure Actuator liveness/readiness and database readiness in `backend/src/main/resources/application.yml`; expose status only at readiness and configure Spring Security in `backend/src/main/java/com/techstore/shared/adapter/in/web/SecurityConfiguration.java` to permit the health endpoints without adding authentication or business routes.
- [X] T011 Configure Compose database/backend/frontend health checks, startup dependencies, ports, environment interpolation, and persistence volume in `docker-compose.yml`; do not add a gateway or another service.

## Phase 3: User Story 1 - Start the local application (Priority: P1) 🎯 MVP

**Goal**: Developers can reproducibly start the frontend, one Spring Boot backend, and PostgreSQL with Flyway migrations and a valid backend health response.

**Independent Test**: Starting from the documented prerequisites and `.env.example`, run the local Compose stack; verify the database and backend become healthy, migrations are recorded, and the backend becomes unready when PostgreSQL is unavailable.

### Tests for User Story 1

- [X] T012 [P] [US1] Add PostgreSQL Testcontainers integration tests for database connection, successful Flyway baseline application, and readiness-down behavior in `backend/src/test/java/com/techstore/foundation/DatabaseReadinessIntegrationTest.java`.
- [X] T013 [P] [US1] Add a backend startup and health endpoint integration test asserting status-only readiness and no exposed component details in `backend/src/test/java/com/techstore/foundation/HealthEndpointIntegrationTest.java`.

### Implementation for User Story 1

- [X] T014 [US1] Add a local Compose smoke-check script in `scripts/verify-local-stack.mjs` that waits for PostgreSQL and backend readiness, confirms a Flyway schema-history table exists, confirms the frontend container is running, and returns failure if any check times out.
- [X] T015 [US1] Document prerequisites, copying `.env.example`, Compose startup/teardown, exposed local ports, migration behavior, test commands, and secret handling in the root `README.md`.
- [ ] T016 [US1] Verify that `docker compose up --build` starts exactly PostgreSQL, the single backend, and the frontend and that the backend health endpoint succeeds; record exact reproducible commands and outcomes in `specs/001-monolith-foundation/quickstart.md`.

**Checkpoint**: A developer can start and health-check the foundation without starting any legacy-project service.

## Phase 4: User Story 2 - Use the frontend with the monolith (Priority: P2)

**Goal**: The frontend visibly checks backend readiness through the monolith and presents API errors safely without a Gateway.

**Independent Test**: With the Compose stack running, open the frontend and verify its API status; request readiness through Vite, then stop backend access and confirm the frontend displays an unavailable state instead of success.

### Tests for User Story 2

- [X] T017 [P] [US2] Add backend contract tests for correlation-ID acceptance/generation/echo and RFC 9457 validation and unexpected-error responses in `backend/src/test/java/com/techstore/shared/adapter/in/web/CorrelationIdFilterTest.java` and `backend/src/test/java/com/techstore/shared/adapter/in/web/GlobalExceptionHandlerTest.java`.
- [X] T018 [P] [US2] Test successful readiness, failed requests, and error-state rendering for the API client and status component in `frontend/src/__tests__/health.test.tsx`.
- [X] T019 [P] [US2] Add an HTTP smoke script that requests the Vite readiness proxy, checks HTTP success and JSON `status: UP`, and exits non-zero on failure in `frontend/scripts/smoke-readiness.mjs`.

### Implementation for User Story 2

- [X] T020 [P] [US2] Implement a typed readiness client for `GET /actuator/health/readiness` in `frontend/src/api/health.ts`; distinguish unavailable/invalid responses from healthy status.
- [X] T021 [US2] Add a visible loading, healthy, and unavailable API status to the minimal branded foundation view in `frontend/src/components/ApiStatus.tsx`, `frontend/src/App.tsx`, and `frontend/src/styles.css`; do not add catalog, login, product, or cart flows.
- [X] T022 [US2] Configure the local Vite proxy from `/actuator` to the backend service in `frontend/vite.config.ts`; proxy only operational health requests and do not create a Gateway or business API proxy.
- [X] T023 [US2] Register frontend unit and smoke test commands in `frontend/package.json`, regenerate `frontend/package-lock.json`, and document `npm run test:smoke` in `README.md`.
- [ ] T024 [US2] Run the backend error/correlation contract tests, frontend component/API tests, and smoke script against the running Compose stack; update expected results and commands in `specs/001-monolith-foundation/quickstart.md`.

**Checkpoint**: The frontend's health request reaches the single backend through the local Vite development server; no Gateway is introduced.

## Phase 5: User Story 3 - Extend business modules safely (Priority: P3)

**Goal**: Developers can locate the four empty hexagonal module boundaries and rely on automated checks for permitted in-process dependencies.

**Independent Test**: Run the architecture verification test and inspect the module packages; verify all four modules have domain/application/adapter packages, dependencies are acyclic, and no business use cases, internal HTTP clients, or cross-module persistence access exist.

### Tests for User Story 3

- [X] T025 [P] [US3] Add a Spring Modulith architecture test that discovers the `com.techstore` modules and fails on undeclared dependencies or cycles in `backend/src/test/java/com/techstore/architecture/ModuleBoundariesTest.java`.

### Implementation for User Story 3

- [X] T026 [P] [US3] Create empty `domain`, `application`, and `adapter` package markers for identity under `backend/src/main/java/com/techstore/identity/`.
- [X] T027 [P] [US3] Create empty `domain`, `application`, and `adapter` package markers for catalog under `backend/src/main/java/com/techstore/catalog/`.
- [X] T028 [P] [US3] Create empty `domain`, `application`, and `adapter` package markers for cart under `backend/src/main/java/com/techstore/cart/`.
- [X] T029 [P] [US3] Create empty `domain`, `application`, and `adapter` package markers for shared under `backend/src/main/java/com/techstore/shared/`, reserving shared for technical cross-cutting concerns only.
- [X] T030 [US3] Declare and verify Spring Modulith module metadata and allowed dependencies in `backend/src/main/java/com/techstore/identity/package-info.java`, `backend/src/main/java/com/techstore/catalog/package-info.java`, `backend/src/main/java/com/techstore/cart/package-info.java`, and `backend/src/main/java/com/techstore/shared/package-info.java`; keep dependencies acyclic and expose no business use cases.
- [X] T031 [US3] Document package visibility, Java interface/use-case calls, forbidden internal HTTP, per-module schema ownership, and the absence of business functionality in `README.md`.
- [X] T032 [US3] Run the architecture test and verify no module can access another module's implementation or persistence packages; record the check in `specs/001-monolith-foundation/quickstart.md`.

**Checkpoint**: Module boundaries are visible, enforced, and contain no functionality beyond the foundation.

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Validate the integrated result against the approved specification and local guide.

- [X] T033 [P] Confirm `.env.example` contains placeholders only and update environment-variable explanations in `README.md`.
- [ ] T034 Run backend unit, architecture, and PostgreSQL integration tests with `mvn -f backend/pom.xml verify` and resolve failures without adding out-of-scope features.
- [ ] T035 Run frontend unit tests and `npm --prefix frontend run test:smoke` with Compose running; resolve failures without adding business behavior.
- [ ] T036 Run every startup, health, migration, module-boundary, and teardown procedure in `specs/001-monolith-foundation/quickstart.md` and make the instructions reproducible.
- [X] T037 Review the final changes against `specs/001-monolith-foundation/spec.md` and `.specify/memory/constitution.md`; remove any Gateway, extra service, separate Maven module, Redis, Kafka, business table, or speculative identity/catalog/cart behavior.

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies; establishes build manifests and container/local-runtime skeleton.
- **Foundational (Phase 2)**: Depends on T001 and T003-T004; blocks user stories.
- **User Story 1 (Phase 3)**: Depends on Phase 2; is the MVP deployment slice.
- **User Story 2 (Phase 4)**: Depends on Phase 2 and the running backend/readiness from US1 (T016); validates browser-to-monolith connectivity.
- **User Story 3 (Phase 5)**: Depends on Phase 2; can proceed in parallel with US1 and US2 once the common backend exists.
- **Polish (Phase 6)**: Depends on all desired stories.

### User Story Dependencies

- **US1 (P1)**: Starts after Phase 2; no dependency on another story.
- **US2 (P2)**: Starts after Phase 2 and depends on the backend readiness endpoint and runnable environment established by US1.
- **US3 (P3)**: Starts after Phase 2; independent of US1/US2 and may proceed in parallel.

### Within Each User Story

- Backend integration tests (T012-T013) can be written in parallel before or alongside the corresponding backend runtime validation.
- US2 component tests (T017) and smoke script (T018) are parallelizable and precede validation tasks.
- US3 package marker tasks (T025-T028) are parallelizable; metadata verification depends on all four package roots.
- Complete each story's independent test before treating its checkpoint as done.

### Parallel Opportunities

- **Setup**: T001, T002, T003, and T005 touch separate project areas; T004 may run alongside them after names/ports are agreed from the plan.
- **Foundational**: T008 and T009 touch separate shared web files; T007 can be developed alongside them after application properties are defined.
- **US1**: T012 and T013 can run in parallel.
- **US2**: T017, T018, T019, and T020 start as parallel tests/client work; T021 depends on the client contract; T022 depends on the proxy path defined in the health contract.
- **US3**: T026, T027, T028, and T029 are independent; T025 and T030 verify/configure the completed package roots.
- **Cross-story**: US3 can run in parallel with US1 and US2 after Phase 2.

## Parallel Example: User Story 1

```text
T012 PostgreSQL migration/readiness integration test
T013 status-only health endpoint integration test
```

## Parallel Example: User Story 3

```text
T026 identity package boundaries
T027 catalog package boundaries
T028 cart package boundaries
T029 shared package boundaries
```

## Implementation Strategy

1. Complete Phase 1 and Phase 2 so the repository has one build, one runtime, database migrations, safe shared HTTP handling, and health probes.
2. Deliver **US1 as MVP**: start the three local components, apply the initial migration, verify backend readiness, and document reproducible commands.
3. Deliver US2 to demonstrate frontend-to-monolith communication and visible health/error states.
4. Deliver US3 to establish empty module boundaries and automated dependency checks; do not create product use cases or tables.
5. Finish with the complete test suite and quickstart validation.
