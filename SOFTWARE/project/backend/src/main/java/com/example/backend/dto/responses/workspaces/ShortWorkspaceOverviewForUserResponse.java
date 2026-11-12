package com.example.backend.dto.responses.workspaces;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ShortWorkspaceOverviewForUserResponse(
        @JsonProperty("user_id") Long userId,
        @JsonProperty("workspaces") List<ShortWorkspaceOverviewDTO> workspaceOverviews
) {
    public static ShortWorkspaceOverviewForUserResponse fromData(List<ShortWorkspaceOverviewDTO> workspaceOverviews, Long userId) {
        return new ShortWorkspaceOverviewForUserResponse(userId, workspaceOverviews);
    }
}
