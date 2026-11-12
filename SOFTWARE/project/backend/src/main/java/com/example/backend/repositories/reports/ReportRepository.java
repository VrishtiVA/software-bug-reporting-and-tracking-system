package com.example.backend.repositories.reports;

import com.example.backend.model.reports.DuplicateDetectionLifecycleState;
import com.example.backend.model.reports.Report;
import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.workspaces.Workspace;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    /**
     * Method to count the number of reports with a specific status in a workspace.
     * @param workspace The workspace to search in.
     * @param state The state of the report wanted.
     * @return the number of reports found.
     */
    int countByWorkspaceAndDuplicateDetectionLifecycleState(Workspace workspace, DuplicateDetectionLifecycleState state);

    int countByReportContainerId(long reportContainerId);

    @Query("""
        SELECT DISTINCT r
        FROM Report r
        LEFT JOIN FETCH r.reportContainer rc
        LEFT JOIN FETCH rc.reports
        WHERE r.id = :report_id
    """)
    Optional<Report> findWithReportContainerAndReports(@Param("report_id") long reportId);

    @Query("""
        SELECT DISTINCT r
        FROM Report r
        LEFT JOIN FETCH r.reportContainer rc
        WHERE r.id = :report_id
    """)
    Optional<Report> findWithReportContainer(@Param("report_id") long reportId);

    @Query("""
        SELECT r
        FROM Report r
        LEFT JOIN FETCH r.user
        WHERE r.id = :report_id
    """)
    Optional<Report> findByIdWithUser(@Param("report_id") long reportId);

    @Query("""
        SELECT r.reportContainer.id
        FROM Report r
        WHERE r.id = :report_id
    """)
    Optional<Long> findReportContainerIdByReport(@Param("report_id") long reportId);

    /**
     * Find reports in report container, paginated.
     * @param reportContainerId The report container to find reports in.
     * @param pageable Pagination configuration.
     * @return Page of reports.
     */
    Page<Report> findByReportContainerId(Long reportContainerId, Pageable pageable);
    Page<Report> findByReportContainerIdAndStepsToReproduceIsNotNullAndStepsToReproduceIsNot(Long reportContainerId, String empty, Pageable pageable);
    Page<Report> findByReportContainerIdAndEnvironmentIsNotNullAndEnvironmentIsNot(Long reportContainerId, String empty, Pageable pageable);
    Page<Report> findByReportContainerIdAndExpectedResultIsNotNullAndExpectedResultIsNot(Long reportContainerId, String empty, Pageable pageable);
    Page<Report> findByReportContainerIdAndActualResultIsNotNullAndActualResultIsNot(Long reportContainerId, String empty, Pageable pageable);
    Page<Report> findByReportContainerIdAndFixSuggestionsIsNotNullAndFixSuggestionsIsNot(Long reportContainerId, String empty, Pageable pageable);
    Page<Report> findByReportContainerIdAndAdditionalCommentsIsNotNullAndAdditionalCommentsIsNot(Long reportContainerId, String empty, Pageable pageable);

    /**
     * Query to check that none of the reports with these ID's don't have these states.
     * @param ids The reports with these IDs to check.
     * @param states The states they should have.
     * @return true if meets condition, otherwise false.
     */
    @Query("""
       SELECT count(r) = 0
       FROM Report r
       WHERE r.id IN :ids AND r.duplicateDetectionLifecycleState NOT IN :states
    """)
    boolean isSampleReportInitialisationComplete(@Param("ids") List<Long> ids, @Param("states") List<DuplicateDetectionLifecycleState> states);

    @Modifying
    @Query("""
        UPDATE Report r
        SET r.vectorEmbedding = :report_vector_embedding,
            r.embeddingTimestamp = :embedding_timestamp
        WHERE r.id = :report_id
    """)
    void updateVectorEmbedding(@Param("report_id") long reportId, @Param("report_vector_embedding") byte[] vectorEmbedding, @Param("embedding_timestamp") Instant embeddingTimestamp);

    @Modifying
    @Query("""
        UPDATE Report r
        SET r.duplicateDetectionLifecycleState = :state
        WHERE r.id = :report_id
    """)
    void updateDuplicateDetectionLifecycleState(@Param("report_id") long reportId, @Param("state") DuplicateDetectionLifecycleState state);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Report r
        SET r.duplicateDetectionLifecycleState = :state
        WHERE r.reportContainer.id = :report_container_id
    """)
    void bulkUpdateDuplicateDetectionLifecycleStateByReportContainer(@Param("report_container_id") long reportContainerId, @Param("state") DuplicateDetectionLifecycleState state);

    @Modifying
    @Query("""
        UPDATE Report r
        SET r.reportContainer = :report_container,
            r.duplicateDetectionLifecycleState = :state
        WHERE r.id = :report_id
    """)
    void updateReportContainerAndDuplicateDetectionLifecycleState(@Param("report_id") long reportId, @Param("report_container") ReportContainer reportContainer, @Param("state") DuplicateDetectionLifecycleState state);

    @Modifying
    @Query("""
        UPDATE Report r
        SET r.reportContainer = :report_container
        WHERE r.id = :report_id
    """)
    void updateReportContainer(@Param("report_id") long reportId, @Param("report_container") ReportContainer reportContainer);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Report r
        SET r.reportContainer = :to_report_container
        WHERE r.reportContainer = :from_report_container
    """)
    void updateReportContainerFromReportContainer(@Param("from_report_container") ReportContainer fromReportContainer, @Param("to_report_container") ReportContainer toReportContainer);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
        UPDATE Report r
        SET r.reportContainer = :to_report_container,
            r.duplicateDetectionLifecycleState = :state
        WHERE r.reportContainer = :from_report_container
    """)
    void updateReportContainerAndDuplicateDetectionLifecycleStateFromReportContainer(
        @Param("from_report_container") ReportContainer fromReportContainer,
        @Param("to_report_container") ReportContainer toReportContainer,
        @Param("state") DuplicateDetectionLifecycleState state
    );

    /**
     * Method to fetch a page of reports by user, with its ReportContainer and Workspace loaded.
     * Used left join fetch on report container as the report may not have a report container, but
     * that shouldn't remove the report from the results.
     * @param userId The user to find reports from.
     * @param pageable The pagination configuration.
     * @return The page of reports by the user, with its ReportContainer and Workspace loaded.
     */
    @Query("""
        SELECT r
        FROM Report r
        LEFT JOIN FETCH r.reportContainer
        JOIN FETCH r.workspace
        WHERE r.user.id = :user_id
    """)
    Page<Report> findPaginatedReportsByUserWithWorkspaceAndReportContainer(
        @Param("user_id") Long userId,
        Pageable pageable
    );

    @Modifying
    @Query("""
        DELETE FROM Report r
        WHERE r.workspace.id = :workspace_id
    """)
    void deleteAllByWorkspaceId(@Param("workspace_id") Long workspaceId);

    @Modifying
    @Query("""
        UPDATE Report r
        SET r.reportContainer = NULL
        WHERE r.workspace.id = :workspace_id
    """)
    void breakReportContainerRelationsByWorkspaceId(@Param("workspace_id") Long workspaceId);
}
