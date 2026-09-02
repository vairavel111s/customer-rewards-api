package com.retailer.rewards.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.data.domain.Page;

/**
 * A stable, framework independent page wrapper.
 *
 * <p>Spring Data's {@code Page} is deliberately not serialised straight onto the wire: its
 * JSON shape is an implementation detail and can change between versions, which would
 * silently break clients.</p>
 *
 * @param <T> the element type
 */
@Schema(description = "A page of results")
public class PagedResponse<T> {

    private final List<T> content;

    @Schema(example = "0")
    private final int page;

    @Schema(example = "20")
    private final int size;

    @Schema(example = "37")
    private final long totalElements;

    @Schema(example = "2")
    private final int totalPages;

    @Schema(example = "false")
    private final boolean last;

    public PagedResponse(List<T> content, int page, int size, long totalElements, int totalPages,
                         boolean last) {
        this.content = content;
        this.page = page;
        this.size = size;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
        this.last = last;
    }

    /**
     * Builds a wrapper from a Spring Data page whose elements have already been mapped.
     */
    public static <S, T> PagedResponse<T> from(Page<S> page, List<T> mappedContent) {
        return new PagedResponse<>(mappedContent, page.getNumber(), page.getSize(),
                page.getTotalElements(), page.getTotalPages(), page.isLast());
    }

    public List<T> getContent() {
        return content;
    }

    public int getPage() {
        return page;
    }

    public int getSize() {
        return size;
    }

    public long getTotalElements() {
        return totalElements;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public boolean isLast() {
        return last;
    }
}
