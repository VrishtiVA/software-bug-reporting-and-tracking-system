package com.example.backend.dto.responses.duplicate_detection;

import com.example.backend.model.bugs.ReportContainerStability;

public record ResolveDuplicatesOutcome(
        Long reportContainerId,
        Integer reportsRelocated,
        ReportContainerStability reportContainerStability
) {
    public static ResolveDuplicatesOutcome fromData(Long reportContainerId, Integer reportsRelocated, ReportContainerStability reportContainerStability) {
        return new ResolveDuplicatesOutcome(reportContainerId, reportsRelocated, reportContainerStability);
    }
}
