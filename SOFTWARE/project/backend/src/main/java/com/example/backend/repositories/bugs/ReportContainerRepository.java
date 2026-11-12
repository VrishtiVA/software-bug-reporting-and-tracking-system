package com.example.backend.repositories.bugs;

import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.bugs.ReportContainerStability;
import com.example.backend.model.bugs.ReportContainerStatus;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import com.example.backend.repositories.bugs.projections.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportContainerRepository extends JpaRepository<ReportContainer, Long> {

    @Query("""
        SELECT rc
        FROM ReportContainer rc
        LEFT JOIN FETCH rc.reports
        WHERE rc.id = :report_container_id
    """)
    Optional<ReportContainer> findByIdWithReports(@Param("report_container_id") Long reportContainerId);

    @Query("""
        SELECT rc
        FROM ReportContainer rc
        LEFT JOIN FETCH rc.reports
        WHERE rc.id IN (:report_container_ids)
    """)
    List<ReportContainer> findReportContainersByIdWithReports(@Param("report_container_ids") List<Long> reportContainerId);

    @Modifying
    @Query("""
        UPDATE ReportContainer rc
        SET rc.stability = :stability
        WHERE rc.id = :report_container_id
    """)
    void updateStability(@Param("report_container_id") Long reportContainerId, @Param("stability") ReportContainerStability stability);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE ReportContainer rc
        SET rc.firstReportedTimestamp = (
            SELECT MIN(r.creationTimestamp)
            FROM Report r
            WHERE r.reportContainer.id = :report_container_id
        )
        WHERE rc.id = :report_container_id
    """)
    void updateFirstReportedTimestamp(@Param("report_container_id") long reportContainerId);

    @Query("""
        SELECT rc
        FROM ReportContainer rc
        WHERE rc.workspace.id = :workspace_id AND rc.stability = :stability
    """)
    List<ReportContainer> findByWorkspaceIdAndStability(
        @Param("workspace_id") Long workspaceId,
        @Param("stability") ReportContainerStability stability
    );

    @Query("""
        SELECT wcl
        FROM Report r
        JOIN r.workspaceCustomLabels rwclm
        JOIN rwclm.workspaceCustomLabel wcl
        WHERE
            r.id = :report_id AND
            wcl.id NOT IN (
                SELECT _wcl.id
                FROM ReportContainer rc
                JOIN rc.workspaceCustomLabels rcwclm
                JOIN rcwclm.workspaceCustomLabel _wcl
                WHERE rc.id = :report_container_id
            )
    """)
    List<WorkspaceCustomLabel> findWorkspaceCustomLabelsInReportNotInReportContainer(
        @Param("report_id") Long reportId,
        @Param("report_container_id") Long reportContainerId
    );

    @Query("""
        SELECT wcl
        FROM ReportContainer rc
        JOIN rc.workspaceCustomLabels rcwclm
        JOIN rcwclm.workspaceCustomLabel wcl
        WHERE
            rc.id = :from_report_container_id AND
            wcl.id NOT IN (
                SELECT _wcl.id
                FROM ReportContainer _rc
                JOIN _rc.workspaceCustomLabels _rcwclm
                JOIN _rcwclm.workspaceCustomLabel _wcl
                WHERE _rc.id = :to_report_container_id
            )
    """)
    List<WorkspaceCustomLabel> findWorkspaceCustomLabelsInReportContainerNotInReportContainer(
        @Param("from_report_container_id") Long fromReportContainerId,
        @Param("to_report_container_id") Long toReportContainerId
    );

    /**
     * Update the bugs total severity rating and total user-impact rating.
     * @param reportContainerId The ID of the report container to do this for.
     */
    @Modifying
    @Query(value = """
        UPDATE report_container rc
        JOIN (
            SELECT
                r.report_container_id,
                SUM(r.severity_rating) AS total_severity_rating,
                SUM(r.user_impact_rating) AS total_user_impact_rating,
                COUNT(r.id) AS frequency
            FROM report r
            WHERE r.report_container_id = :report_container_id
        ) results ON rc.id = results.report_container_id
        SET rc.total_severity_rating = results.total_severity_rating,
            rc.total_user_impact_rating = results.total_user_impact_rating,
            rc.frequency = results.frequency
        WHERE rc.id = :report_container_id
    """, nativeQuery = true)
    void updateReportContainerMetricTotalsById(@Param("report_container_id") Long reportContainerId);

    @Modifying
    @Query("""
        UPDATE ReportContainer rc
        SET rc.hotness = :hotness
        WHERE rc.id = :report_container_id
    """)
    void updateHotness(@Param("report_container_id") long reportContainerId, @Param("hotness") double hotness);

    @Modifying
    @Query(value = """
        UPDATE report_container rc
        SET priority = (
            (rc.total_severity_rating / rc.frequency) * :severity_weight +
            (rc.total_user_impact_rating / rc.frequency) * :user_impact_weight +
            LOG2(rc.frequency) * :frequency_weight +
            (
                (LOG2(FLOOR(DATEDIFF(NOW(), rc.first_reported_timestamp)/7) +1 )) *
                (POWER( ((rc.total_severity_rating / rc.frequency) + (rc.total_user_impact_rating / rc.frequency)) / 10, 2 ) + :week_age_weight )
            ) * :age_multiplier_base
        ) * 20
        WHERE rc.id = :report_container_id
    """, nativeQuery = true)
    void updateReportContainerPriority(
        @Param("report_container_id") Long reportContainerId,
        @Param("severity_weight") double severityWeight,
        @Param("user_impact_weight") double userImpactWeight,
        @Param("frequency_weight") double frequencyWeight,
        @Param("week_age_weight") double weekAgeWeight,
        @Param("age_multiplier_base") double ageMultiplierBase
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        UPDATE report_container rc
        SET priority = (
            (rc.total_severity_rating / rc.frequency) * :severity_weight +
            (rc.total_user_impact_rating / rc.frequency) * :user_impact_weight +
            LOG2(rc.frequency) * :frequency_weight +
            (
                (LOG2( FLOOR(DATEDIFF(NOW(), rc.first_reported_timestamp)/7) +1 )) *
                (POWER( ((rc.total_severity_rating / rc.frequency) + (rc.total_user_impact_rating / rc.frequency)) / 10, 2) + :week_age_weight )
            ) * :age_multiplier_base
        ) * 20
        WHERE rc.workspace_id = :workspace_id AND rc.progress != 'CLOSED'
    """, nativeQuery = true)
    void bulkUpdateReportContainerPrioritybyWorkspace(
            @Param("workspace_id") Long workspaceId,
            @Param("severity_weight") double severityWeight,
            @Param("user_impact_weight") double userImpactWeight,
            @Param("frequency_weight") double frequencyWeight,
            @Param("week_age_weight") double weekAgeWeight,
            @Param("age_multiplier_base") double ageMultiplierBase
    );

    /**
     * Method to bulk update the hotness of report containers/bugs in a workspace.
     * Note: Taking log of greatest from weeks and 2 to avoid division by 0 error.
     * @param workspaceId The workspace ID to update bugs hotness for.
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        UPDATE report_container rc
        SET hotness = (
            rc.frequency / LOG2( GREATEST(FLOOR(DATEDIFF(NOW(), rc.first_reported_timestamp)/7), 2) )
        )
        WHERE rc.workspace_id = :workspace_id AND rc.progress != 'CLOSED'
    """, nativeQuery = true)
    void bulkUpdateReportContainerHotnessByWorkspace(
        @Param("workspace_id") Long workspaceId
    );

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
        UPDATE report_container rc
        LEFT JOIN (
            SELECT r.report_container_id AS report_container_id, COUNT(DISTINCT ds.from_report_id) AS incoming_duplicate_suggestions_count
            FROM report r
            LEFT JOIN duplicate_suggestion ds ON ds.to_report_id = r.id
            WHERE report_container_id IN (:for_report_container_ids)
            GROUP BY report_container_id
        ) results ON rc.id = results.report_container_id
        SET rc.incoming_duplicate_suggestions_count = IFNULL(results.incoming_duplicate_suggestions_count, 0)
        WHERE rc.id = results.report_container_id
    """, nativeQuery = true)
    void bulkUpdateReportContainerIncomingDuplicateSuggestionsCount(
        @Param("for_report_container_ids") List<Long> forReportContainerIds
    );

    @Query("""
        SELECT
            CASE
                WHEN rc.priority >= :p0lb THEN "P0"
                WHEN rc.priority >= :p1lb THEN "P1"
                WHEN rc.priority >= :p2lb THEN "P2"
                WHEN rc.priority >= :p3lb THEN "P3"
                ELSE "P4"
            END AS priorityLabel,
            COUNT(rc.id) AS bugCount
        FROM ReportContainer rc
        WHERE
            rc.workspace.id = :workspace_id AND
            rc.stability = :stability AND
            rc.progress != :exclude_progress
        GROUP BY priorityLabel
    """)
    List<PriorityCategoryDistributionProjection> findPriorityCategoryDistributionByWorkspaceId(
            @Param("workspace_id") Long workspaceId,
            @Param("exclude_progress") ReportContainerProgress excludeProgress,
            @Param("stability") ReportContainerStability stability,
            Double p0lb, Double p1lb, Double p2lb, Double p3lb
    );

    @Query("""
        SELECT
            wcl.name AS workspaceCustomLabelName,
            wcl.colour AS workspaceCustomLabelColour,
            COUNT(rc.id) AS usageCount
        FROM ReportContainer rc
        JOIN rc.workspaceCustomLabels rcwclm
        JOIN rcwclm.workspaceCustomLabel wcl
        WHERE
            rc.workspace.id = :workspace_id AND
            rc.stability = :stability AND
            rc.progress != :exclude_progress
        GROUP BY wcl.name, wcl.colour
    """)
    List<WorkspaceCustomLabelDistributionProjection> findWorkspaceCustomLabelUsageInReportContainersDistributionByWorkspaceId(
        @Param("workspace_id") Long workspaceId,
        @Param("exclude_progress") ReportContainerProgress excludeProgress,
        @Param("stability") ReportContainerStability stability
    );

    @Query("""
        SELECT
            rc.progress AS progress,
            COUNT(rc.id) AS bugCount
        FROM ReportContainer rc
        WHERE
            rc.workspace.id = :workspace_id AND
            rc.stability = :stability AND
            rc.progress != :exclude_progress
        GROUP BY progress
    """)
    List<ProgressDistributionProjection> findReportContainerProgressDistributionByWorkspaceId(
        @Param("workspace_id") Long workspaceId,
        @Param("exclude_progress") ReportContainerProgress excludeProgress,
        @Param("stability") ReportContainerStability stability
    );
    @Query("""
        SELECT
            rc.status AS status,
            COUNT(rc.id) AS bugCount
        FROM ReportContainer rc
        WHERE
            rc.workspace.id = :workspace_id AND
            rc.stability = :stability AND
            rc.progress != :exclude_progress
        GROUP BY status
    """)
    List<StatusDistributionProjection> findReportContainerStatusDistributionByWorkspaceId(
        @Param("workspace_id") Long workspaceId,
        @Param("exclude_progress") ReportContainerProgress excludeProgress,
        @Param("stability") ReportContainerStability stability
    );

    @Query("""
        SELECT COUNT(rc.id)
        FROM ReportContainer rc
        WHERE
            rc.workspace.id = :workspace_id AND
            rc.stability = :stability AND
            rc.progress != :exclude_progress
    """)
    Long countActiveReportContainersByWorkspaceId(
            @Param("workspace_id") Long workspaceId,
            @Param("exclude_progress") ReportContainerProgress excludeProgress,
            @Param("stability") ReportContainerStability stability
    );

    Long countByWorkspaceAndStatusAndStability(Workspace workspace, ReportContainerStatus status, ReportContainerStability stability);


    @Query("""
        SELECT
            FLOOR(rc.priority / 5) * 5 AS priorityBucketLB,
            FLOOR(rc.frequency / 5) * 5 AS frequencyBucketLB,
            COUNT(rc.id) AS bugCount
        FROM ReportContainer rc
        WHERE
            rc.workspace.id = :workspace_id AND
            rc.stability = :stability AND
            rc.progress != :exclude_progress
        GROUP BY priorityBucketLB, frequencyBucketLB
    """)
    List<PriorityFrequencyBugCountBucketProjection> findPriorityFrequencyBugCountBucketsByWorkspaceId(
        @Param("workspace_id") Long workspaceId,
        @Param("exclude_progress") ReportContainerProgress excludeProgress,
        @Param("stability") ReportContainerStability stability
    );

    @Modifying
    @Query("""
        DELETE FROM ReportContainer rc
        WHERE rc.workspace.id = :workspace_id
    """)
    void deleteAllByWorkspaceId(@Param("workspace_id") Long workspaceId);

    @Modifying
    @Query("""
        UPDATE ReportContainer rc
        SET rc.primaryReport = NULL
        WHERE rc.workspace.id = :workspace_id
    """)
    void breakReportRelationsByWorkspaceId(@Param("workspace_id") Long workspaceId);
}
