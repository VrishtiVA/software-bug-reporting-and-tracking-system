package com.example.backend.dto.responses.bugs;

import com.example.backend.dto.responses.workspaces.WorkspaceCustomLabelDTO;
import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.bugs.ReportContainerStatus;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record ReportContainerDetailsDTO(
    @JsonProperty("id") Long id,
    @JsonProperty("user_workspace_role") WorkspaceRole userWorkspaceRole,
    @JsonProperty("progress") ReportContainerProgress progress,
    @JsonProperty("summary") String summary,
    @JsonProperty("status") ReportContainerStatus status,
    @JsonProperty("first_reported_timestamp") Instant firstReportedTimestamp,
    @JsonProperty("last_updated_timestamp") Instant lastUpdatedTimestamp,
    @JsonProperty("priority") Double priority,
    @JsonProperty("priority_label") String priorityLabel,
    @JsonProperty("frequency") int frequency,
    @JsonProperty("relevant_duplicate_suggestions") int relevantDuplicateSuggestions,
    @JsonProperty("assignees") List<AssigneeDetailsOverviewDTO> assignees,
    @JsonProperty("labels") List<WorkspaceCustomLabelDTO> labels
) {
    public static ReportContainerDetailsDTO fromEntity(ReportContainer reportContainer, WorkspaceRole workspaceRole, List<AssigneeDetailsOverviewDTO> assigneesDTO, String priorityLabel) {
        List<WorkspaceCustomLabelDTO> workspaceCustomLabelsDTO = reportContainer.getWorkspaceCustomLabels().stream().map(rcwclm ->
            WorkspaceCustomLabelDTO.fromEntity(rcwclm.getWorkspaceCustomLabel())
        ).toList();
        return new ReportContainerDetailsDTO(
            reportContainer.getId(),
            workspaceRole,
            reportContainer.getProgress(),
            reportContainer.getSummary(),
            reportContainer.getStatus(),
            reportContainer.getFirstReportedTimestamp(),
            reportContainer.getLastUpdatedTimestamp(),
            reportContainer.getPriority(),
            priorityLabel,
            reportContainer.getReports().size(),
            reportContainer.getIncomingDuplicateSuggestionsCount(),
            assigneesDTO,
            workspaceCustomLabelsDTO
        );
    }
}
