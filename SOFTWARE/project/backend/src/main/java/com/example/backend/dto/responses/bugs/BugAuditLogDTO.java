package com.example.backend.dto.responses.bugs;

import com.example.backend.dto.responses.users.UserOverviewDTO;
import com.example.backend.model.bugs.AuditToken;
import com.example.backend.model.bugs.ReportContainerActivity;
import com.example.backend.model.bugs.ReportContainerActivityLogType;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record BugAuditLogDTO(
    @JsonProperty("id") Long id,
    @JsonProperty("report_container_id") Long reportContainerId,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("log_type") ReportContainerActivityLogType logType,
    @JsonProperty("content") List<AuditToken> content,
    @JsonProperty("performer") UserOverviewDTO performer,
    @JsonProperty("performer_role") WorkspaceRole performerRole,
    @JsonProperty("field") String field
) {
    public static BugAuditLogDTO fromEntity(ReportContainerActivity activity) {
        return new BugAuditLogDTO(
            activity.getId(),
            activity.getReportContainer().getId(),
            activity.getTimestamp(),
            activity.getLogType(),
            activity.getContent(),
            (activity.getPerformer() != null ? UserOverviewDTO.fromEntity(activity.getPerformer()) : null),
            activity.getPerformerRole(),
            activity.getField()
        );
    }
}
