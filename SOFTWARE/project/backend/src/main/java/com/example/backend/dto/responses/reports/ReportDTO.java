package com.example.backend.dto.responses.reports;

import com.example.backend.dto.responses.users.UserOverviewDTO;
import com.example.backend.dto.responses.workspaces.WorkspaceCustomLabelDTO;
import com.example.backend.model.reports.Report;
import com.example.backend.model.reports.ReportAttachment;
import com.example.backend.model.reports.ReportWorkspaceCustomLabelMapping;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public record ReportDTO(
    @JsonProperty("id") Long id,
    @JsonProperty("report_container_id") Long containerId,
    @JsonProperty("creation_timestamp") Instant creationTimestamp,
    @JsonProperty("user") UserOverviewDTO reportingUser,
    @JsonProperty("user_role") WorkspaceRole userRole,
    @JsonProperty("summary") String summary,
    @JsonProperty("steps") String steps,
    @JsonProperty("environment") String environment,
    @JsonProperty("expected") String expected,
    @JsonProperty("actual") String actual,
    @JsonProperty("severity_rating") Integer severityRating,
    @JsonProperty("user_impact_rating") Integer userImpactRating,
    @JsonProperty("fix_suggestions") String fixSuggestions,
    @JsonProperty("additional_comments") String additionalComments,
    @JsonProperty("labels") List<WorkspaceCustomLabelDTO> labels,
    @JsonProperty("attachment_metadata") List<ReportAttachmentDTO> attachmentMetadata
) {
    public static ReportDTO fromEntity(Report report) {
        //Fetch custom labels attached to report
        List<WorkspaceCustomLabelDTO> labels = new ArrayList<>();
        for (ReportWorkspaceCustomLabelMapping label : report.getWorkspaceCustomLabels()) {
            labels.add(WorkspaceCustomLabelDTO.fromEntity(label.getWorkspaceCustomLabel()));
        }
        //Fetch attachment DTOs
        List<ReportAttachmentDTO> attachments = new ArrayList<>();
        for (ReportAttachment a : report.getReportAttachments()) {
            attachments.add(ReportAttachmentDTO.fromEntity(a));
        }
        //Return report in DTO format.
        return new ReportDTO(
            report.getId(),
            (report.getReportContainer() == null ? null : report.getReportContainer().getId()),
            report.getCreationTimestamp(),
            (report.getUser() == null ? null : UserOverviewDTO.fromEntity(report.getUser())),
            report.getUserRole(),
            report.getSummaryTitle(),
            report.getStepsToReproduce(),
            report.getEnvironment(),
            report.getExpectedResult(),
            report.getActualResult(),
            report.getSeverityRating(),
            report.getUserImpactRating(),
            report.getFixSuggestions(),
            report.getAdditionalComments(),
            labels,
            attachments
        );
    }
}
