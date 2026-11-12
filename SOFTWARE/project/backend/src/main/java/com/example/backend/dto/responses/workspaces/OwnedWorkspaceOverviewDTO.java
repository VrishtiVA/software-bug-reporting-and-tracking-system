package com.example.backend.dto.responses.workspaces;

import com.fasterxml.jackson.annotation.JsonProperty;

/** Used in response delivery format. */
public record OwnedWorkspaceOverviewDTO(
    @JsonProperty("id") long id,
    @JsonProperty("name") String name,
    @JsonProperty("colour") String colour,
    @JsonProperty("is_public") boolean isPublic,
    @JsonProperty("member_count") long memberCount,
    @JsonProperty("bug_count") long bugCount
) {}
