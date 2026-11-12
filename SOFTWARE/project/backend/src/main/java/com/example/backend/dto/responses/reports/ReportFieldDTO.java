package com.example.backend.dto.responses.reports;

import com.example.backend.dto.responses.users.UserOverviewDTO;
import com.example.backend.model.reports.Report;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ReportFieldDTO(
    @JsonProperty("id") Long id,
    @JsonProperty("report_container_id") Long containerId,
    @JsonProperty("creation_timestamp") Instant creationTimestamp,
    @JsonProperty("user") UserOverviewDTO reportingUser,
    @JsonProperty("user_role") WorkspaceRole userRole,
    @JsonProperty("field_data") String fieldData
) {
    public static ReportFieldDTO fromEntity(Report report, String field) {
        return new ReportFieldDTO(
            report.getId(),
            (report.getReportContainer() == null ? null : report.getReportContainer().getId()),
            report.getCreationTimestamp(),
            (report.getUser() == null ? null : UserOverviewDTO.fromEntity(report.getUser())),
            report.getUserRole(),
            (
                field.equals("steps") ? report.getStepsToReproduce() :
                field.equals("environment") ? report.getEnvironment() :
                field.equals("expected") ? report.getExpectedResult() :
                field.equals("actual") ? report.getActualResult() :
                field.equals("fix-suggestions") ? report.getFixSuggestions() :
                field.equals("additional-comments") ? report.getAdditionalComments() :
                null
            )
        );
    }
}
