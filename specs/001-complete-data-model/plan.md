# Implementation Plan: Complete PostgreSQL Data Model

**Branch**: `feature-001-fundacion` | **Date**: 2026-10-17 | **Spec**: [spec.md](spec.md)

**Input**: Feature specification at `specs/001-complete-data-model/spec.md`

## Summary

Create the 30-table data foundation for the nine logical business modules using small, ordered Flyway migrations in one PostgreSQL database with a dedicated schema per module. Preserve the existing V1 migration and existing `identity`, `catalog`, and `cart` schemas; add the six missing module schemas. Enforce cross-row invariants (typed category attributes and publishable-product variant requirements) with PostgreSQL triggers where ordinary constraints cannot express them. Add PostgreSQL integration tests for successful and rejected relationships, constraints, migration order, and repeat initialization. No JPA entities, endpoints, or business workflows are included.

## Technical Context

**Language/Version**: Java 21

**Primary Dependencies**: Spring Boot 4.1.1; Spring JDBC; Flyway Core and PostgreSQL database support; PostgreSQL JDBC driver; JUnit via Spring Boot Test; Testcontainers PostgreSQL 2.0.5

**Storage**: PostgreSQL; nine module schemas (`identity`, `catalog`, `inventory`, `cart`, `commercial`, `engagement`, `promotions`, `marketing`, `platform`) in one database; `public` retains Flyway metadata

**Testing**: Maven Surefire/JUnit; PostgreSQL integration tests with Testcontainers

**Target Platform**: Java server application; local environment uses the existing Docker Compose setup where available

**Project Type**: Backend modular monolith

**Performance Goals**: Not specified for this schema-only feature. Add indexes for primary/unique/foreign keys and documented lookup/filter paths; avoid speculative workload tuning.

**Constraints**: Do not edit applied migrations. The existing `V1__create_module_schemas.sql` creates `identity`, `catalog`, and `cart`; preserve and use those schemas. Create schemas for the remaining six modules in a new migration. Qualify table references across schemas explicitly and configure Flyway to manage the complete schema set while keeping its history table in `public`. No JPA, APIs, external services, orders, payments, checkout, email delivery, stock operations, or discount processing.

**Scale/Scope**: 30 tables across identity, catalog, inventory, cart, commercial, engagement, promotions, marketing, and platform; schema and migration verification only.

## Constitution Check

*GATE: Must pass before design and remain compliant after design.*

- **Modular monolith**: PASS — schema work stays in the existing backend application.
- **Hexagonal boundaries**: PASS — no domain/application adapters or cross-module runtime calls are added; database constraints encode the approved relational model.
- **Versioned persistence**: PASS — add new schema/domain migrations after V1 and never modify existing migrations.
- **Approved scope**: PASS — no checkout, orders, payments, or other excluded workflow.
- **Specification-driven delivery**: PASS — implementation follows the approved spec and this reviewed plan; tasks are intentionally deferred until plan review.
- **Technology/security requirements**: PASS — PostgreSQL/Flyway, hashed passwords, digest-only session and one-time secrets, no sensitive audit fields.
- **Quality gates**: PASS — validate migrations and constraint behavior using real PostgreSQL integration tests; report infrastructure-blocked tests as not run.

## Design Decisions

1. **Existing migration and module schemas**: Do not alter V1. Retain and use `identity`, `catalog`, and `cart`; create `inventory`, `commercial`, `engagement`, `promotions`, `marketing`, and `platform` after V1. Keep Flyway history in `public` and place each domain table in its owning schema. This follows the existing architecture and the user's explicit decision, and documents the permitted exception to the original shared-schema preference.
2. **Inventory identity and history**: `inventory` has a unique `product_variant_id` (one aggregate balance per variant). Each `inventory_movements` row references the `inventory` row, not an independently supplied variant identifier. The FK uses `ON DELETE RESTRICT`, preserving the movement ledger and its inventory identity. Movement records are append-only through database protection against update/delete. No trigger applies movements to balances; that operational behavior is out of scope.
3. **Cart price snapshot**: A `NULL` unit-price snapshot means unknown. A non-null value is the known price, including zero, and must be nonnegative with a non-null currency. A row-level `CHECK` enforces these cases; no separate “price known” boolean is needed.
4. **Category attribute integrity**: Do not duplicate product/category identifiers in variant attribute rows. Use trigger validation that resolves variant → product → primary category and checks the category/attribute association and declared value type. Protect the association and product-category changes with complementary checks and row locks so concurrent edits cannot leave existing values invalid. Index the lookup columns.
5. **Publication invariant**: A product can be published only while at least one active variant exists. Deferred constraint triggers validate the final transaction state after product publication or variant activation changes; parent-row locking serializes competing product/variant state changes. This allows a transaction to create a variant and publish its product together while rejecting the invalid committed state.
6. **Attachment ownership**: Four nullable FK columns (`user_id`, `product_id`, `contact_request_id`, `marketing_email_id`) are guarded by `CHECK (num_nonnulls(...) = 1)`. Each non-null reference is independently FK-validated. All four owner FKs cascade deletion of dependent attachment metadata; external file cleanup is not implemented here.
7. **Deletion policy**: Use cascade only for disposable dependent data, set-null for optional actor/context references while preserving snapshots, and restrict removal of parents referenced by durable history. Full per-FK policy is enumerated in `data-model.md`.
8. **Finite state domains**: Use the account, role, product, variant, contact-request, review, newsletter, campaign, and recipient values explicitly approved by the user and recorded in the specification. Enforce them with named `CHECK` constraints; do not add state columns to carts, suppliers, or coupons.

## Migration Order

Each version is one bounded migration, and each FK target is created in an earlier version:

| Version | Migration responsibility | Tables introduced |
|---|---|---|
| V1 (existing) | Preserve creation of `identity`, `catalog`, and `cart`; immutable | — |
| V2 | Create remaining module schemas (`inventory`, `commercial`, `engagement`, `promotions`, `marketing`, `platform`) without changing existing schemas | — |
| V3 | Identity users and dependent identity records in `identity` | `users`, `user_addresses`, `email_verification_tokens`, `password_reset_tokens`, `user_sessions` |
| V4 | Catalog roots and definitions in `catalog` | `categories`, `brands`, `products`, `attributes` |
| V5 | Catalog assignments, variants, media, typed values, and publication/attribute invariant triggers in `catalog` | `category_attributes`, `product_variants`, `product_images`, `variant_attribute_values` |
| V6 | Suppliers, aggregate inventory, append-only movement history in `inventory` | `suppliers`, `inventory`, `inventory_movements` |
| V7 | User carts and cart items in `cart` | `carts`, `cart_items` |
| V8 | Contact requests and historical request items in `commercial` | `contact_requests`, `contact_request_items` |
| V9 | Reviews and favorites in `engagement` | `product_reviews`, `favorites` |
| V10 | Coupons and usage history in `promotions` | `coupons`, `coupon_usage` |
| V11 | Newsletter subscriptions, campaigns, and recipients in `marketing` | `newsletter_subscriptions`, `marketing_emails`, `email_recipients` |
| V12 | Audit records, settings, and attachment records referencing all supported owners in `platform` | `audit_logs`, `app_settings`, `file_attachments` |

The migration test must start from the repository's existing V1 on an empty database and verify every migration applies in order. Cross-schema FK targets are created in earlier versions. Configure `spring.flyway.schemas` with `public` plus all nine module schemas and retain `spring.flyway.default-schema: public`; V2 explicitly creates the six missing schemas before any migration references them. Migration tests verify all nine module schemas, model tables only in their owning schema, and Flyway history in `public`.

## Project Structure

### Documentation (this feature)

```text
specs/001-complete-data-model/
├── spec.md
├── plan.md
├── research.md
├── data-model.md
└── quickstart.md
```

### Source Code (repository root)

```text
backend/
├── src/main/resources/db/migration/
│   ├── V1__create_module_schemas.sql       # Existing; immutable
│   ├── V2__create_remaining_module_schemas.sql
│   └── V3__...sql through V12__...sql     # Ordered domain migrations
└── src/test/java/com/techstore/
    └── foundation/
        └── DatabaseSchemaIntegrationTest.java # PostgreSQL migration/constraint tests
```

No `contracts/` directory is needed: this feature exposes no external interface.

**Structure Decision**: Extend the existing Maven backend's Flyway resource and foundation integration-test locations. Keep schema verification tests in the foundation area; do not add JPA entities or domain behavior merely to represent tables.

## Constitution Check (Post-Design)

- The nine module schemas follow the existing architecture and explicit user decision; V1 remains immutable and `public` retains Flyway metadata.
- The allowed finite state and role values are explicitly approved and included in the spec.
- Migration ordering is explicit and every cross-table target precedes its dependent migration.
- Triggers enforce only data invariants that regular row constraints cannot express; they do not implement operational application workflows.
- Every FK deletion action and the exceptional history-retention behavior are documented in [data-model.md](data-model.md).
- Tests and quickstart cover PostgreSQL itself; no external service dependency is added.

## Review Gate

This plan and its supporting research/data-model/quickstart artifacts are submitted for review before `/speckit-tasks`. Review the cross-schema foreign-key matrix and whether the proposed deletion rules match the desired retention policy before authorizing tasks.
