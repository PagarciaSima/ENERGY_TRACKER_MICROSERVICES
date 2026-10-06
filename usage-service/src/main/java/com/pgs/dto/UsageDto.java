package com.pgs.dto;

import java.util.List;

import lombok.Builder;

@Builder
public record UsageDto(
    Long userId,
    List<DeviceDto> devices
) {
}
