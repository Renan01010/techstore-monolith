# Data Model: Monolith Foundation

This feature introduces no catalog, identity, authentication, cart, order, or
customer domain records. The entities below describe only the runtime and structural
foundation.

## Business Module

Represents a bounded package area of the backend.

| Field | Description | Validation |
|---|---|---|
| `name` | One of `identity`, `catalog`, `cart`, or `shared` | Unique within the backend; matches its root package |
| `domain` | Domain responsibility boundary | Present as a package, with no premature domain behavior |
| `application` | Use cases and module-level ports | Present as a package; depends inward on domain |
| `adapter` | Inbound/outbound integration boundary | Present as a package; depends on application/domain, not vice versa |
| `public interfaces` | Explicit capabilities other modules may invoke | None required initially; expose only when a specified use case needs one |
| `dependencies` | Other module public interfaces used by this module | Acyclic; no internal HTTP or persistence bypass |

`shared` is limited to cross-cutting technical contracts and must not become a
general-purpose home for business rules or cross-module repositories.

## Database Schema Ownership

The foundation provides one PostgreSQL database and a single Flyway migration
history. The first migration creates the `identity`, `catalog`, and `cart` schemas.
It creates no business tables. `shared` owns no business schema.

| Schema | Owning module | Initial business tables |
|---|---|---|
| `identity` | identity | None |
| `catalog` | catalog | None |
| `cart` | cart | None |
| Flyway history schema | Application migration process | Flyway metadata only |

Only the owning module may add migrations or access objects in its schema. Applied
migrations are immutable; future schema changes add new versioned migrations.

## Runtime Configuration

Represents non-secret values used to connect the local frontend, backend, and
database.

| Field | Description | Validation |
|---|---|---|
| `database name` | Local PostgreSQL database name | Required and non-empty |
| `database user` | Local application/database user | Required and non-empty |
| `database password` | Local-only password value | Required for Compose; `.env.example` must contain only a placeholder |
| `database URL` | Backend JDBC connection target | Uses the PostgreSQL Compose service name and port internally |
| `frontend API base/proxy` | Development route for the health call | Resolves to the monolith; no Gateway service |

Production secrets and deployment-specific settings are outside this feature. No JWT
signing or issuer secret is needed because token issuance/authentication behavior is
not implemented here.

## Request Correlation

Represents the identifier that joins one HTTP request with its response and log
records.

| Field | Description | Validation |
|---|---|---|
| `value` | Request's `X-Correlation-Id` value | Incoming value is accepted only when non-empty, bounded, and safe for log/header use; otherwise generate a UUID |
| `scope` | One HTTP request | Stored in request logging context and removed when request processing ends |

The response echoes the selected identifier. It is diagnostic metadata, not an
authentication or authorization credential.

## Health Status

Represents backend operational state, not a product-domain entity.

| Field | Description | Validation |
|---|---|---|
| `liveness` | Whether the process is running | Does not fail solely because PostgreSQL is unavailable |
| `readiness` | Whether the backend can serve requests requiring its database | Includes the PostgreSQL datasource health |
| `details` | Component diagnostics | Not exposed to unauthenticated health callers |

## Relationships and State Transitions

- A backend application contains the four named modules.
- Each data-owning business module owns at most its named PostgreSQL schema.
- One database migration history tracks all application schema changes.
- A request receives one selected correlation ID, which is used in its response and
  request-scoped logs.
- A backend may be live while not ready if its required database connection is
  unavailable.
