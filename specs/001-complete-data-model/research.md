# Research: Complete PostgreSQL Data Model

## Decision 1: Preserve the existing module-schema architecture

**Decision**: Preserve `V1__create_module_schemas.sql` unchanged and use its `identity`, `catalog`, and `cart` schemas. Add the six remaining module schemas in a new migration. Place each domain's tables in its module schema and keep Flyway history in `public`.

**Rationale**: The user chose module schemas to match the existing architecture documented in the repository. The feature's original single-schema preference permits a documented technical exception; module data ownership and the existing V1 migration justify it. Applied migrations remain immutable.

**Alternatives considered**:
- Use only the shared `public` schema: rejected by the explicit user decision and current project architecture.
- Edit V1 to create no schemas: rejected because applied migration files must not change.
- Keep only the three existing schemas and put other modules in `public`: rejected because it would break the one-schema-per-module convention.

## Decision 2: Use a unique inventory row per variant as the movement anchor

**Decision**: Enforce `UNIQUE(product_variant_id)` in `inventory.inventory` and have each movement reference the inventory row by FK with `ON DELETE RESTRICT`.

**Rationale**: One inventory row per variant matches the clarified aggregate, location-free balance model. A movement-to-inventory FK makes the historical ledger unambiguous and prevents deleting its balance parent.

**Alternatives considered**:
- Keep variant ID independently on each movement: rejected as redundant and liable to mismatch with a separate inventory row.
- Allow multiple balances per variant: rejected by the agreed scope (no location dimension).

## Decision 3: Represent unknown cart price with SQL NULL

**Decision**: `unit_price_snapshot IS NULL` means unknown; `0` is a known zero price. Enforce nonnegative values and require currency exactly when a price is known.

**Rationale**: Nullability distinguishes absence of knowledge from numeric zero without redundant flags; a check constraint keeps the tuple valid.

**Alternatives considered**:
- Add `price_known`: rejected because it duplicates information represented by nullability.
- Treat zero as unknown: rejected because zero can be a legitimate known price.

## Decision 4: Validate category-dependent attribute values in PostgreSQL triggers

**Decision**: Keep variant values normalized and use trigger-based validation through variant → product → primary category → category/attribute association. Validate declared value type as part of the same write check. Complementary triggers reject category/association edits that invalidate stored values; parent/association row locks serialize conflicting changes.

**Rationale**: A regular `CHECK` cannot query related rows. Composite foreign keys would require repeating category identifiers in variant/value rows; this is unnecessary duplicated data. Triggers provide database-enforced cross-row integrity without that denormalization.

**Alternatives considered**:
- Trust application validation only: rejected because direct SQL writes could violate the model.
- Denormalize category IDs to use composite FKs: rejected in favor of avoiding derivable stored data.

## Decision 5: Enforce publishable-product availability at transaction boundaries

**Decision**: Use deferred constraint triggers for product publication and variant active-state changes. Validate final transaction state and lock the parent product during relevant changes.

**Rationale**: A normal `CHECK` cannot count related active variants. Deferred validation permits a transaction to create a variant and publish a product together, while rejecting committed publication with no active variants.

**Alternatives considered**:
- Application-only validation: rejected because it is not a database guarantee.
- Immediate trigger only: rejected because it rejects valid transactions whose intermediate statement order temporarily has no variant.

## Decision 6: Make attachment owners explicit and exclusive

**Decision**: Use four nullable owner FKs and `CHECK (num_nonnulls(user_id, product_id, contact_request_id, marketing_email_id) = 1)`.

**Rationale**: FK constraints validate each target, and the check ensures exactly one owner. This matches the four approved owner types without an unenforceable polymorphic reference.

**Alternatives considered**:
- `(owner_type, owner_id)` polymorphism: rejected because PostgreSQL cannot enforce one target FK across different tables.
- Separate attachment tables per owner: rejected as unnecessary expansion of the approved model.

## Decision 7: Preserve historical data with explicit FK deletion policy

**Decision**: Cascade only disposable owned data (addresses, authentication artifacts, images, category assignments, cart contents, favorites, and attachment metadata); restrict deletion of products/variants/categories/inventory/campaigns/coupons with retained records; set nullable actor/context FKs to NULL where a durable snapshot remains.

**Rationale**: The policy avoids silent loss of commercial and inventory history while allowing cleanup of disposable dependent data. Exact FK actions are enumerated in `data-model.md`.

**Alternatives considered**:
- Cascade broadly: rejected because it erases durable history.
- Restrict all parent deletion: rejected because it prevents cleanup of disposable owned records.

## Decision 8: Keep Flyway history centralized while qualifying module objects

**Decision**: Configure Flyway with the complete managed-schema list and `public` as its default/history schema. Create all module-owned tables and cross-module FKs with explicitly schema-qualified identifiers.

**Rationale**: Centralized migration history preserves one ordered migration stream while schema qualification makes module ownership and cross-schema relationships unambiguous.

**Alternatives considered**:
- One Flyway history table per schema: rejected because it fragments ordering and complicates fresh-database replay.
- Depend on mutable search paths or unqualified names: rejected because migration behavior would depend on connection defaults.

## Decision 9: Defer all external contract definitions

**Decision**: Do not create API contracts, JPA entities, or application/domain layers in this feature.

**Rationale**: The scope is persistence foundation only and explicitly excludes endpoints, operational business behavior, and unnecessary domain objects.

## Decision 10: Use the approved finite state and role domains

**Decision**: Encode the finite account/role, product/variant, contact-request, review, newsletter, campaign, and recipient values listed in `spec.md` using database `CHECK` constraints.

**Rationale**: The functional requirements call for explicit valid states and roles. The user approved these candidate values so database constraints and positive/negative tests can be concrete. Do not introduce extra lifecycle states or unrelated state columns.

**Alternatives considered**:
- Store arbitrary text values: rejected because invalid states would not be constrained.
- Infer values silently during implementation: rejected because the choices affect behavior and test expectations.
