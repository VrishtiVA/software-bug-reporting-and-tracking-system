package com.example.backend.dto.responses.reports;

import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.bugs.ReportContainerStatus;
import com.example.backend.model.reports.DuplicateDetectionLifecycleState;
import com.example.backend.model.reports.Report;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record UserReportOverviewDTO(
        @JsonProperty("workspace_id") Long workspaceId,
        @JsonProperty("workspace_name") String workspaceName,
        @JsonProperty("workspace_colour") String workspaceColour,
        @JsonProperty("report_id") Long id,
        @JsonProperty("report_summary") String summary,
        @JsonProperty("creation_timestamp") Instant creationTimestamp,
        @JsonProperty("is_contained") Boolean isContained,
        @JsonProperty("is_workspace_visitable") Boolean isWorkspaceVisitable,
        @JsonProperty("bug_id") Long reportContainerId,
        @JsonProperty("bug_progress") ReportContainerProgress reportContainerProgress,
        @JsonProperty("bug_status") ReportContainerStatus reportContainerStatus
) {
    public static UserReportOverviewDTO fromEntity(Report report, Boolean isContained, Boolean isWorkspaceVisitable) {
        boolean hasContainer = report.getReportContainer() != null;
        return new UserReportOverviewDTO(
            report.getWorkspace().getId(),
            report.getWorkspace().getName(),
            report.getWorkspace().getColour(),
            report.getId(),
            report.getSummaryTitle(),
            report.getCreationTimestamp(),
            isContained,
            isWorkspaceVisitable,
            (hasContainer ? report.getReportContainer().getId() : null),
            (hasContainer ? report.getReportContainer().getProgress() : null),
            (hasContainer ? report.getReportContainer().getStatus() : null)
        );
    }
}
