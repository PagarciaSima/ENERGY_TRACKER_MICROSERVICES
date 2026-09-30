package com.pgs.user.service.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * End-to-end integration test that boots the full application against real
 * MySQL and Redis containers (via Testcontainers) and exercises the HTTP API
 * with {@link TestRestTemplate}.
 *
 * <p>Flyway applies the {@code V1__user_table.sql} migration against the real
 * MySQL database, and the {@code CacheConfig} reads/writes the real Redis
 * instance, so the whole stack (controller - service - repository - DB - cache)
 * is verified without any mocks.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class UserServiceIntegrationTest {

    private static final String BASE_URL = "/api/v1/user";

    private static final ObjectMapper JSON = new ObjectMapper();

    private static final HttpHeaders JSON_HEADERS = new HttpHeaders();

    static {
        JSON_HEADERS.setContentType(MediaType.APPLICATION_JSON);
    }

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.3.0")
            .withDatabaseName("testdb");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
                    DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379)
            .waitingFor(Wait.forListeningPort());

    @Autowired
    private TestRestTemplate rest;

    /**
     * Points the datasource and the Spring Cache (Redis) at the running
     * containers before the application context is created.
     */
    @DynamicPropertySource
    static void registerDatasource(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "com.mysql.cj.jdbc.Driver");
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379).toString());
    }

    /* ------------------------------------------------------------------ */
    /*  HTTP helpers (real request/response against the running app)       */
    /* ------------------------------------------------------------------ */

    private static String userJson(String name, String email) {
        return """
                {"name":"%s","surname":"García","email":"%s","address":"Calle Mayor 5, Madrid","alerting":true,"energyAlertingThreshold":3200.5}
                """.formatted(name, email);
    }

    private static String uniqueEmail() {
        return UUID.randomUUID() + "@example.com";
    }

    /** Wraps a JSON payload with the correct {@code Content-Type} header. */
    private static HttpEntity<String> jsonEntity(String body) {
        return new HttpEntity<>(body, JSON_HEADERS);
    }

    /** Creates a user via the API and returns the auto-generated id. */
    private long createUser(String name, String email) throws Exception {
        ResponseEntity<String> created = rest.exchange(
                BASE_URL, HttpMethod.POST, jsonEntity(userJson(name, email)), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return JSON.readTree(created.getBody()).get("id").asLong();
    }

    /** Fetches a user by id via the API, returning the parsed JSON body. */
    private JsonNode getUser(long id) throws Exception {
        ResponseEntity<String> response = rest.getForEntity(
                BASE_URL + "/{id}", String.class, id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JSON.readTree(response.getBody());
    }

    /* ------------------------------------------------------------------ */
    /*  Full CRUD lifecycle over the real stack                            */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("full CRUD lifecycle")
    class Lifecycle {

        @Test
        void createGetSearchUpdateDelete() throws Exception {
            String name = "Ana";
            String email = uniqueEmail();

            // 1. Missing user -> 404
            ResponseEntity<String> missing = rest.exchange(
                    BASE_URL + "/{id}", HttpMethod.GET, null, String.class, 999_999L);
            assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(JSON.readTree(missing.getBody()).get("status").asInt()).isEqualTo(404);

            // 2. Create -> 201 with auto-generated id
            long id = createUser(name, email);

            // 3. Get by id -> 200 (also populates the Redis cache)
            JsonNode created = getUser(id);
            assertThat(created.get("name").asText()).isEqualTo(name);
            assertThat(created.get("email").asText()).isEqualTo(email);
            assertThat(created.get("alerting").asBoolean()).isTrue();

            // 4. Search by name fragment -> contains the created user
            ResponseEntity<String> search = rest.getForEntity(
                    BASE_URL + "/search?name={name}", String.class, name);
            assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(JSON.readTree(search.getBody()).toString()).contains(email);

            // 5. Update -> 200 with updated data (evicts the cache)
            String updatedBody = """
                    {"name":"Beatriz","surname":"López","email":"%s","address":"Gran Vía 1, Madrid","alerting":false,"energyAlertingThreshold":2500.0}
                    """.formatted(email);
            ResponseEntity<String> updated = rest.exchange(
                    BASE_URL + "/{id}", HttpMethod.PUT,
                    jsonEntity(updatedBody), String.class, id);
            assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode updatedJson = JSON.readTree(updated.getBody());
            assertThat(updatedJson.get("name").asText()).isEqualTo("Beatriz");
            assertThat(updatedJson.get("alerting").asBoolean()).isFalse();

            // 6. Delete -> 204
            ResponseEntity<String> deleted = rest.exchange(
                    BASE_URL + "/{id}", HttpMethod.DELETE, null, String.class, id);
            assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // 7. Now it is gone -> 404
            ResponseEntity<String> afterDelete = rest.exchange(
                    BASE_URL + "/{id}", HttpMethod.GET, null, String.class, id);
            assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Business constraint (duplicate e-mail) against the real DB         */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("duplicate e-mail")
    class DuplicateEmail {

        @Test
        void secondCreateForSameEmailReturnsConflict() throws Exception {
            String email = uniqueEmail();
            createUser("Luis", email);

            ResponseEntity<String> duplicate = rest.exchange(
                    BASE_URL, HttpMethod.POST,
                    jsonEntity(userJson("Another", email)), String.class);

            assertThat(duplicate.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
            JsonNode body = JSON.readTree(duplicate.getBody());
            assertThat(body.get("status").asInt()).isEqualTo(409);
            assertThat(body.get("error").asText()).isEqualTo("Conflict");
            assertThat(body.get("message").asText())
                    .isEqualTo("User already exists with email: " + email);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Caching behaviour through the Redis-backed cache                   */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("caching")
    class Caching {

        @Test
        void repeatedReadsAreServedConsistently() throws Exception {
            String email = uniqueEmail();
            long id = createUser("Carmen", email);

            // A second read must be served successfully (cache manager active)
            // and return exactly the same representation as the first.
            JsonNode first = getUser(id);
            JsonNode second = getUser(id);

            assertThat(second.get("id").asLong()).isEqualTo(first.get("id").asLong());
            assertThat(second.get("email").asText()).isEqualTo(email);
            assertThat(second.get("name").asText()).isEqualTo("Carmen");
        }
    }
}