package com.example.backend.dto.responses.duplicate_detection;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DuplicateDetectionResponse(
    @JsonProperty("from_report_id") Long fromReportId,
    @JsonProperty("duplicate_suggestions") List<DuplicateSuggestionDTO> duplicateSuggestions
) {}
