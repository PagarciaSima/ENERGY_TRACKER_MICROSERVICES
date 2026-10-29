package com.pgs.device.service.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Generic pagination wrapper returned by pageable endpoints.
 *
 * @param <T> the type of the elements in the page
 */
@Schema(description = "Pagination wrapper returned by pageable endpoints")
public class PageResponse<T> {

    /** The elements of the current page. */
    @ArraySchema(schema = @Schema(description = "Elements of the current page"))
    private final List<T> content;

    /** The current page index (zero-based). */
    @Schema(description = "Current page index (zero-based)", example = "0")
    private final int page;

    /** The page size. */
    @Schema(description = "Page size", example = "10")
    private final int size;

    /** Total number of elements across all pages. */
    @Schema(description = "Total number of elements across all pages", example = "1")
    private final long totalElements;

    /** Total number of pages. */
    @Schema(description = "Total number of pages", example = "1")
    private final int totalPages;

    /** Whether this is the last page. */
    @Schema(description = "Whether this is the last page", example = "true")
    private final boolean last;

    /** Whether this is the first page. */
    @Schema(description = "Whether this is the first page", example = "true")
    private final boolean first;

    /** Whether the page has no elements. */
    @Schema(description = "Whether the page has no elements", example = "false")
    private final boolean empty;

    /**
     * Creates a pagination wrapper. Instances are normally obtained through
     * {@link #of(Page)} rather than by calling this constructor directly.
     *
     * @param content       the elements of the current page
     * @param page          the current page index (zero-based)
     * @param size          the page size
     * @param totalElements the total number of elements across all pages
     * @param totalPages    the total number of pages
     * @param first         whether this is the first page
     * @param last          whether this is the last page
     * @param empty         whether the page has no elements
     */
    private PageResponse(List<T> content, int page, int size, long totalElements, int totalPages,
                         boolean first, boolean last, boolean empty) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.first = first;
        this.last = last;
        this.empty = empty;
    }

    /**
     * Builds a {@link PageResponse} from a Spring Data {@link Page}.
     *
     * @param page the Spring Data page
     * @param <T>  the element type
     * @return the pagination wrapper
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty());
    }

    /**
     * Returns the elements of the current page.
     *
     * @return the elements of the current page
     */
    public List<T> getContent() {
        return content;
    }

    /**
     * Returns the current page index (zero-based).
     *
     * @return the current page index
     */
    public int getPage() {
        return page;
    }

    /**
     * Returns the page size.
     *
     * @return the page size
     */
    public int getSize() {
        return size;
    }

    /**
     * Returns the total number of elements across all pages.
     *
     * @return the total number of elements
     */
    public long getTotalElements() {
        return totalElements;
    }

    /**
     * Returns the total number of pages.
     *
     * @return the total number of pages
     */
    public int getTotalPages() {
        return totalPages;
    }

    /**
     * Indicates whether this is the last page.
     *
     * @return {@code true} if this is the last page
     */
    public boolean isLast() {
        return last;
    }

    /**
     * Indicates whether this is the first page.
     *
     * @return {@code true} if this is the first page
     */
    public boolean isFirst() {
        return first;
    }

    /**
     * Indicates whether the page has no elements.
     *
     * @return {@code true} if the page is empty
     */
    public boolean isEmpty() {
        return empty;
    }
}
