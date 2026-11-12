package com.example.backend.dto.responses.workspaces;

import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Used in response delivery format. */
public record MemberWorkspaceOverviewDTO(
    @JsonProperty("id") long id,
    @JsonProperty("name") String name,
    @JsonProperty("colour") String colour,
    @JsonProperty("is_public") boolean isPublic,
    @JsonProperty("role") WorkspaceRole role,
    @JsonProperty("member_count") long memberCount,
    @JsonProperty("bug_count") long bugCount
) {}
