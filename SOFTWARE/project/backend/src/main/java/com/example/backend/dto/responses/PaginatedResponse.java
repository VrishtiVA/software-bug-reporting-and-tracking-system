package com.example.backend.dto.responses;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Paginated response wrapper DTO.
 * Providing consistent metadata for pages.
 */
public record PaginatedResponse(
        @JsonProperty("content") List<?> content,
        @JsonProperty("page_number") int pageNumber,
        @JsonProperty("page_size") int pageSize,
        @JsonProperty("total_elements") long totalElements,
        @JsonProperty("total_pages") int totalPages,
        @JsonProperty("is_first") boolean isFirst,
        @JsonProperty("is_last") boolean isLast
) {
    public static PaginatedResponse fromPage(Page<?> page) {
        return new PaginatedResponse(
            page.getContent(),
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages(),
            page.isFirst(),
            page.isLast()
        );
    }
}
