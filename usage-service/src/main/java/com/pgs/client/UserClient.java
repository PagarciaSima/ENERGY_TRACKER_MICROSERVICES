package com.pgs.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.pgs.dto.UserDto;

/**
 * HTTP client for communicating with the User Service.
 * <p>
 * This component acts as an anti-corruption layer between the current service
 * and the {@code user-service}, hiding the details of the HTTP communication
 * (URL building, serialization, status handling) from the business logic.
 * <p>
 * The base URL of the User Service is read from the {@code user.service.url}
 * property, which allows different values per environment (local, Docker,
 * production).
 */
@Component
public class UserClient {

    private final RestTemplate restTemplate;

    private final String baseUrl;

    /**
     * Creates a new {@code UserClient}.
     *
     * @param baseUrl the base URL of the User Service, injected from the
     *                {@code user.service.url} property (for example,
     *                {@code http://localhost:8080} or {@code http://user-service:8080})
     */
    public UserClient(@Value("${user.service.url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
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
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/{userId}")
                .buildAndExpand(userId)
                .toUriString();

        ResponseEntity<UserDto> response = restTemplate.getForEntity(url, UserDto.class);
        return response.getBody();
    }
}