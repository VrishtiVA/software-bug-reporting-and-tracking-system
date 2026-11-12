package com.example.backend.repositories.bugs.projections;

import com.example.backend.model.users.User;

public interface ReportContainerUserAssigneeProjection {
    Long getReportContainerId();
    User getUser();
}
