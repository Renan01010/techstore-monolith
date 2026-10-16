# Research: Monolith Foundation

**Date**: 2026-10-16

## Decisions

### Backend platform and module verification

- **Decision**: Use Java 21, Spring Boot 4.1.x, and one Maven project. Represent
  identity, catalog, cart, and shared as packages under a single base package.
  Use Spring Modulith's module verification from tests to check module dependencies;
  keep business internals out of exported package APIs.
- **Rationale**: This enforces the requested single deployable and single Maven
  project while providing automated checks against accidental package dependencies.
  The official Spring Boot system requirements currently list Java 17 through 26
  and Maven 3.6.3 or later for Boot 4.1.1, so the specified Java 21 and Maven 3.9+
  are supported.
- **Alternatives considered**: Maven subprojects per business module, rejected
  because the constitution and feature explicitly prohibit them; hand-written
  ArchUnit checks, rejected in favor of Spring Modulith's domain-specific module
  verification.

### Hexagonal layers and calls between modules

- **Decision**: Keep `domain`, `application`, and `adapter` layers within each
  business package. Keep domain/application code independent of Spring and JPA.
  Use an explicit Java interface/use case for synchronous calls across modules.
  Introduce neither application events nor event infrastructure in this foundation.
- **Rationale**: The API/use-case boundary meets the constitution, preserves direct
  in-process execution, and avoids introducing asynchronous behavior where no current
  business use case requires it. Layering keeps adapter dependencies from leaking
  inward.
- **Alternatives considered**: Internal HTTP, explicitly prohibited; events for all
  communication, rejected because the current feature has no events and events add
  indirection and persistence requirements.

### PostgreSQL and Flyway

- **Decision**: Use one PostgreSQL database and one Flyway history. Give each
  data-owning business module its own PostgreSQL schema (`identity`, `catalog`,
  `cart`); keep `shared` free of business-owned data. The initial migration creates
  only those schemas and no business tables.
- **Rationale**: Schemas make ownership visible in database object names while
  preserving one database, one deployable, and a single ordered migration history.
  The baseline proves Flyway works without inventing product data or functionality.
- **Alternatives considered**: One shared schema, simpler but with weaker ownership
  visibility; separate databases or Flyway histories, rejected as unnecessary and
  inconsistent with a single application database.

### HTTP errors and request correlation

- **Decision**: Use one global error handler with Spring's `ProblemDetail` shape
  aligned to RFC 9457. Define `X-Correlation-Id` as the request/response header.
  Accept a bounded safe incoming identifier; otherwise generate a UUID. Put the
  value in request logging context and return it with error details.
- **Rationale**: A standard problem document gives frontend and future API clients a
  stable error shape. A generated or validated identifier makes request logs
  correlatable without echoing arbitrary/unbounded header content.
- **Alternatives considered**: Per-module error handlers, rejected because they
  duplicate cross-cutting behavior; a custom unstandardized error document, rejected
  because Spring supports ProblemDetail natively.

### Health and readiness

- **Decision**: Use Spring Boot Actuator health endpoints. Keep liveness independent
  of PostgreSQL; include the datasource health indicator in readiness. Expose only
  health status, not component details, to unauthenticated local checks.
- **Rationale**: A database outage should make the application not ready to receive
  dependent requests, but should not cause an orchestrator to repeatedly restart a
  live process. Health payloads must not leak internal connection details.
- **Alternatives considered**: One undifferentiated health group, acceptable for
  simple local use but less precise; custom health aggregation, unnecessary when the
  datasource indicator is sufficient.

### Local frontend/backend connectivity

- **Decision**: Use a root Compose file for the frontend, backend, and PostgreSQL.
  In local development the Vite server may proxy the readiness path to the backend
  service on the Compose network. The frontend health client calls only that
  readiness path. No separate gateway or business API proxy is created.
- **Rationale**: Browser code cannot resolve a Compose-only backend hostname. A
  development-server proxy lets the browser make a same-origin request while the
  actual health endpoint and all future business behavior remain in the monolith.
- **Alternatives considered**: Requiring host-network configuration for each
  browser, rejected as less reproducible; adding a gateway container, prohibited.
  Spring Boot's Docker Compose development support is not a replacement for a root
  Compose stack because it does not orchestrate the frontend.

### Legacy frontend reuse

- **Decision**: If visual material is reused, carry forward the TechStore brand
  identity (such as the existing wordmark and relevant visual tokens) into the
  minimal foundation page. Do not copy the legacy catalog, product detail, login, or
  cart flows into this feature; their future reuse requires the corresponding
  approved specifications. Leave the legacy repository unchanged.
- **Rationale**: The legacy `frontend/src/App.tsx` contains those routes and connects
  them to product, auth, and cart stores. Reusing those flows would implement
  explicitly excluded product behavior. The root README documents that its original
  API topology depends on a gateway, which the new repository must not reproduce.
- **Alternatives considered**: Copy the whole frontend and replace service URLs,
  rejected because it brings unapproved functionality and store/API assumptions into
  the foundation; ignore the brand, rejected because the feature explicitly asks
  that pertinent visual identity be preserved when assets are reused.

### Test strategy

- **Decision**: Use JUnit for backend unit tests, Spring Modulith verification for
  module boundaries, Testcontainers PostgreSQL tests for Flyway/health behavior,
  Vitest and Testing Library for frontend behavior, and an HTTP smoke test through
  Vite's proxy for frontend-to-backend connectivity.
- **Rationale**: The database integration check uses PostgreSQL itself instead of an
  in-memory substitute. The smoke check crosses the actual local frontend-server to
  backend path. Docker is already a local prerequisite for the requested Compose
  environment.
- **Alternatives considered**: Testing database migrations only against H2, rejected
  because SQL compatibility with PostgreSQL would not be verified; browser
  automation, deferred because a simple HTTP smoke check validates the sole
  foundation integration without adding a browser runtime.

## Version Compatibility

The official Spring Boot system requirements page lists Spring Boot 4.1.1 as
compatible with Java 17 through 26 and Maven 3.6.3 or later. Select a Spring
Modulith release compatible with the chosen Boot BOM and verify dependency
resolution during implementation. Keep the Spring Boot patch version and all
frontend dependencies locked in their respective build manifests.

## References

- [Spring Boot system requirements](https://docs.spring.io/spring-boot/system-requirements.html)
- [Spring Modulith fundamentals and named interfaces](https://docs.spring.io/spring-modulith/reference/fundamentals.html)
- [Spring Modulith events](https://docs.spring.io/spring-modulith/reference/events.html)
- [Spring Boot tracing](https://docs.spring.io/spring-boot/reference/actuator/tracing.html)
- [Spring Boot health endpoints and probes](https://docs.spring.io/spring-boot/reference/actuator/endpoints.html)
- [Spring Boot health probes](https://docs.spring.io/spring-boot/reference/actuator/kubernetes-probes.html)
- [Spring MVC REST exception handling](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-ann-rest-exceptions.html)
- [RFC 9457: Problem Details for HTTP APIs](https://www.rfc-editor.org/rfc/rfc9457)
- [Vite server proxy](https://vite.dev/config/server-options.html#server-proxy)
- [Docker Compose startup order and health checks](https://docs.docker.com/compose/how-tos/startup-order/)
- [TechStore legacy README](https://github.com/Renan01010/e-commerce/blob/main/README.md) — read-only reference for the prior frontend stack and Gateway/service topology; the topology is explicitly not reused.
- [TechStore legacy App.tsx](https://github.com/Renan01010/e-commerce/blob/main/frontend/src/App.tsx) — read-only reference for the existing home, catalog, login, product, and cart routes; feature flows are not copied into the foundation.
