package com.example.backend.dto.requests.duplicate_detection;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * @param fromReportId The report the suggestion is to resolve.
 * @param toReportId The report fromReport was found to be a duplicate of.
 *                   If to_report_id is null, it means fromReport was rejected from all suggestions.
 * @param isAccepted true if accepted, otherwise false.
 */
public record ResolveDuplicateSuggestionRequest(
    @JsonProperty("from_report_id") Long fromReportId,
    @JsonProperty("to_report_id") Long toReportId,
    @JsonProperty("is_accepted") Boolean isAccepted
) {}