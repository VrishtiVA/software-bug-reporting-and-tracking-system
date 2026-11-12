package com.example.backend.dto.responses.workspaces.metrics;

import com.example.backend.dto.responses.workspaces.WorkspaceCustomLabelDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

public record WorkspaceCustomLabelUsageDTO(
        @JsonProperty("workspace_custom_label") WorkspaceCustomLabelDTO workspaceCustomLabelDTO,
        @JsonProperty("label_usage") Integer usage
) {
    public static WorkspaceCustomLabelUsageDTO fromData(WorkspaceCustomLabelDTO workspaceCustomLabelDTO, Integer usage) {
        return new WorkspaceCustomLabelUsageDTO(
            workspaceCustomLabelDTO,
            usage
        );
    }
}
