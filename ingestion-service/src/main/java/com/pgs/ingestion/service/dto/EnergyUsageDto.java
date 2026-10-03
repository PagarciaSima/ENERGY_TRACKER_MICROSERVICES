package com.pgs.ingestion.service.dto;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;

import lombok.Builder;

@Builder
public record EnergyUsageDto (
    Long deviceId,
    double energyConsumed,
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    Instant timestamp) {}