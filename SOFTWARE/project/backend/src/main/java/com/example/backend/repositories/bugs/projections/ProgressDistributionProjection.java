package com.example.backend.repositories.bugs.projections;

import com.example.backend.model.bugs.ReportContainerProgress;

public interface ProgressDistributionProjection {
    ReportContainerProgress getProgress();
    Integer getBugCount();
}
