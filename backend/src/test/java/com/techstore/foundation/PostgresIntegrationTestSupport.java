package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.BeforeAll;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * Real PostgreSQL (Testcontainers) with every Flyway migration applied; no Spring context
 * and no external service beyond the container. Skipped, not passed, when Docker is missing.
 */
@Testcontainers(disabledWithoutDocker = true)
abstract class PostgresIntegrationTestSupport {

    static final String NOT_NULL = "23502";
    static final String FOREIGN_KEY = "23503";
    static final String UNIQUE = "23505";
    static final String CHECK = "23514";
    static final String RESTRICT = "23001"; // raised by guard triggers; FK ON DELETE RESTRICT raises 23503

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @BeforeAll
    static void migrate() {
        migrator().migrate();
    }

    static Flyway migrator() {
        return Flyway.configure()
                .dataSource(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword())
                .locations("classpath:db/migration")
                .schemas("public", "identity", "catalog", "inventory", "cart", "commercial",
                        "engagement", "promotions", "marketing", "platform")
                .defaultSchema("public")
                .createSchemas(true)
                .load();
    }

    static Connection connection() throws SQLException {
        return DriverManager.getConnection(postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
    }

    static int update(String sql, Object... args) {
        try (Connection c = connection(); PreparedStatement s = prepare(c, sql, args)) {
            return s.executeUpdate();
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    /** Runs an INSERT ... RETURNING id statement. */
    static UUID insert(String sql, Object... args) {
        try (Connection c = connection(); PreparedStatement s = prepare(c, sql, args); ResultSet r = s.executeQuery()) {
            r.next();
            return r.getObject(1, UUID.class);
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    static long count(String sql, Object... args) {
        try (Connection c = connection(); PreparedStatement s = prepare(c, sql, args); ResultSet r = s.executeQuery()) {
            r.next();
            return r.getLong(1);
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    static Object scalar(String sql, Object... args) {
        try (Connection c = connection(); PreparedStatement s = prepare(c, sql, args); ResultSet r = s.executeQuery()) {
            r.next();
            return r.getObject(1);
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    static PreparedStatement prepare(Connection c, String sql, Object... args) throws SQLException {
        PreparedStatement s = c.prepareStatement(sql);
        for (int i = 0; i < args.length; i++) {
            s.setObject(i + 1, args[i]);
        }
        return s;
    }

    static void assertSqlState(String expected, Runnable action) {
        try {
            action.run();
        } catch (SqlFailure e) {
            assertThat(e.state()).as(e.getMessage()).isEqualTo(expected);
            return;
        }
        fail("expected SQLSTATE " + expected + " but the statement succeeded");
    }

    /** Runs the statements in one transaction and commits, so deferred constraints fire. */
    static void inTransaction(TransactionBody body) {
        try (Connection c = connection()) {
            c.setAutoCommit(false);
            try {
                body.run(c);
                c.commit();
            } catch (SQLException e) {
                c.rollback();
                throw new SqlFailure(e);
            }
        } catch (SQLException e) {
            throw new SqlFailure(e);
        }
    }

    interface TransactionBody {
        void run(Connection c) throws SQLException;
    }

    static final class SqlFailure extends RuntimeException {
        private final String state;

        SqlFailure(SQLException cause) {
            super(cause.getMessage(), cause);
            this.state = cause.getSQLState();
        }

        String state() {
            return state;
        }
    }

    static String unique(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().substring(0, 12);
    }

    static UUID user() {
        return insert("insert into identity.users (email, password_hash, full_name) values (?, 'hash', 'Test') returning id",
                unique("u") + "@example.com");
    }

    static UUID category() {
        return insert("insert into catalog.categories (name, slug) values ('Cat', ?) returning id", unique("cat"));
    }

    static UUID product(UUID categoryId) {
        return insert("insert into catalog.products (primary_category_id, name, slug) values (?, 'Prod', ?) returning id",
                categoryId, unique("prod"));
    }

    static UUID product() {
        return product(category());
    }

    static UUID variant(UUID productId) {
        return insert("insert into catalog.product_variants (product_id, sku, name, price) values (?, ?, 'Var', 10.00) returning id",
                productId, unique("sku"));
    }

    static UUID attribute(String valueType) {
        return insert("insert into catalog.attributes (name, code, value_type) values ('Attr', ?, ?) returning id",
                unique("attr"), valueType);
    }

    static UUID contactRequest() {
        return insert("insert into commercial.contact_requests (contact_name, contact_email) values ('N', 'n@example.com') returning id");
    }

    static UUID campaign() {
        return insert("insert into marketing.marketing_emails (campaign_name, subject) values ('C', 'S') returning id");
    }
}
