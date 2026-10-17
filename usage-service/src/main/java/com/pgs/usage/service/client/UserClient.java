package com.pgs.usage.service.client;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.pgs.usage.service.dto.UserDto;

/**
 * HTTP client for communicating with the User Service.
 * <p>
 * This component acts as an anti-corruption layer between the current service
 * and the {@code user-service}, hiding the details of the HTTP communication
 * (URL building, serialization, status handling) from the business logic.
 * <p>
 * It is backed by a {@link RestClient} bean (see
 * {@link com.pgs.usage.service.config.RestClientConfig}) whose base URL comes
 * from the {@code user.service.url} property, which allows different values
 * per environment (local, Docker, production).
 */
@Component
public class UserClient {

    private final RestClient restClient;

    /**
     * Creates a new {@code UserClient}.
     *
     * @param restClient the {@code RestClient} pre-configured with the User
     *                   Service base URL ({@code user.service.url}), for
     *                   example {@code http://localhost:8080/api/v1/user} or
     *                   {@code http://user-service:8080/api/v1/user}
     */
    public UserClient(@Qualifier("userRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    /**
     * Retrieves a user by its id from the User Service.
     * <p>
     * Performs a {@code GET} request to {@code {baseUrl}/{userId}} and
     * deserializes the JSON response into a {@link UserDto}.
     *
     * @param userId the id of the user to retrieve; must not be {@code null}
     * @return the {@link UserDto} returned by the User Service, or {@code null}
     *         if the response body is empty
     * @throws org.springframework.web.client.RestClientException if the HTTP
     *         request fails (for example, connection error or 5xx response)
     * @throws org.springframework.web.client.HttpClientErrorException if the
     *         User Service returns a 4xx response (for example, 404 Not Found)
     */
    public UserDto getUserById(Long userId) {
        return restClient.get()
                .uri("/{userId}", userId)
                .retrieve()
                .body(UserDto.class);
    }
}