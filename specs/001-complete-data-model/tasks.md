---

description: "Task list for Feature 001 — Complete PostgreSQL Data Model"
---

# Tasks: Complete PostgreSQL Data Model

**Input**: `specs/001-complete-data-model/` (plan.md, spec.md, data-model.md, research.md, quickstart.md)

**Prerequisites**: plan.md, spec.md. Tests are required by the spec (real PostgreSQL via Testcontainers, positive and negative cases).

**Scope guard**: Schema only. NO JPA entities, endpoints, services, balance mutation, checkout, orders, payments or e-mail sending. Existing `V1__create_module_schemas.sql` is immutable.

**Organization**: Grouped by user story. US1 = repeatable foundation (P1), US2 = valid records and relationships (P1), US3 = security and history protection (P2).

## Format: `[ID] [P?] [Story] Description`

## Path Conventions

- Migrations: `backend/src/main/resources/db/migration/`
- Tests: `backend/src/test/java/com/techstore/foundation/`
- Build/run: `mvn -f backend/pom.xml verify` (Maven is not on PATH in the authoring environment; Docker availability is unverified. Tests that cannot run MUST be reported as "not run", never as passed.)

## Decisions fixed for tasks (approved)

- `inventory_movements.movement_type` ∈ `receipt`, `removal`, `adjustment_increase`, `adjustment_decrease`; `quantity` always positive.
- `app_settings.visibility` ∈ `public`, `internal`, `secret`.
- Other technical conventions (column names, lengths, `currency` as 3 uppercase letters, address type) are chosen consistently with `data-model.md` without changing requirements.
- Naming prefixes: `pk_`, `fk_`, `uq_`, `ck_`, `ix_`. All FK constraints and ON DELETE actions are named/explicit and schema-qualified. Every FK column is indexed.

---

## Phase 1: Setup

- [X] T001 Extend `spring.flyway.schemas` in `backend/src/main/resources/application.yml` to `public, identity, catalog, inventory, cart, commercial, engagement, promotions, marketing, platform`, keeping `default-schema: public`, `create-schemas: true` and the Flyway history table in `public`
- [X] T002 [P] Create shared test support `backend/src/test/java/com/techstore/foundation/PostgresIntegrationTestSupport.java`: `@Testcontainers(disabledWithoutDocker = true)`, `@SpringBootTest`, `postgres:17-alpine` and `@DynamicPropertySource` (same pattern as `DatabaseReadinessIntegrationTest`), plus helpers to insert minimal valid rows and to assert `SQLException` SQLSTATE (23502 not-null, 23503 FK, 23505 unique, 23514 check, 23001/23503 restrict, P0001 trigger)

## Phase 2: Foundational (blocks all stories)

- [X] T003 Create `backend/src/main/resources/db/migration/V2__create_remaining_module_schemas.sql` creating schemas `inventory`, `commercial`, `engagement`, `promotions`, `marketing`, `platform` with `CREATE SCHEMA IF NOT EXISTS`
- [X] T004 Update `backend/src/test/java/com/techstore/foundation/DatabaseReadinessIntegrationTest.java`: it must assert all nine module schemas exist and `flyway_schema_history` is in `public`; remove the assertion that `identity`, `catalog`, `cart` contain no tables (it will be false after V3+)
- [X] T005 Confirm `backend/src/test/java/com/techstore/architecture/ModuleBoundariesTest.java` still passes (no Java production code is added by this feature)

**Checkpoint**: schemas exist; later migrations can reference them.

---

## Phase 3: User Story 1 — Repeatable foundation (P1) 🎯 MVP

**Goal**: An empty PostgreSQL reaches the full 30-table schema through ordered Flyway migrations V1–V12, and a second startup is a safe no-op.

**Independent test**: Start a clean container, run the application, count the 30 expected tables in their owning schemas; restart against the same database with no new migration applied and no failure.

### Tests for US1

- [X] T006 [P] [US1] Create `backend/src/test/java/com/techstore/foundation/DatabaseSchemaIntegrationTest.java`: (a) all migrations V1–V12 succeed on an empty database with no failed rows in `flyway_schema_history`; (b) exactly the 30 expected tables exist in `identity` (5), `catalog` (8), `inventory` (3), `cart` (2), `commercial` (2), `engagement` (2), `promotions` (2), `marketing` (3), `platform` (3); (c) every table has a UUID primary key named `pk_*`; (d) every FK column has a supporting `ix_*` index; (e) every FK has an explicit non-default ON DELETE rule matching the matrix below; (f) all timestamp columns are `timestamptz` and monetary columns are `numeric`; (g) no `bytea` column exists
- [X] T007 [P] [US1] Add to `DatabaseSchemaIntegrationTest.java` a repeat-initialization test: start the context again (`Flyway.migrate()` second run) on the same database and assert no new history rows and unchanged schema; a second test that migration order has no forward references (guaranteed by successful clean run from empty)
- [X] T008 [P] [US1] Add to `DatabaseSchemaIntegrationTest.java` a test that the test setup uses no external service other than the PostgreSQL container (no network URL properties beyond the container's)

### Implementation for US1 (migrations, order is mandatory; each file contains only its domain)

- [X] T009 [US1] Create `V3__create_identity_tables.sql` (schema `identity`): `users` (unique normalized email `uq_users_email`, `password_hash` only, `account_state` CHECK in `pending_verification`,`active`,`suspended`,`deactivated`, `access_role` CHECK in `customer`,`administrator`, created/updated `timestamptz`), `user_addresses`, `email_verification_tokens`, `password_reset_tokens`, `user_sessions` (unique `token_digest` columns, `expires_at`, `used_at`/`revoked_at`, CHECK `expires_at > created_at`; digest only, no raw secret); FKs to `identity.users(id)` ON DELETE CASCADE; index each FK
- [X] T010 [US1] Create `V4__create_catalog_core_tables.sql` (schema `catalog`): `categories` (unique slug, `parent_category_id` FK ON DELETE RESTRICT, CHECK `parent_category_id <> id`), `brands` (unique slug), `products` (`primary_category_id` NOT NULL FK RESTRICT, `brand_id` FK ON DELETE SET NULL, unique slug, `publication_state` CHECK in `draft`,`published`,`archived`, `customizable` boolean), `attributes` (`value_type` CHECK in `text`,`numeric`,`boolean`, optional unit)
- [X] T011 [US1] Create `V5__create_catalog_variants_and_attribute_rules.sql`: `category_attributes` (UNIQUE `(category_id, attribute_id)`; category FK CASCADE, attribute FK RESTRICT), `product_variants` (`product_id` FK RESTRICT, unique SKU, `variant_state` CHECK in `active`,`inactive`, `price` NUMERIC nonnegative with `currency` 3 uppercase letters, dimensions and weight nonnegative when present), `product_images` (`product_id` FK CASCADE, external reference only, `sort_order` unique per product, partial unique index `uq_product_images_primary` where primary flag is true), `variant_attribute_values` (variant FK CASCADE, attribute FK RESTRICT, text/numeric/boolean columns with CHECK that exactly one is populated, UNIQUE `(product_variant_id, attribute_id)`); then the cross-row triggers of T012–T013 in this same file
- [X] T012 [US1] In V5, add trigger function(s) validating category-specific attributes: on insert/update of `variant_attribute_values`, resolve variant → product → `primary_category_id`, require `(category_id, attribute_id)` in `category_attributes` and require the populated typed column to match `attributes.value_type`; lock the product and assignment rows in the validation path; do not store category on the value row
- [X] T013 [US1] In V5, add complementary triggers that reject (a) changing `products.primary_category_id` and (b) deleting a `category_attributes` row when existing variant values would become invalid; add the deferred constraint triggers on `products` and `product_variants` requiring at least one `active` variant for every `published` product at commit, locking the parent product row on state changes
- [X] T014 [US1] Create `V6__create_inventory_tables.sql` (schema `inventory`): `suppliers` (identity/contact, active flag), `inventory` (`product_variant_id` FK RESTRICT with `uq_inventory_product_variant`, `quantity` CHECK `>= 0`, no location), `inventory_movements` (`inventory_id` FK RESTRICT, `supplier_id` FK RESTRICT, `movement_type` CHECK in `receipt`,`removal`,`adjustment_increase`,`adjustment_decrease`, `quantity` CHECK `> 0`, `occurred_at`, non-secret reference/reason); add `BEFORE UPDATE OR DELETE` trigger on `inventory_movements` raising an exception (append-only); no balance-mutation logic
- [X] T015 [US1] Create `V7__create_cart_tables.sql` (schema `cart`): `carts` (`user_id` FK CASCADE), `cart_items` (`cart_id` FK CASCADE, `product_variant_id` FK RESTRICT, UNIQUE `(cart_id, product_variant_id)`, `quantity` CHECK `> 0`, nullable `unit_price_snapshot` and `currency`, CHECK `(unit_price_snapshot IS NULL) = (currency IS NULL)` and `unit_price_snapshot >= 0` when present: NULL = unknown, 0 = known zero)
- [X] T016 [US1] Create `V8__create_commercial_tables.sql` (schema `commercial`): `contact_requests` (`user_id` FK SET NULL, contact snapshot, `request_state` CHECK in `received`,`in_progress`,`completed`,`cancelled`), `contact_request_items` (`contact_request_id` FK RESTRICT, `product_id` and `product_variant_id` FK SET NULL, description/SKU snapshot, `quantity` CHECK `> 0`)
- [X] T017 [US1] Create `V9__create_engagement_tables.sql` (schema `engagement`): `product_reviews` (`product_id` FK RESTRICT, `user_id` nullable FK SET NULL, `rating` CHECK between 1 and 5, `moderation_state` CHECK in `pending`,`approved`,`rejected`), `favorites` (FK CASCADE on user and product, UNIQUE `(user_id, product_id)`)
- [X] T018 [US1] Create `V10__create_promotion_tables.sql` (schema `promotions`): `coupons` (unique normalized code, `benefit_type` CHECK in `fixed_amount`,`percentage`, value nonnegative, percentage `<= 100`, `valid_from`/`valid_until` CHECK `valid_until > valid_from`, optional global and per-user limits CHECK `> 0`, active flag), `coupon_usage` (`coupon_id` FK RESTRICT, `user_id` nullable FK SET NULL, `used_at`; no unique constraint on `(coupon_id, user_id)` because one row is stored per use; index the FKs)
- [X] T019 [US1] Create `V11__create_marketing_tables.sql` (schema `marketing`): `newsletter_subscriptions` (unique normalized email, `subscription_state` CHECK in `pending_confirmation`,`subscribed`,`unsubscribed`), `marketing_emails` (`campaign_state` CHECK in `draft`,`scheduled`,`completed`,`cancelled`; content reference only), `email_recipients` (`marketing_email_id` FK RESTRICT, `user_id` nullable FK SET NULL, recipient email snapshot, `delivery_state` CHECK in `pending`,`sent`,`delivered`,`bounced`,`failed`,`unsubscribed`, UNIQUE `(marketing_email_id, recipient_email)`)
- [X] T020 [US1] Create `V12__create_platform_tables.sql` (schema `platform`): `audit_logs` (`actor_user_id` nullable FK SET NULL, action, descriptive `target_type`/`target_id` with NO FK, `occurred_at`, non-secret `details` jsonb), `app_settings` (unique key, value, `visibility` CHECK in `public`,`internal`,`secret`), `file_attachments` (metadata and external storage reference only; nullable `user_id`, `product_id`, `contact_request_id`, `marketing_email_id` each with own FK ON DELETE CASCADE and index; `CHECK (num_nonnulls(user_id, product_id, contact_request_id, marketing_email_id) = 1)`); add SQL `COMMENT ON` for `audit_logs.target_type/target_id` documenting the polymorphic integrity limitation
- [X] T021 [US1] Run `mvn -f backend/pom.xml verify` (or record "not run" with the reason if Maven/Docker is unavailable) and confirm T006–T008 and T004–T005 pass

**Checkpoint**: US1 delivers the full schema from an empty database. MVP complete.

---

## Phase 4: User Story 2 — Valid records and relationships (P1)

**Goal**: Valid records are accepted and invalid ones rejected by PostgreSQL: uniqueness, NOT NULL, CHECK, FK, triggers and ON DELETE.

**Independent test**: Per-domain test classes insert minimal valid rows and assert that each invalid variant fails with the expected SQLSTATE.

### Tests for US2 (each positive case paired with negative cases)

- [X] T022 [P] [US2] Create `backend/src/test/java/com/techstore/foundation/IdentitySchemaIntegrationTest.java`: duplicate email differing by case rejected; invalid account state/role rejected; token and session digest uniqueness; `expires_at` before creation rejected; deleting a user cascades addresses, tokens, sessions
- [X] T023 [P] [US2] Create `backend/src/test/java/com/techstore/foundation/CatalogSchemaIntegrationTest.java`: duplicate category/brand slug, duplicate SKU, self-parent category, deleting a category with children or products (RESTRICT), deleting a brand sets `brand_id` NULL, negative price/dimension/weight, invalid currency, duplicate image sort order, two primary images rejected while one is accepted, deleting a product with variants rejected, deleting a product without variants cascades images
- [X] T024 [P] [US2] Create `backend/src/test/java/com/techstore/foundation/CatalogAttributeRulesIntegrationTest.java`: value accepted for an attribute assigned to the product's primary category; rejected when not assigned; rejected when typed column does not match `value_type`; rejected when zero or two typed columns populated; duplicate `(variant, attribute)` rejected; changing the primary category or removing an assignment is rejected while values would become invalid and allowed otherwise; attribute reused by two categories works
- [X] T025 [P] [US2] Create `backend/src/test/java/com/techstore/foundation/PublicationRuleIntegrationTest.java`: publishing a product with no variants fails at commit; with only inactive variants fails; with one active variant succeeds; deactivating the last active variant of a published product fails; draft product without variants is allowed
- [X] T026 [P] [US2] Create `backend/src/test/java/com/techstore/foundation/InventoryCartSchemaIntegrationTest.java`: second `inventory` row for the same variant rejected; negative balance rejected; movement with zero/negative quantity or invalid type rejected; deleting a supplier referenced by a movement rejected; deleting an inventory row with movements rejected; deleting a variant with inventory or cart items rejected; cart item quantity ≤ 0 rejected; duplicate variant in same cart rejected; price NULL with currency rejected, price with NULL currency rejected, price NULL/NULL accepted (unknown), price 0 with currency accepted (known zero), negative price rejected; deleting a cart cascades its items
- [X] T027 [P] [US2] Create `backend/src/test/java/com/techstore/foundation/CommercialEngagementSchemaIntegrationTest.java`: invalid request state rejected; deleting a user sets `contact_requests.user_id` NULL while snapshot remains; deleting a request with items rejected; deleting a product/variant sets item references NULL; item quantity ≤ 0 rejected; rating 0 and 6 rejected, 1 and 5 accepted; invalid moderation state rejected; deleting a product with reviews rejected; deleting a user sets review user NULL; duplicate favorite rejected; deleting user or product cascades favorites
- [X] T028 [P] [US2] Create `backend/src/test/java/com/techstore/foundation/PromotionsMarketingSchemaIntegrationTest.java`: duplicate coupon code rejected; several usages of one coupon by the same user accepted; percentage above 100 or negative value rejected; invalid benefit type rejected; `valid_until` not after `valid_from` rejected; non-positive limits rejected; deleting a coupon with usage rejected; deleting a user sets usage `user_id` NULL; duplicate newsletter email rejected; invalid subscription/campaign/delivery states rejected; duplicate recipient e-mail in one campaign rejected, same e-mail in another campaign accepted; deleting a campaign with recipients rejected; deleting a user sets recipient `user_id` NULL
- [X] T029 [P] [US2] Create `backend/src/test/java/com/techstore/foundation/PlatformSchemaIntegrationTest.java`: attachment with zero owners rejected; with two owners rejected; with exactly one of each of the four owner types accepted; deleting each owner type cascades its attachment; duplicate setting key rejected; invalid visibility rejected; `audit_logs` accepts an arbitrary target descriptor with no FK and survives actor deletion (`actor_user_id` NULL)

### Implementation for US2

- [X] T030 [US2] Fix any constraint, trigger or index defects revealed by T022–T029 by editing the migration that is still unreleased (V3–V12 only; never V1); if a migration has already been applied to a shared database, add a new migration instead
- [X] T031 [US2] Run the US2 test classes and record each as passed, failed or "not run" with reason

**Checkpoint**: Every rule in `data-model.md` has at least one positive and one negative test.

---

## Phase 5: User Story 3 — Security and history protection (P2)

**Goal**: Secrets are never stored, commercial/inventory history is not arbitrarily erased, and limits of polymorphic references are documented.

**Independent test**: Inspect the catalog of columns and attempt mutation of protected history.

### Tests for US3

- [X] T032 [P] [US3] Create `backend/src/test/java/com/techstore/foundation/SecurityAndHistoryIntegrationTest.java`: no column in any module table is named `password`, `token`, `secret`, `api_key` or `credential` (only `password_hash`, `*_digest` allowed); `UPDATE` and `DELETE` of `inventory_movements` fail with the trigger error and a compensating `INSERT` succeeds; none of the history relations (movements, contact request items, coupon usage, email recipients, reviews) use CASCADE to their parent; `audit_logs` has no FK to a target table

### Implementation for US3

- [X] T033 [US3] Verify V6 append-only trigger and V12 comments satisfy T032; adjust the migration only if unreleased (see T030)

---

## Phase 6: Polish and documentation

- [X] T034 [P] Update `README.md`: replace the "schemas identity, catalog and cart / migration only creates schemas" text with the nine module schemas, V1–V12 list, how to run migrations and tests, and the Docker limitation note
- [X] T035 [P] Make `specs/001-complete-data-model/data-model.md` match the final SQL (column names, the approved movement and visibility values) and add the table/relationship summary and polymorphic-reference limitation (`audit_logs`, `file_attachments`)
- [X] T036 [P] Add a "Test execution report" section to `specs/001-complete-data-model/quickstart.md` listing each test class as passed, failed or not run
- [X] T037 Run the quickstart scenarios 1–8 and the full `mvn -f backend/pom.xml verify`; confirm no JPA entity, controller, service or dependency was added (`git diff --stat` shows only migrations, tests, `application.yml`, README and spec docs)

---

## Dependencies and Order

- Setup (T001–T002) → Foundational (T003–T005) → US1 → US2 → US3 → Polish.
- Migration order is strict: V2 → V3 → V4 → V5 (needs V4) → V6 (needs V5 variants) → V7 (needs V3, V5) → V8 (V3, V4, V5) → V9 (V3, V4) → V10 (V3) → V11 (V3) → V12 (V3, V4, V8, V11).
- Tests T006–T008 can be written before the migrations (they fail first); US2 test classes T022–T029 are mutually independent and parallel; T030 depends on them.
- US3 tests depend on V6 and V12 only.

## Parallel Examples

- After T003: T006, T007, T008 (same file, but written together) while T009 and T010 proceed.
- US2: T022–T029 are all different files and can be written in parallel.
- Polish: T034, T035, T036 in parallel.

## Implementation Strategy

1. MVP = US1: schemas, V3–V12, clean-database and repeat-start tests passing.
2. Add US2 positive/negative constraint tests and fix defects in unreleased migrations.
3. Add US3 security/history tests.
4. Finish documentation and report truthfully which tests ran.

---

## Phase 7: Convergence

- [ ] T038 Run the complete `mvn -f backend/pom.xml verify` in one pass (with enough free memory, or `-DargLine=-Xmx600m` and a container memory limit) and record the result in the Test Execution Report of `specs/001-complete-data-model/quickstart.md`; T021 and T037 were marked done from per-class runs only per SC-007 and Constitution V (partial)
- [ ] T039 Complete `specs/001-complete-data-model/data-model.md` so each table lists its final column names, constraint names and indexes as created in `V3`–`V12`, since T035 only updated a few values, per FR-024 and SC-005 (partial)
- [ ] T040 Add a Spring-context restart test in `backend/src/test/java/com/techstore/foundation/DatabaseReadinessIntegrationTest.java` (or a new test class) that starts the application twice against the same PostgreSQL container, with data inserted between starts, and asserts no new `flyway_schema_history` rows and no data loss, per SC-002 and FR-022 (partial; today only `Flyway.migrate()` is repeated)
- [ ] T041 Resolve the coupon validity boundary in `backend/src/main/resources/db/migration/V10__create_promotion_tables.sql`: spec Assumptions say validity has an "inclusive start and end" but `ck_coupons_validity` is `valid_until > valid_from`; either change it to `>=` or record the decision in `spec.md`, and adjust `PromotionsMarketingSchemaIntegrationTest.java` to match, per FR-014 (contradicts; edit V10 only while it is unreleased, otherwise add a new migration)
- [ ] T042 Add a test in `backend/src/test/java/com/techstore/foundation/SecurityAndHistoryIntegrationTest.java` showing that `platform.audit_logs.details` and `identity` authentication tables hold only digests and hashes (no raw-secret column, and the documented contract is present in the table comment), per FR-016 and SC-006 (partial)
- [ ] T043 Add to `specs/001-complete-data-model/plan.md` the approved cross-schema foreign keys as the module persistence rule (for example `cart` → `catalog`, `platform` → `identity`) and update the test layout to the real classes under `backend/src/test/java/com/techstore/foundation/`, per Constitution II and III (partial)
- [ ] T044 Review and justify, or remove in a new migration if the V3–V12 files have already been committed, extras not called for by the spec: `uq_user_addresses_default` (V3), `is_required` on `category_attributes`, the `BEFORE TRUNCATE` trigger on `inventory_movements` (V6), and the attribute-type guard trigger (V5); record each decision in `specs/001-complete-data-model/research.md` (unrequested)
