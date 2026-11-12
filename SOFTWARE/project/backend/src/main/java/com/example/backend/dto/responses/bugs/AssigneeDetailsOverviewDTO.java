package com.example.backend.dto.responses.bugs;

import com.example.backend.dto.responses.users.UserOverviewDTO;
import com.example.backend.model.workspaces.WorkspaceMember;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

public record AssigneeDetailsOverviewDTO(
    @JsonProperty("user") UserOverviewDTO user,
    @JsonProperty("user_role") WorkspaceRole userRole,
    @JsonProperty("member_assignment_count") Long memberAssignmentCount
) {
    public static AssigneeDetailsOverviewDTO fromEntity(WorkspaceMember workspaceMember) {
        return new AssigneeDetailsOverviewDTO(
            UserOverviewDTO.fromEntity(workspaceMember.getUser()),
            workspaceMember.getRole(),
            workspaceMember.getAssignmentsCount()
        );
    }
}