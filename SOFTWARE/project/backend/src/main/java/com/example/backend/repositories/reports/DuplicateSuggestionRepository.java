package com.example.backend.repositories.reports;

import com.example.backend.model.reports.DuplicateSuggestion;
import com.example.backend.model.reports.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface DuplicateSuggestionRepository extends JpaRepository<DuplicateSuggestion, Long> {

    void deleteByFromReportId(Long reportId);

    void deleteByFromReportIdAndToReportId(Long fromReportId, Long toReportId);

    Optional<DuplicateSuggestion> findByFromReportAndToReport(Report fromReport, Report toReport);

    /**
     * Method to find report containers of toReports, from fromReports.
     * @param fromReportId The report to find the report containers they may point to indirectly via their toReports.
     * @return A list of report container IDs.
     */
    @Query("""
        SELECT DISTINCT rc.id
        FROM DuplicateSuggestion ds
        LEFT JOIN ds.toReport.reportContainer rc
        WHERE ds.fromReport.id = :from_report_id
    """)
    List<Long> findReportContainerIdsOfToReportsByFromReport(@Param("from_report_id") Long fromReportId);

    /**
     * Claim a report to resolve for a user where
     * <ul>
     *     <li> the report is in the same workspace, </li>
     *     <li> has outstanding duplicate suggestions, </li>
     *     <li> is not currently being resolved by another user or this has expired, </li>
     *     <li> hasn't already been skipped by the user, </li>
     *     <li> may be for a specific report container </li>
     * </ul>
     * An atomic query to prevent consistency issues or race conditions.
     * Using clear automatically to forget stale memory and ensure the claim is set.
     * @param workspaceId The workspace ID
     * @param currentTimestamp The time of claim (now)
     * @param currentUserId The user ID
     * @param expiredTimestamp If the report was claimed before this time (e.g. earlier than 5 minutes ago), its claim has expired,
     *                         therefore the report can still be claimed.
     * @param excludeReportIds The reports to not reclaim in this query.
     * @param includeReportIds The reports available to select from.
     * @return The number of rows that were updated
     */
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE report r
        JOIN (
            SELECT r_.id AS r_id
            FROM report r_
            WHERE
                r_.workspace_id = :workspace_id AND
                r_.duplicate_detection_lifecycle_state = 'OUTSTANDING'
                AND EXISTS (
                    SELECT 1
                    FROM duplicate_suggestion ds
                    WHERE ds.from_report_id = r_.id
                )
                AND (
                    r_.duplicate_suggestions_being_manually_resolved = false OR
                    r_.duplicate_suggestions_being_manually_resolved_by_user_id = :current_user_id OR
                    (
                        r_.duplicate_suggestions_being_manually_resolved_timestamp IS NOT NULL AND
                        r_.duplicate_suggestions_being_manually_resolved_timestamp < :expired_timestamp
                    )
                )
                AND (
                    :include_size = 0 OR
                    r_.id IN (:include_report_ids)
                )
                AND (
                    :exclude_size = 0 OR
                    r_.id NOT IN (:exclude_report_ids)
                )
            LIMIT 1
        ) selected_report ON selected_report.r_id = r.id
        SET r.duplicate_suggestions_being_manually_resolved = true,
            r.duplicate_suggestions_being_manually_resolved_timestamp = :current_timestamp,
            r.duplicate_suggestions_being_manually_resolved_by_user_id = :current_user_id
    """, nativeQuery = true)
    int claimToResolve(
        @Param("workspace_id") Long workspaceId,
        @Param("current_timestamp") Instant currentTimestamp,
        @Param("current_user_id") Long currentUserId,
        @Param("expired_timestamp") Instant expiredTimestamp,
        @Param("exclude_report_ids") List<Long> excludeReportIds,
        @Param("include_report_ids") List<Long> includeReportIds,
        @Param("exclude_size") int excludeSize,
        @Param("include_size") int includeSize
    );


    /**
     * Find reports that were suggested to reports within a container.
     * @param reportContainerId The ID of the report container to check in.
     * @return A list of report IDs.
     */
    @Query("""
        SELECT ds.fromReport.id
        FROM DuplicateSuggestion ds
        WHERE ds.toReport.reportContainer.id = :report_container_id
    """)
    List<Long> findReportsToIncludeInClaimSelection(@Param("report_container_id") Long reportContainerId);

    /**
     * Find the duplicate suggestions to resolve based on a users claim.
     * @param workspaceId The workspace ID user is resolving in
     * @param currentUserId The user ID of the user, to check for their claim.
     * @return A list of duplicate suggestions to resolve.
     */
    @Query(value = """
        SELECT ds.*
        FROM duplicate_suggestion ds
        JOIN report fr ON ds.from_report_id = fr.id
        JOIN report tr ON ds.to_report_id = tr.id
        JOIN (
            SELECT r.id AS r_id
            FROM report r
            JOIN duplicate_suggestion ds ON ds.from_report_id = r.id
            WHERE
                r.workspace_id = :workspace_id AND
                r.duplicate_suggestions_being_manually_resolved = true AND
                r.duplicate_suggestions_being_manually_resolved_by_user_id = :current_user_id
            LIMIT 1
        ) selected_report ON selected_report.r_id = ds.from_report_id
        ORDER BY ds.similarity_order ASC
    """, nativeQuery = true)
    List<DuplicateSuggestion> findSuggestionsToResolveByClaim(
        @Param("workspace_id") Long workspaceId,
        @Param("current_user_id") Long currentUserId
    );

    /**
     * Method to un-claim report to resolve.
     * Only allow if attempting to un-claim a claim owned by the user.
     * @param reportId The report to un-claim.
     * @param currentUserId The ID of the user trying to make the un-claim.
     */
    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE Report r
        SET r.duplicateSuggestionsBeingManuallyResolvedTimestamp = null,
            r.duplicateSuggestionsBeingManuallyResolvedByUserId = null,
            r.duplicateSuggestionsBeingManuallyResolved = false
        WHERE
            r.id = :report_id AND
            r.duplicateSuggestionsBeingManuallyResolved = true AND
            r.duplicateSuggestionsBeingManuallyResolvedByUserId = :current_user_id
    """)
    void unclaimToResolve(
        @Param("report_id") Long reportId,
        @Param("current_user_id") Long currentUserId
    );

    boolean existsByFromReportId(Long fromReportId);

    @Modifying
    @Query("""
        DELETE FROM DuplicateSuggestion ds
        WHERE
            ds.fromReport.workspace.id = :workspace_id OR
            ds.toReport.workspace.id = :workspace_id
    """)
    void deleteAllByWorkspaceId(@Param("workspace_id") Long workspaceId);
}
