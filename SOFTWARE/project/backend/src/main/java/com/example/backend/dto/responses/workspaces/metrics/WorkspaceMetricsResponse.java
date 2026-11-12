package com.example.backend.dto.responses.workspaces.metrics;

import com.example.backend.model.bugs.ReportContainerProgress;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record WorkspaceMetricsResponse(
    @JsonProperty("total_bugs") Long totalActiveBugs,
    @JsonProperty("total_outstanding_duplicate_report_suggestions") Integer totalOutstandingDuplicateReportSuggestions,
    @JsonProperty("total_staged_duplicate_reports") Integer totalStagedDuplicateReports,
    @JsonProperty("workspace_custom_label_usage_distribution") List<WorkspaceCustomLabelUsageDTO> workspaceCustomLabelUsageDistribution,
    @JsonProperty("progress_distribution") Map<ReportContainerProgress, Integer> progressDistribution,
    @JsonProperty("total_bugs_resolved") Long totalBugsResolved,
    @JsonProperty("priority_category_distribution") Map<String, Integer> priorityCategoryDistribution,
    @JsonProperty("priority_frequency_bug_count_buckets") List<PriorityFrequencyBugCountBucketsDTO> priorityFrequencyBugCountBuckets
) {
    public static WorkspaceMetricsResponse fromData(
        Long totalActiveBugs,
        Integer totalOutstandingDuplicateReportSuggestions,
        Integer totalStagedDuplicateReports,
        List<WorkspaceCustomLabelUsageDTO> workspaceCustomLabelUsageDistribution,
        Map<ReportContainerProgress, Integer> progressDistribution,
        Long totalBugsResolved,
        Map<String, Integer> priorityCategoryDistribution,
        List<PriorityFrequencyBugCountBucketsDTO> priorityFrequencyBugCountBuckets
    ) {
        return new WorkspaceMetricsResponse(
            totalActiveBugs,
            totalOutstandingDuplicateReportSuggestions,
            totalStagedDuplicateReports,
            workspaceCustomLabelUsageDistribution,
            progressDistribution,
            totalBugsResolved,
            priorityCategoryDistribution,
            priorityFrequencyBugCountBuckets
        );
    }
}
