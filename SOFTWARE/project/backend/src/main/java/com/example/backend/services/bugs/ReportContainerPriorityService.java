package com.example.backend.services.bugs;

import com.example.backend.dto.responses.workspaces.metrics.PriorityFrequencyBugCountBucketsDTO;
import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.bugs.ReportContainerStability;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.repositories.bugs.projections.PriorityCategoryDistributionProjection;
import com.example.backend.repositories.bugs.ReportContainerRepository;
import com.example.backend.repositories.reports.ReportRepository;
import com.example.backend.repositories.workspaces.WorkspaceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReportContainerPriorityService {

    private static final Logger log = LoggerFactory.getLogger(ReportContainerPriorityService.class);

    @Autowired
    private ReportContainerRepository reportContainerRepository;
    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private WorkspaceRepository workspaceRepository;

    /* ---------- Automatic Priority Constants ---------- */
    public static final double SEVERITY_RATING_WEIGHT = 0.4;
    public static final double USER_IMPACT_RATING_WEIGHT = 0.325;
    public static final double FREQUENCY_WEIGHT = 0.15;
    public static final double AGE_WEEKS_WEIGHT = 0.125;
    public static final double AGE_MULTIPLIER_BASE = 0.5; //So that the age factor isn't too weak.

    /* ---------- Priority Label Lower Bounds ---------- */
    public static final double PRIORITY_P0LB = 77.5;
    public static final double PRIORITY_P1LB = 60;
    public static final double PRIORITY_P2LB = 42.5;
    public static final double PRIORITY_P3LB = 25;

    /**
     * Method to convert priority to priority category label.
     * Category is only used for presentation, and therefore,
     * quickly calculated and delivered on demand in O(1) time.
     * @param priority The priority
     * @return The priority category label.
     */
    public String convertToPriorityCategory(Double priority) {
        if (priority >= PRIORITY_P0LB) return "P0";
        else if (priority >= PRIORITY_P1LB) return "P1";
        else if (priority >= PRIORITY_P2LB) return "P2";
        else if (priority >= PRIORITY_P3LB) return "P3";
        else return "P4";
    }

    /**
     * Method to find the priority category distribution by workspace.
     * This should not include closed report containers/bugs.
     * @param workspace The workspace to find this for.
     * @return The number of bugs in each priority category.
     */
    public Map<String, Integer> findPriorityCategoryDistributionByWorkspace(Workspace workspace) {
        return reportContainerRepository.findPriorityCategoryDistributionByWorkspaceId(
            workspace.getId(), ReportContainerProgress.CLOSED, ReportContainerStability.STABLE,
            PRIORITY_P0LB, PRIORITY_P1LB, PRIORITY_P2LB, PRIORITY_P3LB
        ).stream().collect(Collectors.toMap(
            PriorityCategoryDistributionProjection::getPriorityLabel,
            PriorityCategoryDistributionProjection::getBugCount
        ));
    }

    /**
     * Method to fetch priority frequency bug count buckets for workspace dashboard metrics.
     * This data is useful for reviewing the state of the workspaces, the spread of bug priority.
     * @param workspace The workspace to fetch for.
     * @return List of PriorityFrequencyBugCountBucketsDTO
     * @see PriorityFrequencyBugCountBucketsDTO
     */
    public List<PriorityFrequencyBugCountBucketsDTO> findPriorityFrequencyBugCountBucketsByWorkspace(Workspace workspace) {
         return reportContainerRepository.findPriorityFrequencyBugCountBucketsByWorkspaceId(
            workspace.getId(), ReportContainerProgress.CLOSED, ReportContainerStability.STABLE
         ).stream().map(bucket -> PriorityFrequencyBugCountBucketsDTO.fromData(
            bucket.getPriorityBucketLB(), bucket.getFrequencyBucketLB(), bucket.getBugCount()
         )).toList();
    }

    /**
     * The hotness property aka trending or velocity is a measure to help discover which bugs are surfacing quickly, such as emerging incidents.
     * @param frequency The number of reports in the bug
     * @param firstReportedTimestamp The timestamp of when the bug was first reported.
     * @return The hotness of the report container/bug
     */
    public double calculateContainerHotness(long frequency, Instant firstReportedTimestamp, boolean isNew) {
        //Calculate age in weeks
        long age_weeks = isNew ? 0 : (
                Duration.between(firstReportedTimestamp, Instant.now()).toDays() / 7
        );
        return (double) frequency /(age_weeks == 0 ? 1 : age_weeks);
    }

    /**
     * Method to update report container hotness/trending value.
     * @param reportContainer The report container to update for.
     * @param frequency The report containers current report frequency.
     */
    @Transactional
    public void updateReportContainerHotness(ReportContainer reportContainer, Long frequency) {
        double hotness = calculateContainerHotness(
            (frequency != null ? frequency : countReportsInReportContainer(reportContainer.getId())),
            reportContainer.getFirstReportedTimestamp(),
            false
        );
        reportContainerRepository.updateHotness(reportContainer.getId(), hotness);
        log.info("Updated hotness of report container with ID:{}", reportContainer.getId());
    }

    public int countReportsInReportContainer(Long reportContainerId) {
        return reportRepository.countByReportContainerId(reportContainerId);
    }

    /**
     * Method to update report container total user-impact and severity ratings.
     * @param reportContainerId The ID of the report container to update this for.
     */
    @Transactional
    public void updateReportContainerMetricTotals(Long reportContainerId) {
        reportContainerRepository.updateReportContainerMetricTotalsById(reportContainerId);
    }

    /**
     * Update report container priority for one container
     * @param reportContainerId The ID of the report container to update.
     */
    @Transactional
    public void updateReportContainerPriority(Long reportContainerId) {
        try {
            reportContainerRepository.updateReportContainerPriority(
                reportContainerId,
                SEVERITY_RATING_WEIGHT, USER_IMPACT_RATING_WEIGHT, FREQUENCY_WEIGHT, AGE_WEEKS_WEIGHT, AGE_MULTIPLIER_BASE
            );
            log.info("Updated priority of report container with ID:{}", reportContainerId);
        } catch (Exception e) {
            log.error("Failed to update priority for report container with ID:{}", reportContainerId, e);
        }
    }

    /**
     * Method to orchestrate value updates for a report container,
     * including updating their priority, hotness, and total ratings.
     * @param reportContainer The report container to do this for.
     */
    @Transactional
    public void updateReportContainerMetrics(ReportContainer reportContainer) {
        updateReportContainerHotness(reportContainer, null);
        updateReportContainerMetricTotals(reportContainer.getId());
        updateReportContainerPriority(reportContainer.getId());
    }

    /**
     * Method to bulk update the priorities of report containers in a workspace.
     * @param workspaceId The workspace to update for.
     */
    @Transactional
    public void bulkUpdateReportContainerPriority(Long workspaceId) {
        try {
            reportContainerRepository.bulkUpdateReportContainerPrioritybyWorkspace(
                    workspaceId,
                    SEVERITY_RATING_WEIGHT, USER_IMPACT_RATING_WEIGHT, FREQUENCY_WEIGHT, AGE_WEEKS_WEIGHT, AGE_MULTIPLIER_BASE
            );
        } catch (Exception e) {
            log.error("Failed to bulk update priority of report containers in Workspace ID:{}", workspaceId, e);
        }
    }

    /**
     * Method to bulk update the hotness of report containers in a workspace.
     * @param workspaceId The workspace to update for.
     */
    @Transactional
    public void bulkUpdateReportContainerHotness(Long workspaceId) {
        try {
            reportContainerRepository.bulkUpdateReportContainerHotnessByWorkspace(workspaceId);
        } catch (Exception e) {
            log.error("Failed to bulk update hotness of report containers in Workspace ID:{}", workspaceId, e);
        }
    }

    /**
     * Update the priority and hotness, for time every wednesday and sunday at midnight. This is basically twice a week.
     * Priority and Hotness will be being updated on report movement in a bug anyway, therefore
     * this background task is only carried out occasionally to show the updates thanks to time.
     * Age is also only considered in weeks for this, therefore this task does not need to run frequently.
     * This background tasks updates workspace by workspace, to avoid locking too many rows at once, helping maintain a multi-tenant system.
     */
    @Scheduled(cron = "0 0 0 * * Wed,Sun")
    @Transactional
    public void recomputeReportContainerPrioritiesInWorkspaces() {
        try {
            log.info("Auto-priority background worker started running");
            //Find all workspace ids.
            List<Long> workspaceIds = workspaceRepository.findAllIds();
            //Update their things.
            for (Long workspaceId : workspaceIds) {
                bulkUpdateReportContainerHotness(workspaceId);
                bulkUpdateReportContainerPriority(workspaceId);
            }
            log.info("Auto-priority background worker finished running");
        } catch (Exception e) {
            log.error("Failed in recomputeReportContainerPrioritiesInWorkspaces", e);
        }
    }

}
