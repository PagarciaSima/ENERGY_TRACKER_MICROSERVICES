package com.pgs.usage.service.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.pgs.usage.service.dto.DeviceDto;

/**
 * HTTP client for communicating with the Device Service.
 * <p>
 * This component acts as an anti-corruption layer between the current service
 * and the {@code device-service}, hiding the details of the HTTP communication
 * (URL building, serialization, status handling) from the business logic.
 * <p>
 * The base URL of the Device Service is read from the {@code device.service.url}
 * property, which allows different values per environment (local, Docker,
 * production).
 */
@Component
public class DeviceClient {

    private final RestTemplate restTemplate;

    private final String baseUrl;

    /**
     * Creates a new {@code DeviceClient}.
     *
     * @param baseUrl the base URL of the Device Service, injected from the
     *                {@code device.service.url} property (for example,
     *                {@code http://localhost:8081} or {@code http://device-service:8081})
     */
    public DeviceClient(@Value("${device.service.url}") String baseUrl) {
        this.restTemplate = new RestTemplate();
        this.baseUrl = baseUrl;
    }

    /**
     * Retrieves a device by its id from the Device Service.
     * <p>
     * Performs a {@code GET} request to {@code {baseUrl}/{deviceId}} and
     * deserializes the JSON response into a {@link DeviceDto}.
     *
     * @param deviceId the id of the device to retrieve; must not be {@code null}
     * @return the {@link DeviceDto} returned by the Device Service, or
     *         {@code null} if the response body is empty
     * @throws org.springframework.web.client.RestClientException if the HTTP
     *         request fails (for example, connection error or 5xx response)
     * @throws org.springframework.web.client.HttpClientErrorException if the
     *         Device Service returns a 4xx response (for example, 404 Not Found)
     */
    public DeviceDto getDeviceById(Long deviceId) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/{deviceId}")
                .buildAndExpand(deviceId)
                .toUriString();

        ResponseEntity<DeviceDto> response = restTemplate.getForEntity(url, DeviceDto.class);
        return response.getBody();
    }

    /**
     * Retrieves all devices belonging to a given user from the Device Service.
     * <p>
     * Performs a {@code GET} request to {@code {baseUrl}/user/{userId}} and
     * deserializes the JSON array response into a list of {@link DeviceDto}.
     * <p>
     * If the response body is empty or {@code null}, an empty list is returned
     * instead of {@code null}, so callers can iterate safely without a null
     * check.
     *
     * @param userId the id of the user whose devices are to be retrieved; must
     *               not be {@code null}
     * @return a list of {@link DeviceDto}; never {@code null}, but possibly empty
     * @throws org.springframework.web.client.RestClientException if the HTTP
     *         request fails (for example, connection error or 5xx response)
     * @throws org.springframework.web.client.HttpClientErrorException if the
     *         Device Service returns a 4xx response
     */
    public List<DeviceDto> getAllDevicesForUser(Long userId) {
        String url = UriComponentsBuilder
                .fromUriString(baseUrl)
                .path("/user/{userId}")
                .buildAndExpand(userId)
                .toUriString();

        ResponseEntity<DeviceDto[]> response = restTemplate.getForEntity(url, DeviceDto[].class);
        DeviceDto[] devices = response.getBody();
        return devices == null ? List.of() : List.of(devices);
    }
}