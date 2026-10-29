package com.pgs.usage.service.client;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import com.pgs.usage.service.dto.DeviceDto;
import com.pgs.usage.service.dto.PageResponse;

import lombok.extern.slf4j.Slf4j;

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
@Slf4j
public class DeviceClient {

    /**
     * Safety valve for the pagination loop: the maximum number of pages requested
     * for a single user. It avoids an endless loop if the Device Service ever
     * returned an inconsistent {@code last} flag.
     */
    private static final int MAX_PAGES = 1000;

    private final RestClient restClient;

    /** Page size requested when walking the paginated device endpoints. */
    private final int pageSize;

    /**
     * Creates a new {@code DeviceClient}.
     *
     * @param restClient the {@code RestClient} pre-configured with the Device
     *                   Service base URL ({@code device.service.url}), for
     *                   example {@code http://localhost:8081/api/v1/device} or
     *                   {@code http://device-service:8081/api/v1/device}
     * @param pageSize   the page size used when fetching a user's devices,
     *                   injected from the {@code device.service.page-size}
     *                   property
     */
    public DeviceClient(@Qualifier("deviceRestClient") RestClient restClient,
                        @Value("${device.service.page-size:100}") int pageSize) {
        this.restClient = restClient;
        this.pageSize = pageSize;
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
     * The Device Service answers {@code GET /user/{userId}} with a paginated
     * envelope ({@link PageResponse}) instead of a bare JSON array, so this method
     * requests the pages sequentially ({@code ?page={page}&size={size}}, the page
     * size coming from the {@code device.service.page-size} property) and
     * accumulates the {@code content} of every page until {@code last} is
     * {@code true}.
     * <p>
     * If a page has no content or the response body is empty, an empty list is
     * returned instead of {@code null}, so callers can iterate safely without a
     * null check.
     *
     * @param userId the id of the user whose devices are to be retrieved; must
     *               not be {@code null}
     * @return every device of the user; never {@code null}, but possibly empty
     * @throws org.springframework.web.client.RestClientException if the HTTP
     *         request fails (for example, connection error or 5xx response)
     * @throws org.springframework.web.client.HttpClientErrorException if the
     *         Device Service returns a 4xx response
     */
    public List<DeviceDto> getAllDevicesForUser(Long userId) {
        final List<DeviceDto> devices = new ArrayList<>();
        int page = 0;
        boolean last = false;

        while (!last && page < MAX_PAGES) {
            final PageResponse<DeviceDto> response = restClient.get()
                    .uri("/user/{userId}?page={page}&size={size}", userId, page, pageSize)
                    .retrieve()
                    .body(new ParameterizedTypeReference<PageResponse<DeviceDto>>() {
                    });

            if (response == null || response.content() == null || response.content().isEmpty()) {
                break;
            }

            devices.addAll(response.content());
            last = response.last();
            page++;
        }

        if (!last && page >= MAX_PAGES) {
            log.warn("Stopped fetching devices for user {} after {} pages (page cap reached)",
                    userId, MAX_PAGES);
        }

        log.debug("Fetched {} device(s) for user {} in {} page(s)", devices.size(), userId, page);
        return devices;
    }
}