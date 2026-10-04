package com.pgs.device.service.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.pgs.device.service.dto.DeviceDto;
import com.pgs.device.service.dto.PageResponse;
import com.pgs.device.service.exception.DeviceNotFoundException;
import com.pgs.device.service.model.DeviceType;
import com.pgs.device.service.service.DeviceService;

/**
 * Web-layer slice tests for {@link DeviceController} using {@link WebMvcTest}.
 * <p>
 * The {@link DeviceService} is mocked so no database or Redis is required. The
 * {@code GlobalExceptionHandler} advice is picked up from the application
 * context, so error responses (404) are asserted against the real
 * {@code ErrorResponse} body.
 */
@WebMvcTest(DeviceController.class)
class DeviceControllerTest {

    private static final String NAME = "Smart Bulb Salón";
    private static final DeviceType TYPE = DeviceType.LIGHT;
    private static final String LOCATION = "Salón principal";
    private static final Long USER_ID = 1L;

    private static final String BASE_URL = "/api/v1/device";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DeviceService deviceService;

    /* ------------------------------------------------------------------ */
    /*  Test data helpers                                                  */
    /* ------------------------------------------------------------------ */

    private static DeviceDto existingDeviceDto(Long id) {
        return DeviceDto.builder()
                .id(id)
                .name(NAME).type(TYPE).location(LOCATION).userId(USER_ID)
                .build();
    }

    private static PageResponse<DeviceDto> singlePage() {
        Page<DeviceDto> page = new PageImpl<>(
                List.of(existingDeviceDto(1L)), PageRequest.of(0, 10), 1L);
        return PageResponse.of(page);
    }

    private static String deviceJsonBody() {
        return """
                {
                  "name": "Smart Bulb Salón",
                  "type": "LIGHT",
                  "location": "Salón principal",
                  "userId": 1
                }
                """;
    }
    /* ------------------------------------------------------------------ */
    /*  POST /api/v1/device                                              */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("createDevice")
    class CreateDevice {

        @Test
        void whenValidBody_returnsCreatedWithBody() throws Exception {
            when(deviceService.createDevice(any(DeviceDto.class)))
                    .thenReturn(existingDeviceDto(1L));

            mockMvc.perform(post(BASE_URL)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deviceJsonBody()))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value(NAME))
                    .andExpect(jsonPath("$.type").value("LIGHT"))
                    .andExpect(jsonPath("$.userId").value(1));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/device                                               */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("getAllDevices")
    class GetAllDevices {

        @Test
        void returnsPaginatedPage() throws Exception {
            when(deviceService.getAllDevices(anyInt(), anyInt())).thenReturn(singlePage());

            mockMvc.perform(get(BASE_URL)
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.content[0].type").value("LIGHT"))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/device/search                                        */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("searchDevices")
    class SearchDevices {

        @Test
        void returnsMatchingPage() throws Exception {
            // any(DeviceFilterDto.class) + anyInt() for both ints:
            // all arguments must be matchers (no mixing with literals).
            when(deviceService.searchDevices(any(), anyInt(), anyInt()))
                    .thenReturn(singlePage());

            mockMvc.perform(get(BASE_URL + "/search")
                            .param("name", "Sal")
                            .param("type", "LIGHT")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].id").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/device/{id}                                          */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("getDeviceById")
    class GetDeviceById {

        @Test
        void whenExists_returnsDevice() throws Exception {
            when(deviceService.getDeviceById(1L)).thenReturn(existingDeviceDto(1L));

            mockMvc.perform(get(BASE_URL + "/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.type").value("LIGHT"));
        }

        @Test
        void whenMissing_returnsNotFound() throws Exception {
            when(deviceService.getDeviceById(99L))
                    .thenThrow(new DeviceNotFoundException(99L));

            mockMvc.perform(get(BASE_URL + "/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.status").value(404))
                    .andExpect(jsonPath("$.error").value("Not Found"))
                    .andExpect(jsonPath("$.message").value("Device not found with id: 99"));
        }
    }
    /* ------------------------------------------------------------------ */
    /*  GET /api/v1/device/user/{userId}                                  */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("getDevicesByUserId")
    class GetDevicesByUserId {

        @Test
        void returnsUserDevicesPage() throws Exception {
            when(deviceService.getDevicesByUserId(anyLong(), anyInt(), anyInt()))
                    .thenReturn(singlePage());

            mockMvc.perform(get(BASE_URL + "/user/1")
                            .param("page", "0")
                            .param("size", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content[0].userId").value(1))
                    .andExpect(jsonPath("$.totalElements").value(1));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  PUT /api/v1/device/{id}                                          */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("updateDevice")
    class UpdateDevice {

        @Test
        void whenExists_returnsUpdatedDevice() throws Exception {
            when(deviceService.updateDevice(anyLong(), any(DeviceDto.class)))
                    .thenReturn(existingDeviceDto(1L));

            mockMvc.perform(put(BASE_URL + "/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(deviceJsonBody()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value(NAME));
        }
    }

    /* ------------------------------------------------------------------ */
    /*  DELETE /api/v1/device/{id}                                        */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("deleteDevice")
    class DeleteDevice {

        @Test
        void whenExists_returnsNoContent() throws Exception {
            mockMvc.perform(delete(BASE_URL + "/1"))
                    .andExpect(status().isNoContent());

            verify(deviceService).deleteDevice(1L);
        }
    }
}