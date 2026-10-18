package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

class DatabaseSchemaIntegrationTest extends PostgresIntegrationTestSupport {

    private static final Map<String, List<String>> TABLES = Map.of(
            "identity", List.of("users", "user_addresses", "email_verification_tokens", "password_reset_tokens", "user_sessions"),
            "catalog", List.of("categories", "brands", "products", "product_variants", "product_images", "attributes",
                    "category_attributes", "variant_attribute_values"),
            "inventory", List.of("suppliers", "inventory", "inventory_movements"),
            "cart", List.of("carts", "cart_items"),
            "commercial", List.of("contact_requests", "contact_request_items"),
            "engagement", List.of("product_reviews", "favorites"),
            "promotions", List.of("coupons", "coupon_usage"),
            "marketing", List.of("newsletter_subscriptions", "marketing_emails", "email_recipients"),
            "platform", List.of("audit_logs", "app_settings", "file_attachments"));

    /** Expected ON DELETE action (confdeltype) per foreign key, from the data-model matrix. */
    private static final Map<String, String> ON_DELETE = Map.ofEntries(
            Map.entry("identity.fk_user_addresses_user", "c"),
            Map.entry("identity.fk_email_verification_tokens_user", "c"),
            Map.entry("identity.fk_password_reset_tokens_user", "c"),
            Map.entry("identity.fk_user_sessions_user", "c"),
            Map.entry("catalog.fk_categories_parent", "r"),
            Map.entry("catalog.fk_products_primary_category", "r"),
            Map.entry("catalog.fk_products_brand", "n"),
            Map.entry("catalog.fk_product_variants_product", "r"),
            Map.entry("catalog.fk_product_images_product", "c"),
            Map.entry("catalog.fk_category_attributes_category", "c"),
            Map.entry("catalog.fk_category_attributes_attribute", "r"),
            Map.entry("catalog.fk_variant_attribute_values_variant", "c"),
            Map.entry("catalog.fk_variant_attribute_values_attribute", "r"),
            Map.entry("inventory.fk_inventory_product_variant", "r"),
            Map.entry("inventory.fk_inventory_movements_inventory", "r"),
            Map.entry("inventory.fk_inventory_movements_supplier", "r"),
            Map.entry("cart.fk_carts_user", "c"),
            Map.entry("cart.fk_cart_items_cart", "c"),
            Map.entry("cart.fk_cart_items_product_variant", "r"),
            Map.entry("commercial.fk_contact_requests_user", "n"),
            Map.entry("commercial.fk_contact_request_items_request", "r"),
            Map.entry("commercial.fk_contact_request_items_product", "n"),
            Map.entry("commercial.fk_contact_request_items_variant", "n"),
            Map.entry("engagement.fk_product_reviews_product", "r"),
            Map.entry("engagement.fk_product_reviews_user", "n"),
            Map.entry("engagement.fk_favorites_user", "c"),
            Map.entry("engagement.fk_favorites_product", "c"),
            Map.entry("promotions.fk_coupon_usage_coupon", "r"),
            Map.entry("promotions.fk_coupon_usage_user", "n"),
            Map.entry("marketing.fk_email_recipients_marketing_email", "r"),
            Map.entry("marketing.fk_email_recipients_user", "n"),
            Map.entry("platform.fk_audit_logs_actor_user", "n"),
            Map.entry("platform.fk_file_attachments_user", "c"),
            Map.entry("platform.fk_file_attachments_product", "c"),
            Map.entry("platform.fk_file_attachments_contact_request", "c"),
            Map.entry("platform.fk_file_attachments_marketing_email", "c"));

    @Test
    void appliesAllMigrationsOnEmptyDatabaseWithoutFailures() {
        assertThat(count("select count(*) from public.flyway_schema_history where success = false")).isZero();
        assertThat(count("select count(*) from public.flyway_schema_history where version is not null")).isEqualTo(12);
    }

    @Test
    void createsExactlyTheThirtyExpectedTablesInTheirOwningSchemas() throws Exception {
        int total = 0;
        for (Map.Entry<String, List<String>> entry : TABLES.entrySet()) {
            Set<String> actual = new TreeSet<>(names(
                    "select table_name from information_schema.tables where table_schema = '" + entry.getKey()
                            + "' and table_type = 'BASE TABLE'"));
            assertThat(actual).as(entry.getKey()).containsExactlyInAnyOrderElementsOf(entry.getValue());
            total += actual.size();
        }
        assertThat(total).isEqualTo(30);
    }

    @Test
    void everyTableHasAUuidPrimaryKeyWithPkPrefix() {
        assertThat(count("""
                select count(*) from information_schema.tables t
                where t.table_schema in ('identity','catalog','inventory','cart','commercial','engagement','promotions','marketing','platform')
                  and not exists (
                    select 1 from pg_constraint c
                    join pg_class r on r.oid = c.conrelid
                    join pg_namespace n on n.oid = r.relnamespace
                    join pg_attribute a on a.attrelid = r.oid and a.attnum = c.conkey[1]
                    where c.contype = 'p' and n.nspname = t.table_schema and r.relname = t.table_name
                      and array_length(c.conkey, 1) = 1 and a.attname = 'id' and a.atttypid = 'uuid'::regtype
                      and c.conname like 'pk\\_%')
                """)).isZero();
    }

    @Test
    void everyForeignKeyHasTheDocumentedOnDeleteRuleAndASupportingIndex() throws Exception {
        List<String> actual = new ArrayList<>();
        try (Connection c = connection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery("""
                select n.nspname || '.' || con.conname, con.confdeltype
                from pg_constraint con join pg_namespace n on n.oid = con.connamespace
                where con.contype = 'f' and n.nspname in ('identity','catalog','inventory','cart','commercial','engagement','promotions','marketing','platform')
                """)) {
            while (r.next()) {
                actual.add(r.getString(1) + "=" + r.getString(2));
            }
        }
        assertThat(actual).hasSize(ON_DELETE.size());
        ON_DELETE.forEach((fk, action) -> assertThat(actual).contains(fk + "=" + action));

        assertThat(count("""
                select count(*) from pg_constraint con
                where con.contype = 'f' and not exists (
                    select 1 from pg_index i
                    where i.indrelid = con.conrelid and i.indkey[0] = con.conkey[1])
                  and con.connamespace in (select oid from pg_namespace
                    where nspname in ('identity','catalog','inventory','cart','commercial','engagement','promotions','marketing','platform'))
                """)).as("foreign keys without a leading-column index").isZero();
    }

    @Test
    void constraintNamesUseTheStablePrefixes() {
        assertThat(count("""
                select count(*) from pg_constraint con join pg_namespace n on n.oid = con.connamespace
                where n.nspname in ('identity','catalog','inventory','cart','commercial','engagement','promotions','marketing','platform')
                  and con.contype in ('p','f','u','c')
                  and con.conname !~ '^(pk|fk|uq|ck)_'
                """)).isZero();
    }

    @Test
    void usesTimestamptzAndNumericAndStoresNoBinaryColumns() {
        assertThat(count("""
                select count(*) from information_schema.columns
                where table_schema in ('identity','catalog','inventory','cart','commercial','engagement','promotions','marketing','platform')
                  and (data_type = 'timestamp without time zone' or data_type = 'bytea' or data_type in ('real','double precision','money'))
                """)).isZero();
        assertThat(count("""
                select count(*) from information_schema.columns
                where table_schema in ('identity','catalog','inventory','cart','commercial','engagement','promotions','marketing','platform')
                  and column_name in ('price','unit_price_snapshot','benefit_value') and data_type <> 'numeric'
                """)).isZero();
    }

    @Test
    void repeatingInitializationIsASafeNoOp() {
        long historyBefore = count("select count(*) from public.flyway_schema_history");
        long tablesBefore = count("select count(*) from information_schema.tables where table_schema not in ('pg_catalog','information_schema')");

        migrator().migrate();
        migrator().migrate();

        assertThat(count("select count(*) from public.flyway_schema_history")).isEqualTo(historyBefore);
        assertThat(count("select count(*) from information_schema.tables where table_schema not in ('pg_catalog','information_schema')"))
                .isEqualTo(tablesBefore);
        migrator().validate();
    }

    @Test
    void usesOnlyTheTestcontainersDatabase() {
        assertThat(postgres.getJdbcUrl()).startsWith("jdbc:postgresql://");
        assertThat(postgres.isRunning()).isTrue();
        assertThat(System.getProperty("spring.datasource.url")).isNull();
    }

    private static List<String> names(String sql) throws Exception {
        List<String> out = new ArrayList<>();
        try (Connection c = connection(); Statement s = c.createStatement(); ResultSet r = s.executeQuery(sql)) {
            while (r.next()) {
                out.add(r.getString(1));
            }
        }
        return out;
    }
}
