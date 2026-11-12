package com.example.backend.dto.responses.workspaces;

import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

public record ShortWorkspaceOverviewDTO(
    @JsonProperty("id") long id,
    @JsonProperty("name") String name,
    @JsonProperty("colour") String colour,
    @JsonProperty("is_public") boolean isPublic,
    @JsonProperty("role") WorkspaceRole role
) {}