package com.example.backend.dto.responses.bugs;

import com.example.backend.dto.responses.workspaces.WorkspaceCustomLabelDTO;
import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.bugs.ReportContainerStatus;
import com.example.backend.model.workspaces.WorkspaceMember;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record ReportContainerOverviewDTO(
    @JsonProperty("workspace_id") Long workspaceId,
    @JsonProperty("workspace_name") String workspaceName,
    @JsonProperty("workspace_colour") String workspaceColour,
    @JsonProperty("id") Long id,
    @JsonProperty("progress") ReportContainerProgress progress,
    @JsonProperty("summary") String summary,
    @JsonProperty("status") ReportContainerStatus status,
    @JsonProperty("first_reported_timestamp") Instant firstReportedTimestamp,
    @JsonProperty("priority") Double priority,
    @JsonProperty("priority_label") String priorityLabel,
    @JsonProperty("frequency") Integer frequency,
    @JsonProperty("hotness") Double hotness,
    @JsonProperty("relevant_duplicate_suggestions") int relevantDuplicateSuggestions,
    @JsonProperty("assignees") AssigneesOverviewDTO assignees,
    @JsonProperty("labels") List<WorkspaceCustomLabelDTO> labels
) {
    public static ReportContainerOverviewDTO fromEntity(ReportContainer reportContainer, List<WorkspaceMember> assignees, Long totalAssignees, String priorityLabel) {
        List<WorkspaceCustomLabelDTO> workspaceCustomLabelDTOS = reportContainer.getWorkspaceCustomLabels().stream().map(mapping -> WorkspaceCustomLabelDTO.fromEntity(mapping.getWorkspaceCustomLabel())).toList();
        AssigneesOverviewDTO limitedAssignees = AssigneesOverviewDTO.fromData(assignees, totalAssignees);
        return new ReportContainerOverviewDTO(
            reportContainer.getWorkspace().getId(),
            reportContainer.getWorkspace().getName(),
            reportContainer.getWorkspace().getColour(),
            reportContainer.getId(),
            reportContainer.getProgress(),
            reportContainer.getSummary(),
            reportContainer.getStatus(),
            reportContainer.getFirstReportedTimestamp(),
            reportContainer.getPriority(),
            priorityLabel,
            reportContainer.getReports().size(),
            reportContainer.getHotness(),
            reportContainer.getIncomingDuplicateSuggestionsCount(),
            limitedAssignees,
            workspaceCustomLabelDTOS
        );
    }
}
