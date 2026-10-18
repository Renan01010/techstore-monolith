package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class PlatformSchemaIntegrationTest extends PostgresIntegrationTestSupport {

    private static final String ATTACHMENT = "insert into platform.file_attachments "
            + "(user_id, product_id, contact_request_id, marketing_email_id, file_name, content_type, size_bytes, storage_reference) "
            + "values (?, ?, ?, ?, 'f.pdf', 'application/pdf', 10, 'bucket/key') returning id";

    @Test
    void attachmentRequiresExactlyOneOwner() {
        assertSqlState(CHECK, () -> insert(ATTACHMENT, null, null, null, null));
        assertSqlState(CHECK, () -> insert(ATTACHMENT, user(), product(), null, null));
        assertSqlState(CHECK, () -> insert(ATTACHMENT, user(), product(), contactRequest(), campaign()));
    }

    @Test
    void attachmentAcceptsEachSupportedOwnerType() {
        insert(ATTACHMENT, user(), null, null, null);
        insert(ATTACHMENT, null, product(), null, null);
        insert(ATTACHMENT, null, null, contactRequest(), null);
        insert(ATTACHMENT, null, null, null, campaign());
    }

    @Test
    void attachmentOwnerMustExist() {
        assertSqlState(FOREIGN_KEY, () -> insert(ATTACHMENT, UUID.randomUUID(), null, null, null));
        assertSqlState(FOREIGN_KEY, () -> insert(ATTACHMENT, null, UUID.randomUUID(), null, null));
        assertSqlState(FOREIGN_KEY, () -> insert(ATTACHMENT, null, null, UUID.randomUUID(), null));
        assertSqlState(FOREIGN_KEY, () -> insert(ATTACHMENT, null, null, null, UUID.randomUUID()));
    }

    @Test
    void deletingAnOwnerCascadesItsAttachments() {
        UUID user = user();
        UUID product = product();
        UUID request = contactRequest();
        UUID campaign = campaign();
        UUID a1 = insert(ATTACHMENT, user, null, null, null);
        UUID a2 = insert(ATTACHMENT, null, product, null, null);
        UUID a3 = insert(ATTACHMENT, null, null, request, null);
        UUID a4 = insert(ATTACHMENT, null, null, null, campaign);

        update("delete from identity.users where id = ?", user);
        update("delete from catalog.products where id = ?", product);
        update("delete from commercial.contact_requests where id = ?", request);
        update("delete from marketing.marketing_emails where id = ?", campaign);

        for (UUID id : new UUID[] {a1, a2, a3, a4}) {
            assertThat(count("select count(*) from platform.file_attachments where id = ?", id)).isZero();
        }
    }

    @Test
    void attachmentSizeCannotBeNegative() {
        assertSqlState(CHECK, () -> update("insert into platform.file_attachments (user_id, file_name, content_type, size_bytes, storage_reference) "
                + "values (?, 'f', 't', -1, 'r')", user()));
    }

    @Test
    void settingKeyIsUniqueAndVisibilityValidated() {
        String key = unique("setting");
        update("insert into platform.app_settings (setting_key, setting_value) values (?, 'v')", key);
        assertSqlState(UNIQUE, () -> update("insert into platform.app_settings (setting_key, setting_value) values (?, 'v')", key));
        for (String visibility : new String[] {"public", "internal", "secret"}) {
            update("insert into platform.app_settings (setting_key, visibility) values (?, ?)", unique("k"), visibility);
        }
        assertSqlState(CHECK, () -> update("insert into platform.app_settings (setting_key, visibility) values (?, 'hidden')", unique("k")));
        assertSqlState(NOT_NULL, () -> update("insert into platform.app_settings (setting_value) values ('v')"));
    }

    @Test
    void auditLogSurvivesActorRemovalAndAcceptsAnyDescriptiveTarget() {
        UUID actor = user();
        UUID log = insert("insert into platform.audit_logs (actor_user_id, action, target_type, target_id, details) "
                + "values (?, 'product.updated', 'Product', 'not-a-uuid-and-no-fk', '{\"field\":\"name\"}'::jsonb) returning id", actor);
        update("delete from identity.users where id = ?", actor);
        assertThat(scalar("select actor_user_id from platform.audit_logs where id = ?", log)).isNull();
        assertThat(scalar("select target_id from platform.audit_logs where id = ?", log)).isEqualTo("not-a-uuid-and-no-fk");
        assertSqlState(NOT_NULL, () -> update("insert into platform.audit_logs (details) values ('{}'::jsonb)"));
        assertSqlState(CHECK, () -> update("insert into platform.audit_logs (action, target_type) values ('a', 'Product')"));
    }
}
