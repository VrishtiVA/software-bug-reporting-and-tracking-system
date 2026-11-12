package com.example.backend.repositories.bugs.projections;

public interface WorkspaceCustomLabelDistributionProjection {
    String getWorkspaceCustomLabelName();
    String getWorkspaceCustomLabelColour();
    Integer getUsageCount();
}
