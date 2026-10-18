package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CatalogAttributeRulesIntegrationTest extends PostgresIntegrationTestSupport {

    private static void assign(UUID category, UUID attribute) {
        update("insert into catalog.category_attributes (category_id, attribute_id) values (?, ?)", category, attribute);
    }

    @Test
    void acceptsTypedValueForAttributeAssignedToThePrimaryCategory() {
        UUID category = category();
        UUID variant = variant(product(category));
        UUID text = attribute("text");
        UUID number = attribute("numeric");
        UUID flag = attribute("boolean");
        assign(category, text);
        assign(category, number);
        assign(category, flag);
        update("insert into catalog.variant_attribute_values (product_variant_id, attribute_id, value_text) values (?, ?, 'oak')", variant, text);
        update("insert into catalog.variant_attribute_values (product_variant_id, attribute_id, value_numeric) values (?, ?, 12.5)", variant, number);
        update("insert into catalog.variant_attribute_values (product_variant_id, attribute_id, value_boolean) values (?, ?, false)", variant, flag);
    }

    @Test
    void rejectsValueWhenAttributeIsNotAssignedToTheCategory() {
        UUID variant = variant(product());
        UUID attribute = attribute("text");
        assertSqlState(CHECK, () -> update(
                "insert into catalog.variant_attribute_values (product_variant_id, attribute_id, value_text) values (?, ?, 'x')", variant, attribute));
    }

    @Test
    void rejectsValueOfTheWrongTypeOrWithZeroOrManyValues() {
        UUID category = category();
        UUID variant = variant(product(category));
        UUID text = attribute("text");
        assign(category, text);
        assertSqlState(CHECK, () -> update(
                "insert into catalog.variant_attribute_values (product_variant_id, attribute_id, value_numeric) values (?, ?, 1)", variant, text));
        assertSqlState(CHECK, () -> update(
                "insert into catalog.variant_attribute_values (product_variant_id, attribute_id) values (?, ?)", variant, text));
        assertSqlState(CHECK, () -> update(
                "insert into catalog.variant_attribute_values (product_variant_id, attribute_id, value_text, value_numeric) values (?, ?, 'a', 1)",
                variant, text));
    }

    @Test
    void rejectsDuplicateValueForTheSameVariantAndAttribute() {
        UUID category = category();
        UUID variant = variant(product(category));
        UUID text = attribute("text");
        assign(category, text);
        String insert = "insert into catalog.variant_attribute_values (product_variant_id, attribute_id, value_text) values (?, ?, 'a')";
        update(insert, variant, text);
        assertSqlState(UNIQUE, () -> update(insert, variant, text));
    }

    @Test
    void attributeCanBeReusedByTwoCategories() {
        UUID attribute = attribute("text");
        for (int i = 0; i < 2; i++) {
            UUID category = category();
            assign(category, attribute);
            UUID variant = variant(product(category));
            update("insert into catalog.variant_attribute_values (product_variant_id, attribute_id, value_text) values (?, ?, 'x')", variant, attribute);
        }
    }

    @Test
    void blocksCategoryChangeAndAssignmentRemovalThatWouldInvalidateValues() {
        UUID category = category();
        UUID other = category();
        UUID product = product(category);
        UUID variant = variant(product);
        UUID attribute = attribute("text");
        assign(category, attribute);
        update("insert into catalog.variant_attribute_values (product_variant_id, attribute_id, value_text) values (?, ?, 'x')", variant, attribute);

        assertSqlState(CHECK, () -> update("update catalog.products set primary_category_id = ? where id = ?", other, product));
        assertSqlState(RESTRICT, () -> update("delete from catalog.category_attributes where category_id = ? and attribute_id = ?", category, attribute));
        assertSqlState(RESTRICT, () -> update("update catalog.attributes set value_type = 'numeric' where id = ?", attribute));

        assign(other, attribute);
        update("update catalog.products set primary_category_id = ? where id = ?", other, product);
        assertThat(scalar("select primary_category_id from catalog.products where id = ?", product)).isEqualTo(other);
    }

    @Test
    void allowsChangesWhenNoValuesDependOnTheAssignment() {
        UUID category = category();
        UUID attribute = attribute("text");
        assign(category, attribute);
        update("delete from catalog.category_attributes where category_id = ? and attribute_id = ?", category, attribute);
        UUID product = product(category);
        update("update catalog.products set primary_category_id = ? where id = ?", category(), product);
    }

    @Test
    void variantValuesCascadeWithTheVariantAndAttributeInUseCannotBeDeleted() {
        UUID category = category();
        UUID product = product(category);
        UUID variant = variant(product);
        UUID attribute = attribute("text");
        assign(category, attribute);
        update("insert into catalog.variant_attribute_values (product_variant_id, attribute_id, value_text) values (?, ?, 'x')", variant, attribute);
        assertSqlState(FOREIGN_KEY, () -> update("delete from catalog.attributes where id = ?", attribute));
        update("delete from catalog.product_variants where id = ?", variant);
        assertThat(count("select count(*) from catalog.variant_attribute_values where attribute_id = ?", attribute)).isZero();
    }
}
