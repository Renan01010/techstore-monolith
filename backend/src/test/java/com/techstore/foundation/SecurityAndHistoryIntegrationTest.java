package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class SecurityAndHistoryIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String MODULE_SCHEMAS =
            "('identity','catalog','inventory','cart','commercial','engagement','promotions','marketing','platform')";

    @Test
    void noColumnCanHoldASecretInPlainText() {
        assertThat(count("select count(*) from information_schema.columns where table_schema in " + MODULE_SCHEMAS
                + " and (column_name ~* '(^|_)(password|secret|api_key|credential|raw_token|session_token)($|_)'"
                + " or column_name in ('token', 'password', 'jwt'))"
                + " and column_name not in ('password_hash')")).isZero();
        assertThat(count("select count(*) from information_schema.columns where table_schema = 'identity'"
                + " and column_name in ('password_hash','token_digest','session_digest')")).isEqualTo(4);
    }

    @Test
    void historyRelationsNeverCascadeFromTheirParent() {
        assertThat(count("""
                select count(*) from pg_constraint
                where contype = 'f' and confdeltype = 'c'
                  and conname in ('fk_inventory_movements_inventory', 'fk_contact_request_items_request',
                                  'fk_coupon_usage_coupon', 'fk_email_recipients_marketing_email', 'fk_product_reviews_product')
                """)).isZero();
    }

    @Test
    void auditTargetIsNotAForeignKey() {
        assertThat(count("""
                select count(*) from pg_constraint con
                join pg_attribute a on a.attrelid = con.conrelid and a.attnum = any(con.conkey)
                where con.contype = 'f' and con.conrelid = 'platform.audit_logs'::regclass
                  and a.attname in ('target_type', 'target_id')
                """)).isZero();
        assertThat((String) scalar("select col_description('platform.audit_logs'::regclass, "
                + "(select attnum from pg_attribute where attrelid = 'platform.audit_logs'::regclass and attname = 'target_type'))"))
                .contains("polymorphic");
    }

    @Test
    void movementHistoryIsAppendOnlyAndCorrectedByCompensatingRows() {
        UUID inventory = insert("insert into inventory.inventory (product_variant_id, quantity_on_hand) values (?, 0) returning id", variant(product()));
        UUID original = insert("insert into inventory.inventory_movements (inventory_id, movement_type, quantity) values (?, 'receipt', 5) returning id", inventory);
        assertSqlState(RESTRICT, () -> update("update inventory.inventory_movements set quantity = 1 where id = ?", original));
        assertSqlState(RESTRICT, () -> update("delete from inventory.inventory_movements where id = ?", original));
        insert("insert into inventory.inventory_movements (inventory_id, movement_type, quantity) values (?, 'adjustment_decrease', 5) returning id", inventory);
        assertThat(count("select count(*) from inventory.inventory_movements where inventory_id = ?", inventory)).isEqualTo(2);
    }

    @Test
    void schemaContainsNoOrderOrPaymentTables() {
        assertThat(count("select count(*) from information_schema.tables where table_schema in " + MODULE_SCHEMAS
                + " and table_name ~* '(order|payment|checkout|invoice)'")).isZero();
    }
}
