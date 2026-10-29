package com.pgs.usage.service.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;

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

import com.pgs.usage.service.dto.DeviceDto;

/**
 * Unit tests for {@link DeviceClient}. The HTTP layer is mocked with
 * {@link MockRestServiceServer}, so no real Device Service is required.
 * <p>
 * The tests verify URL building (relative to the base URL), JSON
 * deserialization, the mapping of HTTP status codes to exceptions
 * ({@code retrieve()} throws on 4xx, as the old {@code RestTemplate} did) and
 * the empty-body handling (returns {@code null} / empty list).
 */
@DisplayName("DeviceClient")
class DeviceClientTest {

    private static final String BASE_URL = "http://device-service:8081/api/v1/device";

    /** Page size injected into the client, matching {@code device.service.page-size}. */
    private static final int PAGE_SIZE = 100;

    private MockRestServiceServer server;
    private DeviceClient deviceClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        deviceClient = new DeviceClient(builder.baseUrl(BASE_URL).build(), PAGE_SIZE);
    }

    @Nested
    @DisplayName("getDeviceById")
    class GetDeviceById {

        @Test
        @DisplayName("returns the deserialized device on 200")
        void returnsDeserializedDeviceOn200() {
            String json = """
                    {"id":42,"name":"Smart Bulb","type":"LIGHT","location":"Salon","userId":1,"energyConsumed":3.5}
                    """;
            server.expect(requestTo(BASE_URL + "/42"))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            DeviceDto device = deviceClient.getDeviceById(42L);

            assertThat(device).isNotNull();
            assertThat(device.id()).isEqualTo(42L);
            assertThat(device.name()).isEqualTo("Smart Bulb");
            assertThat(device.type()).isEqualTo("LIGHT");
            assertThat(device.location()).isEqualTo("Salon");
            assertThat(device.userId()).isEqualTo(1L);
            assertThat(device.energyConsumed()).isEqualTo(3.5);
            server.verify();
        }

        @Test
        @DisplayName("performs a GET request to {baseUrl}/{deviceId}")
        void performsGetRequestToExpectedUrl() {
            server.expect(requestTo(BASE_URL + "/7"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess("{\"id\":7}", MediaType.APPLICATION_JSON));

            deviceClient.getDeviceById(7L);

            server.verify();
        }

        @Test
        @DisplayName("throws HttpClientErrorException.NotFound on 404")
        void throwsNotFoundOn404() {
            server.expect(requestTo(BASE_URL + "/999"))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> deviceClient.getDeviceById(999L))
                    .isInstanceOf(HttpClientErrorException.NotFound.class);
            server.verify();
        }

        @Test
        @DisplayName("throws HttpClientErrorException.BadRequest on 400")
        void throwsBadRequestOn400() {
            server.expect(requestTo(BASE_URL + "/-1"))
                    .andRespond(withStatus(HttpStatus.BAD_REQUEST));

            assertThatThrownBy(() -> deviceClient.getDeviceById(-1L))
                    .isInstanceOf(HttpClientErrorException.BadRequest.class);
            server.verify();
        }

        @Test
        @DisplayName("returns null when the response body is empty")
        void returnsNullWhenBodyEmpty() {
            server.expect(requestTo(BASE_URL + "/42"))
                    .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

            assertThat(deviceClient.getDeviceById(42L)).isNull();
            server.verify();
        }
    }

    @Nested
    @DisplayName("getAllDevicesForUser")
    class GetAllDevicesForUser {

        @Test
        @DisplayName("returns the devices of a single page on 200")
        void returnsDevicesOfSinglePageOn200() {
            String json = """
                    {
                      "content": [
                        {"id":1,"name":"Bulb","type":"LIGHT","location":"Salon","userId":7},
                        {"id":2,"name":"Thermostat","type":"THERMOSTAT","location":"Cocina","userId":7}
                      ],
                      "page": 0,
                      "size": 100,
                      "totalElements": 2,
                      "totalPages": 1,
                      "last": true,
                      "first": true,
                      "empty": false
                    }
                    """;
            server.expect(requestTo(BASE_URL + "/user/7?page=0&size=100"))
                    .andExpect(method(HttpMethod.GET))
                    .andRespond(withSuccess(json, MediaType.APPLICATION_JSON));

            List<DeviceDto> devices = deviceClient.getAllDevicesForUser(7L);

            assertThat(devices).hasSize(2);
            assertThat(devices.get(0).id()).isEqualTo(1L);
            assertThat(devices.get(0).name()).isEqualTo("Bulb");
            assertThat(devices.get(0).userId()).isEqualTo(7L);
            assertThat(devices.get(1).id()).isEqualTo(2L);
            server.verify();
        }

        @Test
        @DisplayName("walks every page until last is true")
        void walksEveryPageUntilLastIsTrue() {
            server.expect(requestTo(BASE_URL + "/user/7?page=0&size=100"))
                    .andRespond(withSuccess("""
                            {"content":[{"id":1,"userId":7},{"id":2,"userId":7}],
                             "page":0,"size":100,"totalElements":3,"totalPages":2,
                             "last":false,"first":true,"empty":false}
                            """, MediaType.APPLICATION_JSON));
            server.expect(requestTo(BASE_URL + "/user/7?page=1&size=100"))
                    .andRespond(withSuccess("""
                            {"content":[{"id":3,"userId":7}],
                             "page":1,"size":100,"totalElements":3,"totalPages":2,
                             "last":true,"first":false,"empty":false}
                            """, MediaType.APPLICATION_JSON));

            List<DeviceDto> devices = deviceClient.getAllDevicesForUser(7L);

            assertThat(devices).extracting(DeviceDto::id).containsExactly(1L, 2L, 3L);
            server.verify();
        }

        @Test
        @DisplayName("throws HttpClientErrorException.NotFound on 404")
        void throwsNotFoundOn404ForUserDevices() {
            server.expect(requestTo(BASE_URL + "/user/7?page=0&size=100"))
                    .andRespond(withStatus(HttpStatus.NOT_FOUND));

            assertThatThrownBy(() -> deviceClient.getAllDevicesForUser(7L))
                    .isInstanceOf(HttpClientErrorException.NotFound.class);
            server.verify();
        }

        @Test
        @DisplayName("returns an empty list when the page has no content")
        void returnsEmptyListWhenContentIsEmpty() {
            server.expect(requestTo(BASE_URL + "/user/7?page=0&size=100"))
                    .andRespond(withSuccess("""
                            {"content":[],"page":0,"size":100,"totalElements":0,"totalPages":0,
                             "last":true,"first":true,"empty":true}
                            """, MediaType.APPLICATION_JSON));

            assertThat(deviceClient.getAllDevicesForUser(7L)).isEmpty();
            server.verify();
        }

        @Test
        @DisplayName("returns an empty list when the response body is empty")
        void returnsEmptyListWhenBodyEmpty() {
            server.expect(requestTo(BASE_URL + "/user/7?page=0&size=100"))
                    .andRespond(withSuccess("", MediaType.APPLICATION_JSON));

            assertThat(deviceClient.getAllDevicesForUser(7L)).isEmpty();
            server.verify();
        }
    }
}
