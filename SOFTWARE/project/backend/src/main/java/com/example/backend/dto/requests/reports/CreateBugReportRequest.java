package com.example.backend.dto.requests.reports;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * Note:
 * Captions are contractually in the order of provided attachments
 */
public record CreateBugReportRequest(
    @JsonProperty("summary") String summary,
    @JsonProperty("steps") String steps,
    @JsonProperty("environment") String environment,
    @JsonProperty("expected") String expected,
    @JsonProperty("actual") String actual,
    @JsonProperty("user_impact") Integer userImpact,
    @JsonProperty("severity") Integer severity,
    @JsonProperty("fix_suggestions") String fixSuggestions,
    @JsonProperty("additional_comments") String additionalComments,
    @JsonProperty("labels") List<String> labels,
    @JsonProperty("captions") List<String> captions
) {}