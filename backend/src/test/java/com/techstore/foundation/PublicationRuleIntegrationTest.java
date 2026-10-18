package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class PublicationRuleIntegrationTest extends PostgresIntegrationTestSupport {

    private static void run(Connection c, String sql, Object... args) throws SQLException {
        try (var s = prepare(c, sql, args)) {
            s.executeUpdate();
        }
    }

    @Test
    void draftProductWithoutVariantsIsAllowed() {
        product();
    }

    @Test
    void publishingWithoutVariantsFailsAtCommit() {
        UUID product = product();
        assertSqlState(CHECK, () -> update("update catalog.products set publication_state = 'published' where id = ?", product));
        assertThat(scalar("select publication_state from catalog.products where id = ?", product)).isEqualTo("draft");
        assertSqlState(CHECK, () -> update(
                "insert into catalog.products (primary_category_id, name, slug, publication_state) values (?, 'P', ?, 'published')",
                category(), unique("p")));
    }

    @Test
    void publishingWithOnlyInactiveVariantsFails() {
        UUID product = product();
        update("insert into catalog.product_variants (product_id, sku, name, price, variant_state) values (?, ?, 'V', 1, 'inactive')",
                product, unique("sku"));
        assertSqlState(CHECK, () -> update("update catalog.products set publication_state = 'published' where id = ?", product));
    }

    @Test
    void publishingWithAnActiveVariantSucceeds() {
        UUID product = product();
        variant(product);
        update("update catalog.products set publication_state = 'published' where id = ?", product);
        assertThat(scalar("select publication_state from catalog.products where id = ?", product)).isEqualTo("published");
    }

    @Test
    void variantAndPublicationCanBeCreatedInOneTransaction() {
        UUID category = category();
        inTransaction(c -> {
            UUID product = UUID.randomUUID();
            run(c, "insert into catalog.products (id, primary_category_id, name, slug) values (?, ?, 'P', ?)", product, category, unique("p"));
            run(c, "insert into catalog.product_variants (product_id, sku, name, price) values (?, ?, 'V', 1)", product, unique("sku"));
            run(c, "update catalog.products set publication_state = 'published' where id = ?", product);
        });
    }

    @Test
    void deactivatingOrDeletingTheLastActiveVariantOfAPublishedProductFails() {
        UUID product = product();
        UUID variant = variant(product);
        update("update catalog.products set publication_state = 'published' where id = ?", product);

        assertSqlState(CHECK, () -> update("update catalog.product_variants set variant_state = 'inactive' where id = ?", variant));
        assertSqlState(CHECK, () -> update("delete from catalog.product_variants where id = ?", variant));

        UUID second = variant(product);
        update("update catalog.product_variants set variant_state = 'inactive' where id = ?", variant);
        assertThat(scalar("select variant_state from catalog.product_variants where id = ?", second)).isEqualTo("active");
    }

    @Test
    void archivingAPublishedProductIsAllowed() {
        UUID product = product();
        variant(product);
        update("update catalog.products set publication_state = 'published' where id = ?", product);
        update("update catalog.products set publication_state = 'archived' where id = ?", product);
    }
}
