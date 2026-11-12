package com.example.backend.dto.responses.workspaces;

import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import com.fasterxml.jackson.annotation.JsonProperty;

public record WorkspaceCustomLabelDTO(
    @JsonProperty("name") String name,
    @JsonProperty("colour") String colour
) {
    public static WorkspaceCustomLabelDTO fromEntity(WorkspaceCustomLabel workspaceCustomLabel) {
        return new WorkspaceCustomLabelDTO(
            workspaceCustomLabel.getName(),
            workspaceCustomLabel.getColour()
        );
    }

    public static WorkspaceCustomLabelDTO fromData(String name, String colour) {
        return new WorkspaceCustomLabelDTO(name, colour);
    }
}
