package com.techstore.foundation;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import com.techstore.TechStoreApplication;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = TechStoreApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class DatabaseReadinessIntegrationTest {

    @Container
    static final PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:17-alpine");

    @LocalServerPort
    private int port;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Test
    void appliesSchemaMigrationAndGoesUnreadyWhenDatabaseStops() throws Exception {
        Map<String, Integer> moduleSchemas = Map.of(
                "identity", 1,
                "catalog", 1,
                "inventory", 1,
                "cart", 1,
                "commercial", 1,
                "engagement", 1,
                "promotions", 1,
                "marketing", 1,
                "platform", 1);
        moduleSchemas.forEach((schema, expectedCount) -> assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.schemata where schema_name = ?",
                Integer.class,
                schema)).isEqualTo(expectedCount));

        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from public.flyway_schema_history where version = '1'",
                Integer.class)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_schema = 'public' and table_name = 'flyway_schema_history'",
                Integer.class)).isEqualTo(1);

        HttpResponse<String> readyResponse = readinessResponse();
        assertThat(readyResponse.statusCode()).isEqualTo(200);
        assertThat(readyResponse.body()).contains("\"status\":\"UP\"");

        postgres.stop();
        HttpResponse<String> unavailableResponse = readinessResponse();
        assertThat(unavailableResponse.statusCode()).isEqualTo(503);
        assertThat(unavailableResponse.body()).contains("\"status\":\"DOWN\"");
    }

    private HttpResponse<String> readinessResponse() throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + "/actuator/health/readiness"))
                .timeout(Duration.ofSeconds(10))
                .GET()
                .build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }
}
