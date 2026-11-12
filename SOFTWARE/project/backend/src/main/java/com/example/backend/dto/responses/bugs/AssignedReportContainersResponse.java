package com.example.backend.dto.responses.bugs;

import com.example.backend.dto.responses.workspaces.WorkspaceMemberDTO;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AssignedReportContainersResponse(
    @JsonProperty("assigned_bugs") PaginatedBugOverviewResponse assignedBugs,
    @JsonProperty(value = "workspace_assignable_members", required = false) List<WorkspaceMemberDTO> workspaceAssignableMembers
) {
    public static AssignedReportContainersResponse fromData(PaginatedBugOverviewResponse assignedBugsPage, List<WorkspaceMemberDTO> workspaceAssignableMembers) {
        return new AssignedReportContainersResponse(
            assignedBugsPage,
            workspaceAssignableMembers
        );
    }
}
