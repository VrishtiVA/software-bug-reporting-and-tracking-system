package com.example.backend.dto.requests.bugs;

import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.bugs.ReportContainerStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record FiltersBugSearchRequest(
    @JsonProperty("progress") List<ReportContainerProgress> progress,
    @JsonProperty("status") List<ReportContainerStatus> status,
    @JsonProperty("labels") List<String> labels
) {}
