package com.pgs.usage.service.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Pagination wrapper returned by the paginated endpoints of the Device Service.
 * <p>
 * The Device Service answers its list endpoints with this envelope
 * ({@code {"content": [...], "page": 0, "last": true, ...}}) instead of a bare
 * JSON array. Consumers must therefore unwrap {@link #content()} and keep
 * requesting pages until {@link #last()} is {@code true} (see
 * {@link com.pgs.usage.service.client.DeviceClient#getAllDevicesForUser(Long)}).
 *
 * @param <T>           the type of the elements in the page
 * @param content       the elements of the current page
 * @param page          the current page index (zero-based)
 * @param size          the page size
 * @param totalElements the total number of elements across all pages
 * @param totalPages    the total number of pages
 * @param last          whether this is the last page
 * @param first         whether this is the first page
 * @param empty         whether the page has no elements
 */
@Schema(description = "Pagination wrapper returned by the paginated endpoints of the Device Service")
public record PageResponse<T>(
        @Schema(description = "Elements of the current page")
        List<T> content,

        @Schema(description = "Current page index (zero-based)", example = "0")
        int page,

        @Schema(description = "Page size", example = "10")
        int size,

        @Schema(description = "Total number of elements across all pages", example = "1")
        long totalElements,

        @Schema(description = "Total number of pages", example = "1")
        int totalPages,

        @Schema(description = "Whether this is the last page", example = "true")
        boolean last,

        @Schema(description = "Whether this is the first page", example = "true")
        boolean first,

        @Schema(description = "Whether the page has no elements", example = "false")
        boolean empty) {}
