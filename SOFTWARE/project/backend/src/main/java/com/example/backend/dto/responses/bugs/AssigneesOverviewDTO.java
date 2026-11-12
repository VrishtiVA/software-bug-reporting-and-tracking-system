package com.example.backend.dto.responses.bugs;

import com.example.backend.model.workspaces.WorkspaceMember;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AssigneesOverviewDTO(
        @JsonProperty("limited_assignees") List<AssigneeDetailsOverviewDTO> limitedAssignees,
        @JsonProperty("total_assignees") Long totalAssignees
) {
    public static AssigneesOverviewDTO fromData(List<WorkspaceMember> assignees, Long totalAssignees) {
        List<AssigneeDetailsOverviewDTO> limitedAssignees = assignees != null ? assignees.stream().map(AssigneeDetailsOverviewDTO::fromEntity).toList() : null; //guard against null here.
        return new AssigneesOverviewDTO(
            limitedAssignees,
            totalAssignees
        );
    }
}
