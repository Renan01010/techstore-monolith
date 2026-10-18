package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CatalogSchemaIntegrationTest extends PostgresIntegrationTestSupport {

    @Test
    void rejectsDuplicateSlugsAndSku() {
        String slug = unique("slug");
        update("insert into catalog.categories (name, slug) values ('A', ?)", slug);
        assertSqlState(UNIQUE, () -> update("insert into catalog.categories (name, slug) values ('B', ?)", slug));
        update("insert into catalog.brands (name, slug) values ('A', ?)", slug);
        assertSqlState(UNIQUE, () -> update("insert into catalog.brands (name, slug) values ('B', ?)", slug));

        UUID product = product();
        String sku = unique("sku");
        update("insert into catalog.product_variants (product_id, sku, name, price) values (?, ?, 'V', 1)", product, sku);
        assertSqlState(UNIQUE, () -> update("insert into catalog.product_variants (product_id, sku, name, price) values (?, ?, 'V', 1)", product, sku));
    }

    @Test
    void supportsHierarchyButRejectsSelfParentAndDeletingAParent() {
        UUID parent = category();
        UUID child = insert("insert into catalog.categories (name, slug, parent_category_id) values ('Child', ?, ?) returning id",
                unique("c"), parent);
        assertSqlState(CHECK, () -> update("update catalog.categories set parent_category_id = id where id = ?", parent));
        assertSqlState(FOREIGN_KEY, () -> update("delete from catalog.categories where id = ?", parent));
        update("delete from catalog.categories where id = ?", child);
        update("delete from catalog.categories where id = ?", parent);
    }

    @Test
    void productRequiresPrimaryCategoryAndValidState() {
        assertSqlState(NOT_NULL, () -> update("insert into catalog.products (name, slug) values ('P', ?)", unique("p")));
        assertSqlState(FOREIGN_KEY, () -> update("insert into catalog.products (primary_category_id, name, slug) values (?, 'P', ?)",
                UUID.randomUUID(), unique("p")));
        assertSqlState(CHECK, () -> update("insert into catalog.products (primary_category_id, name, slug, publication_state) values (?, 'P', ?, 'live')",
                category(), unique("p")));
    }

    @Test
    void categoryInUseCannotBeDeletedAndBrandDeletionNullsTheReference() {
        UUID category = category();
        UUID brand = insert("insert into catalog.brands (name, slug) values ('B', ?) returning id", unique("b"));
        UUID product = insert("insert into catalog.products (primary_category_id, brand_id, name, slug) values (?, ?, 'P', ?) returning id",
                category, brand, unique("p"));
        assertSqlState(FOREIGN_KEY, () -> update("delete from catalog.categories where id = ?", category));
        update("delete from catalog.brands where id = ?", brand);
        assertThat(scalar("select brand_id from catalog.products where id = ?", product)).isNull();
    }

    @Test
    void variantPriceCurrencyDimensionsAndStateAreValidated() {
        UUID product = product();
        String base = "insert into catalog.product_variants (product_id, sku, name, price, currency, length_mm, weight_kg, variant_state) "
                + "values (?, ?, 'V', ?, ?, ?, ?, ?)";
        update(base, product, unique("ok"), 0, "BRL", 10, 1, "active");
        assertSqlState(CHECK, () -> update(base, product, unique("neg"), -1, "BRL", 10, 1, "active"));
        assertSqlState(CHECK, () -> update(base, product, unique("cur"), 1, "brl", 10, 1, "active"));
        assertSqlState(CHECK, () -> update(base, product, unique("len"), 1, "BRL", -1, 1, "active"));
        assertSqlState(CHECK, () -> update(base, product, unique("wgt"), 1, "BRL", 10, -1, "active"));
        assertSqlState(CHECK, () -> update(base, product, unique("st"), 1, "BRL", 10, 1, "retired"));
        assertSqlState(FOREIGN_KEY, () -> update(base, UUID.randomUUID(), unique("fk"), 1, "BRL", 10, 1, "active"));
    }

    @Test
    void imagesHaveUniqueOrderAndAtMostOnePrimary() {
        UUID product = product();
        String insert = "insert into catalog.product_images (product_id, storage_reference, sort_order, is_primary) values (?, 'ref', ?, ?)";
        update(insert, product, 0, true);
        update(insert, product, 1, false);
        assertSqlState(UNIQUE, () -> update(insert, product, 1, false));
        assertSqlState(UNIQUE, () -> update(insert, product, 2, true));
        assertSqlState(CHECK, () -> update(insert, product, -1, false));
        assertSqlState(FOREIGN_KEY, () -> update(insert, UUID.randomUUID(), 0, false));
        update(insert, product(), 0, true);
    }

    @Test
    void productWithVariantsCannotBeDeletedButImagesCascade() {
        UUID withVariant = product();
        variant(withVariant);
        assertSqlState(FOREIGN_KEY, () -> update("delete from catalog.products where id = ?", withVariant));

        UUID plain = product();
        update("insert into catalog.product_images (product_id, storage_reference, sort_order) values (?, 'ref', 0)", plain);
        update("delete from catalog.products where id = ?", plain);
        assertThat(count("select count(*) from catalog.product_images where product_id = ?", plain)).isZero();
    }

    @Test
    void attributeTypeAndAssignmentAreValidated() {
        assertSqlState(CHECK, () -> update("insert into catalog.attributes (name, code, value_type) values ('A', ?, 'date')", unique("a")));
        String code = unique("code");
        update("insert into catalog.attributes (name, code, value_type) values ('A', ?, 'text')", code);
        assertSqlState(UNIQUE, () -> update("insert into catalog.attributes (name, code, value_type) values ('B', ?, 'text')", code));

        UUID category = category();
        UUID attribute = attribute("text");
        update("insert into catalog.category_attributes (category_id, attribute_id) values (?, ?)", category, attribute);
        assertSqlState(UNIQUE, () -> update("insert into catalog.category_attributes (category_id, attribute_id) values (?, ?)", category, attribute));
        assertSqlState(FOREIGN_KEY, () -> update("delete from catalog.attributes where id = ?", attribute));
        update("delete from catalog.categories where id = ?", category);
        update("delete from catalog.attributes where id = ?", attribute);
    }
}
