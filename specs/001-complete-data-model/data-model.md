# Data Model: Complete PostgreSQL Foundation

Use one PostgreSQL database with one schema per module: `identity`, `catalog`, `inventory`, `cart`, `commercial`, `engagement`, `promotions`, `marketing`, and `platform`. The existing `V1__create_module_schemas.sql` creates the first, second, and fourth schemas; the next migration creates the remaining six. Keep the single Flyway history table in `public`. Every table has a UUID primary key and UTC-capable `TIMESTAMPTZ` audit timestamps where applicable. Monetary columns use `NUMERIC` plus an explicit currency code. Tables listed under each domain heading belong to that module's schema; migration FKs must use fully qualified identifiers.

## Identity

| Table | Principal data and integrity |
|---|---|
| `users` | Unique normalized email; password hash only; account state (`pending_verification`, `active`, `suspended`, `deactivated`), access role (`customer`, `administrator`), created/updated timestamps. No plaintext password or reusable auth token. |
| `user_addresses` | Required `user_id`, address fields, address type/default marker. |
| `email_verification_tokens` | Required `user_id`, unique token digest, creation/expiry/use timestamps; digest only. |
| `password_reset_tokens` | Required `user_id`, unique token digest, creation/expiry/use timestamps; digest only. |
| `user_sessions` | Required `user_id`, unique session-token digest, creation/expiry/revocation timestamps; never raw session secret. |

## Catalog

| Table | Principal data and integrity |
|---|---|
| `categories` | Name, unique slug, optional parent category; reject self-parenting. |
| `brands` | Name and unique slug. |
| `products` | Required primary category; optional brand; name/slug/description; publication state (`draft`, `published`, `archived`), customization flag, created/updated timestamps. Published status requires an active variant at commit. |
| `product_variants` | Required product, unique SKU, state (`active`, `inactive`), exact price/currency, dimensions and weight with nonnegative checks where applicable. |
| `product_images` | Required product, external file/image reference, alt text, sort order, primary flag. Unique order per product and partial unique index allowing at most one primary image per product. |
| `attributes` | Name, type (`text`, `numeric`, or `boolean`), optional unit, and required/type metadata. |
| `category_attributes` | Unique `(category_id, attribute_id)` assignment; attribute definitions can be reused across categories. |
| `variant_attribute_values` | Required variant and attribute; columns for text/numeric/boolean forms. A `CHECK` requires exactly one populated value and type-specific validation requires it to match the attribute definition. Trigger requires the attribute to be assigned to the product's primary category. |

## Inventory

| Table | Principal data and integrity |
|---|---|
| `suppliers` | Supplier identity/contact and active state; no procurement workflow. |
| `inventory` | Required unique `product_variant_id`; one nonnegative aggregate balance per variant, no location. |
| `inventory_movements` | Required inventory FK, optional supplier FK, movement type (`receipt`, `removal`, `adjustment_increase`, `adjustment_decrease`), positive quantity magnitude, timestamp, non-secret reference/reason. Protected from update and delete; no automatic balance mutation. |

## Cart

| Table | Principal data and integrity |
|---|---|
| `carts` | Required user owner and state/created/updated timestamps. |
| `cart_items` | Required cart and variant; positive quantity; unique `(cart_id, product_variant_id)`; nullable price snapshot and currency. Check: price is NULL iff currency is NULL; otherwise price is nonnegative and currency is present. Thus NULL means unknown and numeric zero is known zero. |

## Commercial

| Table | Principal data and integrity |
|---|---|
| `contact_requests` | Optional user, contact details snapshot, state (`received`, `in_progress`, `completed`, `cancelled`), created/updated timestamps. |
| `contact_request_items` | Required request, optional product/variant references, description/SKU snapshot, positive quantity and submitted context needed for history. |

## Engagement

| Table | Principal data and integrity |
|---|---|
| `product_reviews` | Required product, nullable user after account removal, rating check (1–5), review text, moderation state (`pending`, `approved`, `rejected`), timestamps. |
| `favorites` | Required user/product and unique `(user_id, product_id)`. |

## Promotions

| Table | Principal data and integrity |
|---|---|
| `coupons` | Unique normalized code, fixed-amount or percentage type, nonnegative amount/percentage checks, validity range, optional positive global/per-user limits, active state. Percentage capped at 100. |
| `coupon_usage` | Required coupon, nullable user for anonymized history, use timestamp. One row per use, so no unique coupon/user constraint; per-user and global limits are stored data enforced by a future feature. Records are retained when the user is deleted. |

## Marketing

| Table | Principal data and integrity |
|---|---|
| `newsletter_subscriptions` | Unique normalized email, state (`pending_confirmation`, `subscribed`, `unsubscribed`), subscribed/unsubscribed timestamps. |
| `marketing_emails` | Campaign name/content reference and state (`draft`, `scheduled`, `completed`, `cancelled`)/timestamps; no send operation. |
| `email_recipients` | Required campaign, optional user, recipient email snapshot, state (`pending`, `sent`, `delivered`, `bounced`, `failed`, `unsubscribed`) and timestamps; unique recipient email per campaign. |

## Platform

| Table | Principal data and integrity |
|---|---|
| `audit_logs` | Optional actor, event/action, target context, occurrence time, non-secret structured details. Any `target_type`/`target_id` pair is descriptive historical context only, not a polymorphic FK; PostgreSQL cannot validate the target across module tables. Prohibit secret-bearing fields by schema contract and test. |
| `app_settings` | Unique key, value, visibility classification (`public`, `internal`, `secret`), timestamps. Secret values must not be copied into audit records. |
| `file_attachments` | Metadata and external storage reference only; nullable owner FKs to user/product/contact request/marketing campaign; check exactly one owner is non-null. No binary data stored here. |

## Cross-Row Invariants

1. **Category-specific variant attributes**: On insert/update of a variant value, a PostgreSQL trigger resolves its variant's product and the product's primary category, verifies `(category_id, attribute_id)` exists in `category_attributes`, and verifies the populated typed-value column matches `attributes.value_type`. On changes to a product's primary category or deletion of a category/attribute assignment, a complementary trigger rejects changes that would leave stored values invalid. Serialize conflicting writes by locking the product and category-assignment rows in the validation path; do not store a duplicate category identifier on each value.
2. **Published products**: A deferred constraint trigger on products and product variants validates at transaction commit that each published product has at least one active variant. Product and variant state changes lock the parent product row to serialize concurrent publication/activation changes.
3. **Append-only movement history**: Prevent `UPDATE` and `DELETE` of `inventory_movements` through database trigger protection. Corrections are represented by a subsequent compensating movement in a future operational feature.
4. **Attachment owner exclusivity**: A check on `num_nonnulls(user_id, product_id, contact_request_id, marketing_email_id)` equals 1. Each owner column has its own FK.

## ON DELETE Matrix

`CASCADE` is reserved for ephemeral/dependent records. History is preserved by `RESTRICT` or `SET NULL` with copied context.
Table names in the matrix are schema-qualified using the module ownership listed above.

| FK relationship | ON DELETE | Reason |
|---|---|---|
| `identity.user_addresses.user_id → identity.users.id` | CASCADE | Disposable account-owned addresses |
| `identity.email_verification_tokens.user_id → identity.users.id` | CASCADE | Expiring auth artifacts |
| `identity.password_reset_tokens.user_id → identity.users.id` | CASCADE | Expiring auth artifacts |
| `identity.user_sessions.user_id → identity.users.id` | CASCADE | Revocable auth artifacts |
| `catalog.categories.parent_category_id → catalog.categories.id` | RESTRICT | Preserve hierarchy until children are reassigned |
| `catalog.products.primary_category_id → catalog.categories.id` | RESTRICT | Prevent deletion of a category still classifying products |
| `catalog.products.brand_id → catalog.brands.id` | SET NULL | Product remains; brand attribution is optional |
| `catalog.product_variants.product_id → catalog.products.id` | RESTRICT | Variants can be referenced by inventory/commercial records |
| `catalog.product_images.product_id → catalog.products.id` | CASCADE | Image metadata is product-owned, not commercial history |
| `catalog.category_attributes.category_id → catalog.categories.id` | CASCADE | Category assignment is disposable configuration |
| `catalog.category_attributes.attribute_id → catalog.attributes.id` | RESTRICT | Preserve definitions still assigned to categories |
| `catalog.variant_attribute_values.product_variant_id → catalog.product_variants.id` | CASCADE | Technical values are variant-owned |
| `catalog.variant_attribute_values.attribute_id → catalog.attributes.id` | RESTRICT | Preserve definitions used by recorded variant values |
| `inventory.inventory.product_variant_id → catalog.product_variants.id` | RESTRICT | Inventory cannot be removed while a variant owns a balance |
| `inventory.inventory_movements.inventory_id → inventory.inventory.id` | RESTRICT | Keep movement history anchored to its balance identity |
| `inventory.inventory_movements.supplier_id → inventory.suppliers.id` | RESTRICT | Supplier referenced by movement history cannot be removed; retire it with its inactive flag |
| `cart.carts.user_id → identity.users.id` | CASCADE | Cart is user-owned and disposable |
| `cart.cart_items.cart_id → cart.carts.id` | CASCADE | Lines are cart-owned and disposable |
| `cart.cart_items.product_variant_id → catalog.product_variants.id` | RESTRICT | Prevent variant removal while referenced by a live cart |
| `commercial.contact_requests.user_id → identity.users.id` | SET NULL | Retain request and contact snapshot after account removal |
| `commercial.contact_request_items.contact_request_id → commercial.contact_requests.id` | RESTRICT | Do not cascade away commercial request history |
| `commercial.contact_request_items.product_id → catalog.products.id` | SET NULL | Preserve product snapshot on the request item |
| `commercial.contact_request_items.product_variant_id → catalog.product_variants.id` | SET NULL | Preserve variant snapshot on the request item |
| `engagement.product_reviews.product_id → catalog.products.id` | RESTRICT | Keep review history attached to retained product identity |
| `engagement.product_reviews.user_id → identity.users.id` | SET NULL | Keep moderated review without retaining deleted account identity |
| `engagement.favorites.user_id → identity.users.id` | CASCADE | Disposable engagement association |
| `engagement.favorites.product_id → catalog.products.id` | CASCADE | Disposable engagement association |
| `promotions.coupon_usage.coupon_id → promotions.coupons.id` | RESTRICT | Preserve coupon usage history |
| `promotions.coupon_usage.user_id → identity.users.id` | SET NULL | Retain usage without retaining deleted user identity |
| `marketing.email_recipients.marketing_email_id → marketing.marketing_emails.id` | RESTRICT | Preserve campaign/recipient history |
| `marketing.email_recipients.user_id → identity.users.id` | SET NULL | Preserve recipient email snapshot without account identity |
| `platform.audit_logs.actor_user_id → identity.users.id` | SET NULL | Preserve audit event and actor context snapshot |
| `platform.file_attachments.user_id → identity.users.id` | CASCADE | Attachment metadata is owner-dependent |
| `platform.file_attachments.product_id → catalog.products.id` | CASCADE | Attachment metadata is owner-dependent |
| `platform.file_attachments.contact_request_id → commercial.contact_requests.id` | CASCADE | Attachment metadata is owner-dependent |
| `platform.file_attachments.marketing_email_id → marketing.marketing_emails.id` | CASCADE | Attachment metadata is owner-dependent |

`app_settings` has no FK relationships. Audit target descriptors are intentionally not treated as referential relationships; they may become stale after a target is removed and must not be used for cascading or authorization. SQL migrations must name all FK constraints and deletion actions explicitly, using stable prefixes `pk_`, `fk_`, `uq_`, `ck_`, and `ix_`. Index every FK column, including each nullable attachment owner column.
