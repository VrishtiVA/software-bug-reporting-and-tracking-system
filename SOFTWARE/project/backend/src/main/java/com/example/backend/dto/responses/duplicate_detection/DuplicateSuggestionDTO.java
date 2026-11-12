package com.example.backend.dto.responses.duplicate_detection;

import com.fasterxml.jackson.annotation.JsonProperty;

public record DuplicateSuggestionDTO(
    @JsonProperty("to_report_id") Long toReportId,
    @JsonProperty("similarity_order") int similarityOrder,
    @JsonProperty("similarity_score") double similarityScore,
    @JsonProperty("is_confident") boolean isConfident
) {}
