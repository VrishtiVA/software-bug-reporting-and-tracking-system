package com.example.backend.dto.responses.workspaces;

import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record WorkspaceDTO(
    @JsonProperty("id") long id,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("colour") String colour,
    @JsonProperty("is_public") boolean isPublic,
    @JsonProperty("creation_timestamp") Instant creationTimestamp,
    @JsonProperty("owner") String owner,
    @JsonProperty("user_role") WorkspaceRole userRole,
    @JsonProperty("custom_workspace_labels") List<WorkspaceCustomLabelDTO> workspaceCustomLabelDTO
) {
    /**
     * Method to convert workspace into a JSON response data transfer object.
     * @param workspace The workspace
     * @param role The users role in the workspace.
     * @return The workspace DTO object.
     */
    public static WorkspaceDTO fromEntity(Workspace workspace, WorkspaceRole role) {
        List<WorkspaceCustomLabelDTO> workspaceCustomLabelDTOList = workspace.getWorkspaceCustomLabels().stream().map(WorkspaceCustomLabelDTO::fromEntity).toList();
        return new WorkspaceDTO(
            workspace.getId(),
            workspace.getName(),
            workspace.getDescription(),
            workspace.getColour(),
            workspace.isPublic(),
            workspace.getCreationTimestamp(),
            workspace.getOwner().getUsername(),
            role,
            workspaceCustomLabelDTOList
        );
    }
}
