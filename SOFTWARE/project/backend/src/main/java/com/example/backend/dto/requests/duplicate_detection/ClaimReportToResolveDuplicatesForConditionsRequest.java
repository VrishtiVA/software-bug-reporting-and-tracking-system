package com.example.backend.dto.requests.duplicate_detection;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ClaimReportToResolveDuplicatesForConditionsRequest(
       @JsonProperty(value = "exclude_report_ids", required = false) List<Long> excludeReportIds,
       @JsonProperty(value = "bug_id", required = false) Long reportContainerId
) {}
