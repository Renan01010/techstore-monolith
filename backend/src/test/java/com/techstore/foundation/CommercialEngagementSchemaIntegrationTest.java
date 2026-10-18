package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class CommercialEngagementSchemaIntegrationTest extends PostgresIntegrationTestSupport {

    private static UUID request(UUID user) {
        return insert("insert into commercial.contact_requests (user_id, contact_name, contact_email) values (?, 'Name', 'n@example.com') returning id", user);
    }

    private static UUID item(UUID request, UUID product, UUID variant) {
        return insert("insert into commercial.contact_request_items (contact_request_id, product_id, product_variant_id, item_description, sku_snapshot, quantity) "
                + "values (?, ?, ?, 'Desk', 'SKU-1', 2) returning id", request, product, variant);
    }

    @Test
    void contactRequestStatesAreValidated() {
        for (String state : new String[] {"received", "in_progress", "completed", "cancelled"}) {
            update("insert into commercial.contact_requests (contact_name, contact_email, request_state) values ('N', 'n@example.com', ?)", state);
        }
        assertSqlState(CHECK, () -> update(
                "insert into commercial.contact_requests (contact_name, contact_email, request_state) values ('N', 'n@example.com', 'paid')"));
        assertSqlState(NOT_NULL, () -> update("insert into commercial.contact_requests (contact_email) values ('n@example.com')"));
        assertSqlState(FOREIGN_KEY, () -> request(UUID.randomUUID()));
    }

    @Test
    void deletingTheUserKeepsTheRequestAndItsSnapshot() {
        UUID user = user();
        UUID request = request(user);
        update("delete from identity.users where id = ?", user);
        assertThat(scalar("select user_id from commercial.contact_requests where id = ?", request)).isNull();
        assertThat(scalar("select contact_email from commercial.contact_requests where id = ?", request)).isEqualTo("n@example.com");
    }

    @Test
    void requestWithItemsCannotBeDeletedAndItemsKeepSnapshotWhenCatalogIsRemoved() {
        UUID request = request(null);
        UUID product = product();
        UUID variant = variant(product);
        UUID item = item(request, product, variant);

        assertSqlState(FOREIGN_KEY, () -> update("delete from commercial.contact_requests where id = ?", request));
        assertSqlState(CHECK, () -> update(
                "insert into commercial.contact_request_items (contact_request_id, item_description, quantity) values (?, 'x', 0)", request));
        assertSqlState(FOREIGN_KEY, () -> item(UUID.randomUUID(), null, null));

        update("delete from catalog.product_variants where id = ?", variant);
        update("delete from catalog.products where id = ?", product);
        assertThat(scalar("select product_variant_id from commercial.contact_request_items where id = ?", item)).isNull();
        assertThat(scalar("select product_id from commercial.contact_request_items where id = ?", item)).isNull();
        assertThat(scalar("select sku_snapshot from commercial.contact_request_items where id = ?", item)).isEqualTo("SKU-1");
    }

    @Test
    void ratingAndModerationStateAreValidated() {
        UUID product = product();
        UUID user = user();
        String insert = "insert into engagement.product_reviews (product_id, user_id, rating, moderation_state) values (?, ?, ?, ?)";
        update(insert, product, user, 1, "pending");
        update(insert, product, user, 5, "approved");
        update(insert, product, user, 3, "rejected");
        assertSqlState(CHECK, () -> update(insert, product, user, 0, "pending"));
        assertSqlState(CHECK, () -> update(insert, product, user, 6, "pending"));
        assertSqlState(CHECK, () -> update(insert, product, user, 3, "spam"));
        assertSqlState(NOT_NULL, () -> update("insert into engagement.product_reviews (user_id, rating) values (?, 3)", user));
        assertSqlState(FOREIGN_KEY, () -> update(insert, UUID.randomUUID(), user, 3, "pending"));
    }

    @Test
    void reviewsProtectTheProductAndSurviveUserRemoval() {
        UUID product = product();
        UUID user = user();
        UUID review = insert("insert into engagement.product_reviews (product_id, user_id, rating) values (?, ?, 4) returning id", product, user);
        assertSqlState(FOREIGN_KEY, () -> update("delete from catalog.products where id = ?", product));
        update("delete from identity.users where id = ?", user);
        assertThat(scalar("select user_id from engagement.product_reviews where id = ?", review)).isNull();
        assertThat(scalar("select rating from engagement.product_reviews where id = ?", review)).isEqualTo(4);
    }

    @Test
    void favoritesAreUniqueAndCascadeFromUserAndProduct() {
        UUID user = user();
        UUID product = product();
        String insert = "insert into engagement.favorites (user_id, product_id) values (?, ?)";
        update(insert, user, product);
        assertSqlState(UNIQUE, () -> update(insert, user, product));
        assertSqlState(FOREIGN_KEY, () -> update(insert, UUID.randomUUID(), product));

        update("delete from identity.users where id = ?", user);
        assertThat(count("select count(*) from engagement.favorites where product_id = ?", product)).isZero();

        UUID user2 = user();
        update(insert, user2, product);
        update("delete from catalog.products where id = ?", product);
        assertThat(count("select count(*) from engagement.favorites where user_id = ?", user2)).isZero();
    }
}
