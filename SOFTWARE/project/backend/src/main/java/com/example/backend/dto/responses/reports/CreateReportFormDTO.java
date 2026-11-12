package com.example.backend.dto.responses.reports;

import com.example.backend.dto.responses.workspaces.WorkspaceCustomLabelDTO;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record CreateReportFormDTO(
    @JsonProperty("user_role") WorkspaceRole userRole,
    @JsonProperty("custom_labels") List<WorkspaceCustomLabelDTO> customLabels,
    @JsonProperty("workspace_id") Long workspaceId,
    @JsonProperty("workspace_name") String workspaceName,
    @JsonProperty("workspace_colour") String workspaceColour
) {
    /** Method to combine this data into a joint DTO */
    public static CreateReportFormDTO fromData(WorkspaceRole userRole, List<WorkspaceCustomLabelDTO> customLabels, Workspace workspace) {
        return new CreateReportFormDTO(
            userRole,
            customLabels,
            workspace.getId(),
            workspace.getName(),
            workspace.getColour()
        );
    }
}
