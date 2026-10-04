package com.pgs.device.service.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.pgs.device.service.dto.DeviceDto;
import com.pgs.device.service.dto.DeviceFilterDto;
import com.pgs.device.service.dto.PageResponse;
import com.pgs.device.service.entity.Device;
import com.pgs.device.service.exception.DeviceNotFoundException;
import com.pgs.device.service.model.DeviceType;
import com.pgs.device.service.repository.DeviceRepository;

/**
 * Unit tests for {@link DeviceService} business logic. The {@link DeviceRepository}
 * is mocked so no database is required. Caching and transactional behaviour are
 * covered separately (see {@link DeviceServiceCacheTest}).
 */
@ExtendWith(MockitoExtension.class)
class DeviceServiceTest {

    private static final String NAME = "Smart Bulb Salón";
    private static final DeviceType TYPE = DeviceType.LIGHT;
    private static final String LOCATION = "Salón principal";
    private static final Long USER_ID = 1L;

    @Mock
    private DeviceRepository deviceRepository;

    @InjectMocks
    private DeviceService deviceService;

    /* ------------------------------------------------------------------ */
    /*  Test data factories                                                */
    /* ------------------------------------------------------------------ */

    private static Device existingDevice(Long id) {
        return Device.builder()
                .id(id)
                .name(NAME).type(TYPE).location(LOCATION).userId(USER_ID)
                .build();
    }

    /** Request DTO as it would arrive via HTTP: no id yet. */
    private static DeviceDto newDeviceRequest() {
        return DeviceDto.builder()
                .name(NAME).type(TYPE).location(LOCATION).userId(USER_ID)
                .build();
    }

    private static DeviceDto existingDeviceDto(Long id) {
        return DeviceDto.builder()
                .id(id)
                .name(NAME).type(TYPE).location(LOCATION).userId(USER_ID)
                .build();
    }

    private static Page<Device> singlePage() {
        return new PageImpl<>(List.of(existingDevice(1L)), PageRequest.of(0, 10), 1L);
    }

    /* ------------------------------------------------------------------ */
    /*  createDevice                                                       */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("createDevice")
    class CreateDevice {

        @Test
        void persistsAndReturnsMappedDto() {
            when(deviceRepository.save(any(Device.class))).thenReturn(existingDevice(1L));

            DeviceDto result = deviceService.createDevice(newDeviceRequest());

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo(NAME);
            assertThat(result.getType()).isEqualTo(TYPE);
            assertThat(result.getLocation()).isEqualTo(LOCATION);
            assertThat(result.getUserId()).isEqualTo(USER_ID);

            verify(deviceRepository).save(any(Device.class));
        }
    }
    /* ------------------------------------------------------------------ */
    /*  read operations                                                    */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("getAllDevices")
    class GetAllDevices {

        @Test
        void returnsPageResponseWithMappedContent() {
            when(deviceRepository.findAll(any(Pageable.class))).thenReturn(singlePage());

            PageResponse<DeviceDto> result = deviceService.getAllDevices(0, 10);

            assertThat(result.getPage()).isZero();
            assertThat(result.getSize()).isEqualTo(10);
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getTotalPages()).isEqualTo(1);
            assertThat(result.isFirst()).isTrue();
            assertThat(result.isLast()).isTrue();
            assertThat(result.isEmpty()).isFalse();
            assertThat(result.getContent()).map(DeviceDto::getId).containsExactly(1L);
        }
    }

    @Nested
    @DisplayName("getDevicesByUserId")
    class GetDevicesByUserId {

        @Test
        void delegatesToFindAllByUserIdAndMapsResults() {
            when(deviceRepository.findAllByUserId(USER_ID, PageRequest.of(0, 10)))
                    .thenReturn(singlePage());

            PageResponse<DeviceDto> result = deviceService.getDevicesByUserId(USER_ID, 0, 10);

            assertThat(result.getContent()).map(DeviceDto::getId).containsExactly(1L);
            assertThat(result.getContent()).map(DeviceDto::getUserId)
                    .containsExactly(USER_ID);
            verify(deviceRepository).findAllByUserId(USER_ID, PageRequest.of(0, 10));
        }
    }

    @Nested
    @DisplayName("searchDevices")
    class SearchDevices {

        @SuppressWarnings("unchecked")
        @Test
        void buildsSpecificationAndMapsResults() {
            when(deviceRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenReturn(singlePage());

            DeviceFilterDto filter = DeviceFilterDto.builder().name("salón").build();
            PageResponse<DeviceDto> result = deviceService.searchDevices(filter, 0, 10);

            assertThat(result.getContent()).map(DeviceDto::getId).containsExactly(1L);
            verify(deviceRepository).findAll(any(Specification.class), any(Pageable.class));
        }
    }

    @Nested
    @DisplayName("getDeviceById")
    class GetDeviceById {

        @Test
        void whenExists_mapsEntityToDto() {
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(existingDevice(1L)));

            DeviceDto result = deviceService.getDeviceById(1L);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getType()).isEqualTo(TYPE);
        }

        @Test
        void whenMissing_throwsNotFound() {
            when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.getDeviceById(99L))
                    .isInstanceOf(DeviceNotFoundException.class)
                    .hasMessage("Device not found with id: 99");
        }
    }
    /* ------------------------------------------------------------------ */
    /*  updateDevice                                                       */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("updateDevice")
    class UpdateDevice {

        @Test
        void updatesManagedEntityInPlaceAndReturnsMappedDto() {
            Device existing = existingDevice(1L);
            when(deviceRepository.findById(1L)).thenReturn(Optional.of(existing));

            DeviceDto updates = DeviceDto.builder()
                    .name("Smart Bulb Cocina").type(DeviceType.LIGHT)
                    .location("Cocina").userId(2L)
                    .build();

            DeviceDto result = deviceService.updateDevice(1L, updates);

            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getName()).isEqualTo("Smart Bulb Cocina");
            assertThat(result.getLocation()).isEqualTo("Cocina");
            assertThat(result.getUserId()).isEqualTo(2L);

            assertThat(existing.getName()).isEqualTo("Smart Bulb Cocina");
            verify(deviceRepository, never()).save(any(Device.class));
        }

        @Test
        void whenMissing_throwsNotFound() {
            when(deviceRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> deviceService.updateDevice(99L, existingDeviceDto(99L)))
                    .isInstanceOf(DeviceNotFoundException.class)
                    .hasMessage("Device not found with id: 99");
        }
    }

    /* ------------------------------------------------------------------ */
    /*  deleteDevice                                                       */
    /* ------------------------------------------------------------------ */

    @Nested
    @DisplayName("deleteDevice")
    class DeleteDevice {

        @Test
        void whenExists_deletesById() {
            when(deviceRepository.existsById(anyLong())).thenReturn(true);

            deviceService.deleteDevice(1L);

            verify(deviceRepository).deleteById(1L);
        }

        @Test
        void whenMissing_throwsNotFoundAndDoesNotDelete() {
            when(deviceRepository.existsById(99L)).thenReturn(false);

            assertThatThrownBy(() -> deviceService.deleteDevice(99L))
                    .isInstanceOf(DeviceNotFoundException.class)
                    .hasMessage("Device not found with id: 99");

            verify(deviceRepository, never()).deleteById(99L);
        }
    }
}

