package com.pgs.usage.service.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.pgs.usage.service.dto.DeviceDto;

/**
 * HTTP client for communicating with the Device Service.
 * <p>
 * This component acts as an anti-corruption layer between the current service
 * and the {@code device-service}, hiding the details of the HTTP communication
 * (URL building, serialization, status handling) from the business logic.
 * <p>
 * It is backed by a {@link RestClient} bean (see
 * {@link com.pgs.usage.service.config.RestClientConfig}) whose base URL comes
 * from the {@code device.service.url} property, which allows different values
 * per environment (local, Docker, production).
 */
@Component
public class DeviceClient {

    private final RestClient restClient;

    /**
     * Creates a new {@code DeviceClient}.
     *
     * @param restClient the {@code RestClient} pre-configured with the Device
     *                   Service base URL ({@code device.service.url}), for
     *                   example {@code http://localhost:8081/api/v1/device} or
     *                   {@code http://device-service:8081/api/v1/device}
     */
    public DeviceClient(@Qualifier("deviceRestClient") RestClient restClient) {
        this.restClient = restClient;
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
        return restClient.get()
                .uri("/{deviceId}", deviceId)
                .retrieve()
                .body(DeviceDto.class);
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
        List<DeviceDto> devices = restClient.get()
                .uri("/user/{userId}", userId)
                .retrieve()
                .body(new ParameterizedTypeReference<List<DeviceDto>>() {
                });
        return devices == null ? List.of() : devices;
    }
}