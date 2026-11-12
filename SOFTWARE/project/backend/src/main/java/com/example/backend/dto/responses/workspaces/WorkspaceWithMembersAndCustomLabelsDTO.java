package com.example.backend.dto.responses.workspaces;

import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record WorkspaceWithMembersAndCustomLabelsDTO(
    @JsonProperty("id") long id,
    @JsonProperty("name") String name,
    @JsonProperty("description") String description,
    @JsonProperty("colour") String colour,
    @JsonProperty("is_public") boolean isPublic,
    @JsonProperty("owner") String owner,
    @JsonProperty("user_role") WorkspaceRole userRole,
    @JsonProperty("members") List<WorkspaceMemberDTO> members,
    @JsonProperty("custom_labels") List<WorkspaceCustomLabelDTO> customLabels
) {
    /**
     * Method to convert workspace into a JSON response data transfer object.
     * This response also includes the members and custom labels collection.
     * @param workspace The workspace
     * @param role The users role in the workspace.
     * @param members The workspace members.
     * @param customLabels The workspace custom labels.
     * @return The workspace DTO object.
     */
    public static WorkspaceWithMembersAndCustomLabelsDTO fromEntity(
        Workspace workspace,
        WorkspaceRole role,
        List<WorkspaceMemberDTO> members,
        List<WorkspaceCustomLabelDTO> customLabels
    ) {
        return new WorkspaceWithMembersAndCustomLabelsDTO(
                workspace.getId(),
                workspace.getName(),
                workspace.getDescription(),
                workspace.getColour(),
                workspace.isPublic(),
                workspace.getOwner().getUsername(),
                role,
                members,
                customLabels
        );
    }
}
