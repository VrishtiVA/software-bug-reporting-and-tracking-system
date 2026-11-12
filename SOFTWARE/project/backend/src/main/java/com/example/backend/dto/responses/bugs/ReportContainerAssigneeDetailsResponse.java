package com.example.backend.dto.responses.bugs;

import com.example.backend.model.workspaces.WorkspaceMember;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReportContainerAssigneeDetailsResponse(
    @JsonProperty("bug_id") Long bugId,
    @JsonProperty("user_role") WorkspaceRole userRole,
    @JsonProperty("assignees") List<AssigneeDetailsOverviewDTO> assignedUsers,
    @JsonProperty("assignable") List<AssigneeDetailsOverviewDTO> assignableUsers
) {
    public static ReportContainerAssigneeDetailsResponse fromData(
            Long bugId,
            WorkspaceRole userRole,
            List<WorkspaceMember> assignedUsers,
            List<WorkspaceMember> assignableUsers
    ) {
        //Convert workspace member lists into detail overview DTOs.
        List<AssigneeDetailsOverviewDTO> assignedUsersList = assignedUsers.stream().map(AssigneeDetailsOverviewDTO::fromEntity).toList();
        List<AssigneeDetailsOverviewDTO> assignableUsersList = assignableUsers.stream().map(AssigneeDetailsOverviewDTO::fromEntity).toList();
        //Return serialisable record.
        return new ReportContainerAssigneeDetailsResponse(
            bugId,
            userRole,
            assignedUsersList,
            assignableUsersList
        );
    }
}