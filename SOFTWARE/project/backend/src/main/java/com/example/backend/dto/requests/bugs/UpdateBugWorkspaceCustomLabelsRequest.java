package com.example.backend.dto.requests.bugs;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record UpdateBugWorkspaceCustomLabelsRequest(
        @JsonProperty("add") List<String> add,
        @JsonProperty("remove") List<String> remove
) {}
