package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class InventoryCartSchemaIntegrationTest extends PostgresIntegrationTestSupport {

    private static UUID inventory(UUID variant, int quantity) {
        return insert("insert into inventory.inventory (product_variant_id, quantity_on_hand) values (?, ?) returning id", variant, quantity);
    }

    private static UUID movement(UUID inventory, UUID supplier, String type, int quantity) {
        return insert("insert into inventory.inventory_movements (inventory_id, supplier_id, movement_type, quantity) values (?, ?, ?, ?) returning id",
                inventory, supplier, type, quantity);
    }

    private static UUID supplier() {
        return insert("insert into inventory.suppliers (name) values ('S') returning id");
    }

    @Test
    void oneNonNegativeBalancePerVariant() {
        UUID variant = variant(product());
        inventory(variant, 5);
        assertSqlState(UNIQUE, () -> inventory(variant, 1));
        assertSqlState(CHECK, () -> inventory(variant(product()), -1));
        assertSqlState(FOREIGN_KEY, () -> inventory(UUID.randomUUID(), 0));
    }

    @Test
    void movementsRequirePositiveQuantityValidTypeAndExistingInventory() {
        UUID inventory = inventory(variant(product()), 0);
        for (String type : new String[] {"receipt", "removal", "adjustment_increase", "adjustment_decrease"}) {
            movement(inventory, null, type, 1);
        }
        assertSqlState(CHECK, () -> movement(inventory, null, "receipt", 0));
        assertSqlState(CHECK, () -> movement(inventory, null, "receipt", -3));
        assertSqlState(CHECK, () -> movement(inventory, null, "sale", 1));
        assertSqlState(FOREIGN_KEY, () -> movement(UUID.randomUUID(), null, "receipt", 1));
        assertSqlState(FOREIGN_KEY, () -> movement(inventory, UUID.randomUUID(), "receipt", 1));
    }

    @Test
    void historyBlocksDeletingInventorySupplierAndVariant() {
        UUID variant = variant(product());
        UUID inventory = inventory(variant, 0);
        UUID supplier = supplier();
        movement(inventory, supplier, "receipt", 1);

        assertSqlState(FOREIGN_KEY, () -> update("delete from inventory.inventory where id = ?", inventory));
        assertSqlState(FOREIGN_KEY, () -> update("delete from inventory.suppliers where id = ?", supplier));
        assertSqlState(FOREIGN_KEY, () -> update("delete from catalog.product_variants where id = ?", variant));
    }

    @Test
    void movementsCannotBeUpdatedOrDeleted() {
        UUID movement = movement(inventory(variant(product()), 0), null, "receipt", 2);
        assertSqlState(RESTRICT, () -> update("update inventory.inventory_movements set quantity = 9 where id = ?", movement));
        assertSqlState(RESTRICT, () -> update("delete from inventory.inventory_movements where id = ?", movement));
        assertSqlState(RESTRICT, () -> update("truncate inventory.inventory_movements"));
        assertThat(scalar("select quantity from inventory.inventory_movements where id = ?", movement)).isEqualTo(2);
    }

    @Test
    void cartItemQuantityAndUniquenessAreEnforced() {
        UUID cart = insert("insert into cart.carts (user_id) values (?) returning id", user());
        UUID variant = variant(product());
        String insert = "insert into cart.cart_items (cart_id, product_variant_id, quantity) values (?, ?, ?)";
        update(insert, cart, variant, 1);
        assertSqlState(UNIQUE, () -> update(insert, cart, variant, 2));
        assertSqlState(CHECK, () -> update(insert, cart, variant(product()), 0));
        assertSqlState(CHECK, () -> update(insert, cart, variant(product()), -1));
        assertSqlState(FOREIGN_KEY, () -> update(insert, cart, UUID.randomUUID(), 1));
        assertSqlState(FOREIGN_KEY, () -> update(insert, UUID.randomUUID(), variant(product()), 1));
        assertSqlState(FOREIGN_KEY, () -> update("insert into cart.carts (user_id) values (?)", UUID.randomUUID()));
    }

    @Test
    void priceSnapshotDistinguishesUnknownKnownAndZero() {
        UUID cart = insert("insert into cart.carts (user_id) values (?) returning id", user());
        String insert = "insert into cart.cart_items (cart_id, product_variant_id, quantity, unit_price_snapshot, price_currency) values (?, ?, 1, ?, ?)";
        update(insert, cart, variant(product()), null, null);
        UUID zero = insert("insert into cart.cart_items (cart_id, product_variant_id, quantity, unit_price_snapshot, price_currency) "
                + "values (?, ?, 1, 0, 'BRL') returning id", cart, variant(product()));
        update(insert, cart, variant(product()), new java.math.BigDecimal("19.90"), "BRL");

        assertThat(scalar("select unit_price_snapshot is null from cart.cart_items where id = ?", zero)).isEqualTo(false);
        assertSqlState(CHECK, () -> update(insert, cart, variant(product()), null, "BRL"));
        assertSqlState(CHECK, () -> update(insert, cart, variant(product()), new java.math.BigDecimal("1.00"), null));
        assertSqlState(CHECK, () -> update(insert, cart, variant(product()), new java.math.BigDecimal("-0.01"), "BRL"));
        assertSqlState(CHECK, () -> update(insert, cart, variant(product()), new java.math.BigDecimal("1.00"), "brl"));
    }

    @Test
    void deletingACartOrUserCascadesButVariantInCartIsProtected() {
        UUID user = user();
        UUID cart = insert("insert into cart.carts (user_id) values (?) returning id", user);
        UUID variant = variant(product());
        update("insert into cart.cart_items (cart_id, product_variant_id, quantity) values (?, ?, 1)", cart, variant);

        assertSqlState(FOREIGN_KEY, () -> update("delete from catalog.product_variants where id = ?", variant));
        update("delete from identity.users where id = ?", user);
        assertThat(count("select count(*) from cart.carts where id = ?", cart)).isZero();
        assertThat(count("select count(*) from cart.cart_items where cart_id = ?", cart)).isZero();
    }
}
