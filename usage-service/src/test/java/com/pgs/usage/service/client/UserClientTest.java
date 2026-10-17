package com.pgs.usage.service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import com.pgs.usage.service.dto.UserDto;

/**
 * Unit tests for {@link UserClient}. The HTTP layer is mocked with
 * {@link MockRestServiceServer}, so no real User Service is required.
 * <p>
 * The tests verify URL building (relative to the base URL), JSON
 * deserialization, the mapping of HTTP status codes to exceptions
 * ({@code retrieve()} throws on 4xx, as the old {@code RestTemplate} did) and
 * the empty-body handling (returns {@code null}).
 */
@DisplayName("UserClient")
class UserClientTest {

    private static final String BASE_URL = "http://user-service:8080/api/v1/user";

    private MockRestServiceServer server;
    private UserClient userClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        userClient = new UserClient(builder.baseUrl(BASE_URL).build());
    }

    @Nested
    @DisplayName("getUserById")
    class GetUserById {

        @Test
        @DisplayName("returns the deserialized user on 200")
        void returnsDeserializedUserOn200() {
            String json = """
                    {"id":1,"name":"Ana","surname":"Garcia","email":"ana@example.com",\
                    "address":"Calle Mayor 1","alerting":true,"energyAlertingThreshold":10.5}
                    """;
            server.expect(requestTo(BASE_URL + "/1"))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            UserDto user = userClient.getUserById(1L);

            assertThat(user).isNotNull();
            assertThat(user.id()).isEqualTo(1L);
            assertThat(user.name()).isEqualTo("Ana");
            assertThat(user.surname()).isEqualTo("Garcia");
            assertThat(user.email()).isEqualTo("ana@example.com");
            assertThat(user.alerting()).isTrue();
            assertThat(user.energyAlertingThreshold()).isEqualTo(10.5);
            server.verify();
        }

        @Test
        @DisplayName("performs a GET request to {baseUrl}/{userId}")
        void performsGetRequestToExpectedUrl() {
            String json = """
                    {"id":5,"name":"Luis","surname":"Perez","email":"luis@example.com",\
                    "address":"Avda. Libertad 2","alerting":false,"energyAlertingThreshold":8.0}
                    """;
            server.expect(requestTo(BASE_URL + "/5"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            userClient.getUserById(5L);

            server.verify();
        }

        @Test
        @DisplayName("throws HttpClientErrorException.NotFound on 404")
        void throwsNotFoundOn404() {
            server.expect(requestTo(BASE_URL + "/999"))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> userClient.getUserById(999L))
                    .isInstanceOf(HttpClientErrorException.NotFound.class);
            server.verify();
        }

        @Test
        @DisplayName("returns null when the response body is empty")
        void returnsNullWhenBodyEmpty() {
            server.expect(requestTo(BASE_URL + "/1"))
                    .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

            assertThat(userClient.getUserById(1L)).isNull();
            server.verify();
        }
    }
}