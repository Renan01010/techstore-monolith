# Feature Specification: Complete PostgreSQL Data Model

**Feature Branch**: `feature-001-fundacion`

**Created**: 2026-10-17

**Status**: Draft — modeling choices recorded; ready for planning review

**Input**: Foundation of the TechStore Monolith relational data model, with versioned PostgreSQL schema creation and the identity, catalog, inventory, cart, commercial, engagement, promotion, marketing, and platform records specified by the requester.

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Establish a repeatable data foundation (Priority: P1)

As a developer setting up TechStore, I need a fresh environment to acquire the complete approved data foundation consistently, so that future business capabilities can be built without reconstructing core relationships.

**Why this priority**: All later capabilities depend on a reliable, reproducible foundation.

**Independent Test**: Start with an empty PostgreSQL database, apply the schema history, and verify that all specified records, relationships, and integrity rules exist without connecting to an external service.

**Acceptance Scenarios**:

1. **Given** an empty PostgreSQL database, **When** the application initializes, **Then** it creates the complete agreed schema and records its migration history.
2. **Given** a database initialized once, **When** the application initializes again, **Then** it completes successfully without duplicating or corrupting schema objects.
3. **Given** a PostgreSQL instance with no external services available, **When** schema initialization is run, **Then** initialization succeeds without depending on those services.

### User Story 2 - Preserve valid records and relationships (Priority: P1)

As a future module developer, I need the data foundation to accept valid identity, catalog, inventory, cart, commercial, engagement, promotion, and marketing records while rejecting invalid values and references.

**Why this priority**: Enforced integrity prevents inconsistent records from becoming a burden on every future module.

**Independent Test**: Insert representative valid records and attempt invalid values, duplicate unique values, and missing or disallowed references; confirm the valid records are accepted and invalid operations are rejected.

**Acceptance Scenarios**:

1. **Given** records with valid identifiers and relationships, **When** they are stored, **Then** their links can be traversed according to the documented model.
2. **Given** a duplicate of a value defined as unique, **When** it is stored, **Then** the duplicate is rejected.
3. **Given** an invalid quantity, state, rating, monetary value, or attribute value, **When** it is stored, **Then** it is rejected.
4. **Given** a record that references a missing parent, **When** it is stored, **Then** it is rejected.

### User Story 3 - Preserve security and business history (Priority: P2)

As a system owner, I need authentication artifacts and commercial history to be handled safely, so that sensitive credentials are not exposed and important records are not erased by unrelated cleanup.

**Why this priority**: Secure authentication data and preserved history are necessary before identity and commercial capabilities are built.

**Independent Test**: Inspect the resulting model and exercise expiry, use, revocation, and deletion rules for authentication artifacts and history-bearing records.

**Acceptance Scenarios**:

1. **Given** a password, session, or verification/reset credential, **When** its record is persisted or an audit event is created, **Then** raw passwords and reusable secret tokens are not stored in plaintext or recorded in logs.
2. **Given** an expired or already-used verification/reset token, **When** it is treated as active, **Then** the model exposes enough state to reject that use.
3. **Given** a historical inventory movement or commercial record, **When** its referenced record is deleted, **Then** the documented retention rule prevents arbitrary loss of history.

### Edge Cases

- Re-running initialization against an already initialized database must not create duplicate objects or alter existing data.
- Concurrent or repeated attempts to create the same unique email, SKU, coupon code, or user/product association must not create duplicates.
- A product with no variants must remain unpublished; an inventory balance at zero, an unknown cart price, and an optional anonymous contact request must be represented without violating integrity rules.
- Values at numeric boundaries, zero and negative quantities, invalid enum/state values, expired tokens, and conflicting primary-image selections must follow explicit constraints.
- Deleting a parent with dependent or historical records must follow the documented cascade, nullification, or restriction rule rather than silently orphaning records.
- Attribute values that do not match their declared type, and attachments with no owner or more than one owner, must be rejected.

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: The system MUST provide a reproducible schema foundation for a fresh PostgreSQL database and record each schema change in version order using Flyway.
- **FR-002**: The system MUST create the following 30 named tables, grouped by logical ownership: identity (`users`, `user_addresses`, `email_verification_tokens`, `password_reset_tokens`, `user_sessions`); catalog (`categories`, `brands`, `products`, `product_variants`, `product_images`, `attributes`, `category_attributes`, `variant_attribute_values`); inventory (`suppliers`, `inventory`, `inventory_movements`); cart (`carts`, `cart_items`); commercial (`contact_requests`, `contact_request_items`); engagement (`product_reviews`, `favorites`); promotions (`coupons`, `coupon_usage`); marketing (`newsletter_subscriptions`, `marketing_emails`, `email_recipients`); and platform (`audit_logs`, `app_settings`, `file_attachments`).
- **FR-003**: Every entity MUST have a UUID identifier; timestamps representing dates or times MUST include time-zone information.
- **FR-004**: User records MUST support unique email addresses, non-plaintext password credentials, account states `pending_verification`, `active`, `suspended`, and `deactivated`, access roles `customer` and `administrator`, and relationships to addresses, verification/reset tokens, and sessions.
- **FR-005**: Verification and password-reset records MUST represent expiration and use state. Session records MUST support expiration and revocation and MUST NOT require storage of a reusable raw session secret.
- **FR-006**: Catalog records MUST represent hierarchical categories, brands, products, commercial variants, unique variant SKUs, product images with ordering and a single designated primary image per product, product publication state (`draft`, `published`, or `archived`), variant state (`active` or `inactive`), product customization capability, dimensions, weight, and monetary prices. Each product MUST reference its primary category and MAY reference a brand. A product MUST NOT be published unless it has at least one active variant; a product with no variants MUST remain unpublished.
- **FR-007**: Catalog attributes MUST support differing technical characteristics across categories and typed variant values, with an explicit rule that rejects a value incompatible with its attribute type. Attribute definitions are reusable across categories through an explicit category-to-attribute association, and a variant value is valid only when its product's primary category is associated with that attribute. PostgreSQL MUST enforce this association on writes and MUST prevent category or association changes from invalidating existing values.
- **FR-008**: Each product variant MUST reference one product, and each product image MUST reference one product. Monetary values MUST use exact decimal representation, reject negative values where not meaningful, and declare currency where the value may otherwise be ambiguous.
- **FR-009**: Inventory MUST represent one nonnegative aggregate balance per product variant, without distinguishing stock locations, enforced by a unique constraint on the variant reference. Every inventory movement MUST reference that inventory balance, record a positive quantity magnitude, movement type, and timestamp, and remain protected from arbitrary deletion. Deleting a balance with movement history MUST be rejected. Maintaining balance changes during operational stock changes is outside this schema-only feature.
- **FR-010**: Supplier records MUST be available to the inventory domain, and an inventory movement MAY reference its supplier as its source. Supplier references used in movement history MUST NOT be silently removed. This relationship MUST NOT imply implementation of procurement, sales, or reservation processes.
- **FR-011**: Carts MUST have a user owner; each cart item MUST reference a commercial variant, have a positive quantity, and be unique per cart and variant. Each cart item MUST retain the price known at addition time and distinguish an unknown price from a known zero price. An absent snapshot price MUST mean unknown; a present price MUST be nonnegative, and zero MUST be accepted as a known price.
- **FR-012**: Contact requests MUST support an optional user association, one of the states `received`, `in_progress`, `completed`, or `cancelled`, and multiple historical line items that preserve the submitted product/variant description and requested quantity when catalog records later change.
- **FR-013**: Product reviews MUST reference a product and a user, except that the user reference becomes null when the account is removed (documented anonymization exception per FR-018), enforce an allowed rating range, and represent one of the moderation states `pending`, `approved`, or `rejected`. Favorites MUST prevent duplicate user/product associations.
- **FR-014**: Coupons MUST have unique codes, a defined benefit type and value, validity period, and global and per-user use limits. Coupon usage records (one row per use, with no uniqueness per coupon and user because limits may exceed one) MUST retain who used the coupon and when, without implementing discount calculation or checkout. The default benefit types are fixed amount and percentage; percentages MUST be bounded to 0–100.
- **FR-015**: Newsletter subscriptions MUST enforce appropriate email uniqueness and represent one of the states `pending_confirmation`, `subscribed`, or `unsubscribed`. Marketing campaigns MUST represent one of the states `draft`, `scheduled`, `completed`, or `cancelled`; recipients MUST represent one of the states `pending`, `sent`, `delivered`, `bounced`, `failed`, or `unsubscribed`. This foundation MUST NOT send email.
- **FR-016**: Audit records MUST support actor, event, time, and relevant non-secret context without storing passwords, reusable tokens, or credentials. Application settings MUST have unique keys and explicit value visibility/sensitivity classification.
- **FR-017**: File attachment records MUST store file metadata and references, not binary file contents. Attachments MUST use explicit foreign-key relationships for the supported owner types: users, products, contact requests, and marketing campaigns. PostgreSQL MUST enforce that exactly one of the four nullable owner foreign keys is set using a row-level check constraint; each non-null owner reference MUST satisfy its foreign key. References to unsupported owner types MUST be rejected. This design avoids polymorphic targets that the database cannot validate with a foreign key.
- **FR-018**: Every foreign-key relationship MUST declare and document its `ON DELETE` behavior. Ephemeral dependent data MAY cascade; references from durable history MUST be restricted or nulled while preserving historical snapshots. Account removal MUST preserve required business history through deactivation, anonymization, or nullable actor references rather than cascading away history. Deletion behavior for every relationship MUST be enumerated in the data model.
- **FR-019**: Uniqueness, required fields, valid enumerated states, positive/nonnegative quantities, valid date ranges, and other domain invariants MUST be enforced at the data boundary, with indexes on foreign-key columns and frequent lookup paths.
- **FR-020**: The model MUST use one PostgreSQL database and preserve logical ownership for identity, catalog, inventory, cart, commercial, engagement, promotions, marketing, and platform data. By the explicit project architecture decision for this feature, each module's tables MUST reside in its own PostgreSQL schema (`identity`, `catalog`, `inventory`, `cart`, `commercial`, `engagement`, `promotions`, `marketing`, or `platform`). This is a documented exception to the initial shared-schema preference, justified by the existing modular-schema architecture and V1 migration.
- **FR-021**: The feature MUST NOT create orders or payments, implement checkout, perform discount calculations, send marketing email, implement procurement/sales/reservations, add unnecessary domain entities or business logic, store binary files in the database, or introduce microservices or internal HTTP communication.
- **FR-022**: Automated verification MUST use a real PostgreSQL-compatible integration environment to validate fresh initialization, repeat initialization, expected tables/constraints/indexes, uniqueness, invalid values, referential integrity, deletion behavior, and primary relationships. Tests that cannot run due to unavailable infrastructure MUST be explicitly reported as not run, not passed.
- **FR-023**: Schema changes MUST be organized into small, domain-oriented migrations rather than one migration for the entire model. An applied migration MUST NOT be edited; subsequent schema changes MUST use a new migration.
- **FR-024**: Model documentation MUST identify every table and relationship, its logical owner, important integrity constraints and indexes, and deletion behavior, including the reason when historical records restrict deletion.

### Key Entities

- **User and identity artifacts**: User account, addresses, email verification credentials, password-reset credentials, and revocable sessions.
- **Catalog**: Category tree, brands, products, commercial variants, product images, reusable typed attribute definitions, category-to-attribute assignments, and variant attribute values.
- **Inventory**: Suppliers, one aggregate balance per variant, and immutable or protected quantity movement history.
- **Cart**: User-owned carts and variant lines with quantity and a snapshot of known or unknown price.
- **Commercial requests**: Optional-user contact requests and durable item snapshots.
- **Engagement**: Moderated product reviews and unique user/product favorites.
- **Promotions**: Coupon rules and auditable usage records with global and per-user limits.
- **Marketing**: Email subscriptions, campaign records, and campaign recipients.
- **Platform**: Audit events, classified application settings, and file metadata linked by explicit foreign keys to supported owners.

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: A new, empty PostgreSQL database can be initialized successfully from the versioned schema history, with all 30 named tables available and no external service required.
- **SC-002**: Repeating initialization on the same database succeeds without duplicate schema objects, lost data, or migration-history errors.
- **SC-003**: Every required primary relationship and every documented unique, check, and deletion rule passes its corresponding positive and negative integration test.
- **SC-004**: Invalid references, duplicate unique values, invalid states, out-of-range ratings, and negative or otherwise invalid quantities are rejected in all tested cases.
- **SC-005**: Reviewers can trace every table to one of the nine logical modules and find its relationships, important constraints, indexes, and deletion behavior in the model documentation.
- **SC-006**: No password, reusable session secret, verification/reset secret, or credential appears in plaintext in persisted authentication artifacts or audit records.
- **SC-007**: Every test is reported as passed, failed, or not run; infrastructure-related omissions are identified without being reported as successful.

## Assumptions

- PostgreSQL, Flyway, UUID identifiers, time-zone-aware timestamps, and exact decimal monetary values are mandatory product constraints supplied by the requester, not choices made by this specification.
- The model uses one PostgreSQL database with one schema per logical module. This explicit architecture choice follows the project's existing module-schema convention and the approved decision for this feature; `public` remains available for Flyway metadata. No module-to-module HTTP communication is introduced.
- Allowed account states, access roles, publication/variant states, contact request states, review moderation states, newsletter states, campaign states, and recipient states use the finite values listed in the functional requirements, as explicitly approved during planning.
- Passwords are stored only as password hashes. Session, verification, and reset secrets are stored as one-way token digests, with issue/expiry/use or revocation timestamps as applicable.
- Technical attribute values support text, decimal, and boolean forms. Exactly one typed value form must be populated and it must match the attribute definition. Attribute definitions may be assigned to multiple categories, and a variant may only have values for attributes assigned to its product's primary category.
- Each product has one primary category in this initial model; support for placing the same product in multiple categories is not required by this feature.
- Product publication requires at least one active commercial variant. The database rejects publication without one and prevents deactivating the last active variant while the product remains published.
- Inventory movements are append-only to normal application operations. A movement is classified (for example, receipt, adjustment, or removal) and records a positive magnitude; its type determines its effect on balance.
- Durable commercial and inventory history is preserved when users, products, or variants are retired. The proposed safe default is to deactivate/anonymize a user where history requires it, restrict destructive deletion of referenced historical entities, and cascade only disposable owned records.
- Coupons may provide either a fixed monetary amount or a percentage, and usage limits are global and per user. Validity has an inclusive start and end; no stacking or discount application behavior is included.
- File attachments contain metadata and an owner reference only; files themselves live outside the database. The initial supported attachment owners are users, products, contact requests, and marketing campaigns; explicit foreign keys enforce those relationships.
- A missing cart price is represented separately from a known zero price. A contact request may exist without a registered user, and its item description is preserved as a historical snapshot.
- PostgreSQL integration testing is expected to use an isolated real PostgreSQL instance, preferably a disposable container. If Docker or equivalent infrastructure is unavailable, affected tests remain explicitly unexecuted.
- This feature defines persistence requirements only; it does not add JPA entities or operational business workflows unless later implementation artifacts establish a concrete need.
