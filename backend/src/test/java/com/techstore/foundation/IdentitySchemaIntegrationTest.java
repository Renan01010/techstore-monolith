package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class IdentitySchemaIntegrationTest extends PostgresIntegrationTestSupport {

    @Test
    void rejectsDuplicateAndNonNormalizedEmail() {
        String email = unique("dup") + "@example.com";
        update("insert into identity.users (email, password_hash, full_name) values (?, 'h', 'A')", email);
        assertSqlState(UNIQUE, () -> update("insert into identity.users (email, password_hash, full_name) values (?, 'h', 'B')", email));
        assertSqlState(CHECK, () -> update("insert into identity.users (email, password_hash, full_name) values (?, 'h', 'B')",
                email.toUpperCase()));
    }

    @Test
    void rejectsInvalidStateRoleAndMissingPasswordHash() {
        assertSqlState(CHECK, () -> update(
                "insert into identity.users (email, password_hash, full_name, account_state) values (?, 'h', 'A', 'bogus')",
                unique("s") + "@example.com"));
        assertSqlState(CHECK, () -> update(
                "insert into identity.users (email, password_hash, full_name, access_role) values (?, 'h', 'A', 'root')",
                unique("r") + "@example.com"));
        assertSqlState(NOT_NULL, () -> update("insert into identity.users (email, full_name) values (?, 'A')", unique("n") + "@example.com"));
    }

    @Test
    void acceptsEveryApprovedStateAndRole() {
        for (String state : new String[] {"pending_verification", "active", "suspended", "deactivated"}) {
            update("insert into identity.users (email, password_hash, full_name, account_state) values (?, 'h', 'A', ?)",
                    unique("ok") + "@example.com", state);
        }
        for (String role : new String[] {"customer", "administrator"}) {
            update("insert into identity.users (email, password_hash, full_name, access_role) values (?, 'h', 'A', ?)",
                    unique("ok") + "@example.com", role);
        }
    }

    @Test
    void tokensAndSessionsRequireUniqueDigestsAndValidExpiry() {
        UUID user = user();
        String digest = unique("d");
        update("insert into identity.email_verification_tokens (user_id, token_digest, expires_at) values (?, ?, now() + interval '1 hour')", user, digest);
        assertSqlState(UNIQUE, () -> update(
                "insert into identity.email_verification_tokens (user_id, token_digest, expires_at) values (?, ?, now() + interval '1 hour')", user, digest));
        assertSqlState(CHECK, () -> update(
                "insert into identity.password_reset_tokens (user_id, token_digest, expires_at) values (?, ?, now() - interval '1 hour')", user, unique("p")));
        assertSqlState(CHECK, () -> update(
                "insert into identity.user_sessions (user_id, session_digest, expires_at) values (?, ?, now() - interval '1 hour')", user, unique("s")));
        update("insert into identity.password_reset_tokens (user_id, token_digest, expires_at) values (?, ?, now() + interval '1 hour')", user, unique("p"));
        update("insert into identity.user_sessions (user_id, session_digest, expires_at) values (?, ?, now() + interval '1 hour')", user, unique("s"));
    }

    @Test
    void rejectsOrphanOwnedRecords() {
        assertSqlState(FOREIGN_KEY, () -> update(
                "insert into identity.user_sessions (user_id, session_digest, expires_at) values (?, ?, now() + interval '1 hour')",
                UUID.randomUUID(), unique("s")));
    }

    @Test
    void deletingAUserCascadesAddressesTokensAndSessions() {
        UUID user = user();
        update("insert into identity.user_addresses (user_id, recipient_name, postal_code, street, street_number, city, state_code) "
                + "values (?, 'N', '00000-000', 'S', '1', 'C', 'SP')", user);
        update("insert into identity.email_verification_tokens (user_id, token_digest, expires_at) values (?, ?, now() + interval '1 hour')", user, unique("v"));
        update("insert into identity.password_reset_tokens (user_id, token_digest, expires_at) values (?, ?, now() + interval '1 hour')", user, unique("p"));
        update("insert into identity.user_sessions (user_id, session_digest, expires_at) values (?, ?, now() + interval '1 hour')", user, unique("s"));

        update("delete from identity.users where id = ?", user);

        for (String table : new String[] {"user_addresses", "email_verification_tokens", "password_reset_tokens", "user_sessions"}) {
            assertThat(count("select count(*) from identity." + table + " where user_id = ?", user)).as(table).isZero();
        }
    }

    @Test
    void allowsOnlyOneDefaultAddressPerUser() {
        UUID user = user();
        String insert = "insert into identity.user_addresses (user_id, recipient_name, postal_code, street, street_number, city, state_code, is_default) "
                + "values (?, 'N', '00000-000', 'S', '1', 'C', 'SP', true)";
        update(insert, user);
        assertSqlState(UNIQUE, () -> update(insert, user));
    }
}
