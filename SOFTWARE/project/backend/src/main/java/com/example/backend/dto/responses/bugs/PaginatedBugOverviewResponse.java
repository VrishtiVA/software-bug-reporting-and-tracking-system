package com.example.backend.dto.responses.bugs;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.data.domain.Page;

import java.util.List;

public record PaginatedBugOverviewResponse (
    @JsonProperty("content") List<?> content,
    @JsonProperty("page_number") int pageNumber,
    @JsonProperty("page_size") int pageSize,
    @JsonProperty("total_elements") long totalElements,
    @JsonProperty("total_pages") int totalPages,
    @JsonProperty("is_first") boolean isFirst,
    @JsonProperty("is_last") boolean isLast
) {
    public static PaginatedBugOverviewResponse fromPage(Page<ReportContainerOverviewDTO> reportContainerOverviews) {
        return new PaginatedBugOverviewResponse(
            reportContainerOverviews.getContent(),
            reportContainerOverviews.getNumber(),
            reportContainerOverviews.getSize(),
            reportContainerOverviews.getTotalElements(),
            reportContainerOverviews.getTotalPages(),
            reportContainerOverviews.isFirst(),
            reportContainerOverviews.isLast()
        );
    }
}