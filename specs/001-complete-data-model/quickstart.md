# Quickstart: PostgreSQL Schema Validation

This guide defines verification scenarios for implementation. It is not a setup guide for business workflows.

## Prerequisites

- Java 21 and Maven available.
- Docker available for PostgreSQL Testcontainers tests.
- Run from the repository root.

If Docker is unavailable, record PostgreSQL integration tests as **not run**. Do not report them as passing.

## Validation

1. Run the backend test suite:

   ```bash
   cd backend
   mvn -f backend/pom.xml verify
   ```

2. Verify `DatabaseSchemaIntegrationTest` provisions a real PostgreSQL instance, starts from an empty database, and applies V1 through V12 in order.
3. Restart the application against the same database and verify Flyway initialization succeeds again while schema objects and existing data remain intact.
4. Verify all 30 expected tables, named constraints, FK indexes, partial unique image index, each table's assigned module schema, and Flyway's history in `public`.
5. Run a positive insert scenario for every FK relationship in the model, along with representative valid attributes, zero cart price, unknown cart price, inventory at zero, and publishing a product with an active variant.
6. Run negative scenarios for each declared unique key, every invalid state/role from `spec.md`, invalid prices/quantities/rating/type, missing required FK targets, disallowed category attributes, category changes that invalidate values, published products without active variants, and attachment owner counts other than one.
7. Test every `ON DELETE` outcome in the [data model](data-model.md), including cascade of ephemeral records, nullable-history retention, and RESTRICT behavior for history-bearing references.
8. Confirm inventory movement UPDATE/DELETE are rejected and audit/authentication records do not contain raw secrets.

## Expected Outcome

- Fresh migration and repeated initialization both succeed on PostgreSQL.
- Every documented valid insert succeeds; every invalid operation is rejected by PostgreSQL.
- History-preserving delete operations retain historical records, and disposable dependent records follow the documented cascade.
- No external service, JPA entity, API, checkout, payment, email send, or inventory operation is required to run these tests.

## Test Execution Report

Run with Docker (Testcontainers, postgres:17-alpine), one class at a time in maven:3.9-eclipse-temurin-21 with limited memory.

Passed: DatabaseSchemaIntegrationTest (8), IdentitySchemaIntegrationTest (7), CatalogSchemaIntegrationTest (8), CatalogAttributeRulesIntegrationTest (8), PublicationRuleIntegrationTest (7), InventoryCartSchemaIntegrationTest (7), CommercialEngagementSchemaIntegrationTest (6), PromotionsMarketingSchemaIntegrationTest (8), PlatformSchemaIntegrationTest (7), SecurityAndHistoryIntegrationTest (5), DatabaseReadinessIntegrationTest (1), ModuleBoundariesTest (1).

Not run: a single combined mvn verify of the whole suite (run per class to limit memory) and any test class outside this list.
