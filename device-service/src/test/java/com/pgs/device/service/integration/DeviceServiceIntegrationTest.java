package com.pgs.device.service.integration;

import static org.assertj.core.api.Assertions.assertThat;

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
 * <p>Flyway applies the {@code V1__device_table.sql} migration against the real
 * MySQL database, so the whole stack (controller - service - repository - DB)
 * is verified without any mocks.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
class DeviceServiceIntegrationTest {

    private static final String BASE_URL = "/api/v1/device";

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
     * Points the datasource (and Redis, when used) at the running containers
     * before the application context is created.
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

    private static String deviceJson(String name, String type, String location, long userId) {
        return """
                {"name":"%s","type":"%s","location":"%s","userId":%d}
                """.formatted(name, type, location, userId);
    }

    /** Wraps a JSON payload with the correct {@code Content-Type} header. */
    private static HttpEntity<String> jsonEntity(String body) {
        return new HttpEntity<>(body, JSON_HEADERS);
    }

    /** Creates a device via the API and returns the auto-generated id. */
    private long createDevice(String name, String location, long userId) throws Exception {
        ResponseEntity<String> created = rest.exchange(
                BASE_URL, HttpMethod.POST,
                jsonEntity(deviceJson(name, "LIGHT", location, userId)), String.class);
        assertThat(created.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        return JSON.readTree(created.getBody()).get("id").asLong();
    }

    /** Fetches a device by id via the API, returning the parsed JSON body. */
    private JsonNode getDevice(long id) throws Exception {
        ResponseEntity<String> response = rest.getForEntity(
                BASE_URL + "/{id}", String.class, id);
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        return JSON.readTree(response.getBody());
    }
    /* ------------------------------------------------------------------ */
    /*  Full CRUD lifecycle over the real stack                           */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("full CRUD lifecycle")
    class Lifecycle {

        @Test
        void createGetSearchListByUserUpdateDelete() throws Exception {
            String name = "Bombilla Salón";
            String location = "Salón principal";
            long userId = 1L;

            // 1. Missing device -> 404
            ResponseEntity<String> missing = rest.exchange(
                    BASE_URL + "/{id}", HttpMethod.GET, null, String.class, 999_999L);
            assertThat(missing.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
            assertThat(JSON.readTree(missing.getBody()).get("status").asInt()).isEqualTo(404);

            // 2. Create -> 201 with auto-generated id
            long id = createDevice(name, location, userId);

            // 3. Get by id -> 200
            JsonNode created = getDevice(id);
            assertThat(created.get("name").asText()).isEqualTo(name);
            assertThat(created.get("type").asText()).isEqualTo("LIGHT");
            assertThat(created.get("location").asText()).isEqualTo(location);
            assertThat(created.get("userId").asLong()).isEqualTo(userId);

            // 4. Search by name fragment -> contains the created device
            ResponseEntity<String> search = rest.getForEntity(
                    BASE_URL + "/search?name={name}", String.class, "bombilla");
            assertThat(search.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(JSON.readTree(search.getBody()).toString()).contains(name);

            // 5. List by user -> contains the created device
            ResponseEntity<String> byUser = rest.exchange(
                    BASE_URL + "/user/{userId}?page=0&size=10",
                    HttpMethod.GET, null, String.class, userId);
            assertThat(byUser.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(JSON.readTree(byUser.getBody()).toString()).contains(name);

            // 6. Update -> 200 with updated data
            String updatedBody = deviceJson("Termostato", "THERMOSTAT", "Cocina", userId);
            ResponseEntity<String> updated = rest.exchange(
                    BASE_URL + "/{id}", HttpMethod.PUT,
                    jsonEntity(updatedBody), String.class, id);
            assertThat(updated.getStatusCode()).isEqualTo(HttpStatus.OK);
            JsonNode updatedJson = JSON.readTree(updated.getBody());
            assertThat(updatedJson.get("name").asText()).isEqualTo("Termostato");
            assertThat(updatedJson.get("type").asText()).isEqualTo("THERMOSTAT");

            // 7. Delete -> 204
            ResponseEntity<String> deleted = rest.exchange(
                    BASE_URL + "/{id}", HttpMethod.DELETE, null, String.class, id);
            assertThat(deleted.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

            // 8. Now it is gone -> 404
            ResponseEntity<String> afterDelete = rest.exchange(
                    BASE_URL + "/{id}", HttpMethod.GET, null, String.class, id);
            assertThat(afterDelete.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        }
    }

    /* ------------------------------------------------------------------ */
    /*  Repeated reads are served consistently                             */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("repeated reads")
    class RepeatedReads {

        @Test
        void areServedConsistently() throws Exception {
            long id = createDevice("Cámara", "Cocina", 3L);

            JsonNode first = getDevice(id);
            JsonNode second = getDevice(id);

            assertThat(second.get("id").asLong()).isEqualTo(first.get("id").asLong());
            assertThat(second.get("name").asText()).isEqualTo("Cámara");
            assertThat(second.get("type").asText()).isEqualTo("LIGHT");
        }
    }
}