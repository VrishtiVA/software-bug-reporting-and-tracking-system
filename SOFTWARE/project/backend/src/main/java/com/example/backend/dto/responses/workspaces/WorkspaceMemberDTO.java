package com.example.backend.dto.responses.workspaces;

import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

public record WorkspaceMemberDTO(
    @JsonProperty("user_id") long userId,
    @JsonProperty("username") String username,
    @JsonProperty("role") WorkspaceRole role,
    @JsonProperty("user_colour") String userColour,
    @JsonProperty("assignment_count") long assignmentCount
) {}