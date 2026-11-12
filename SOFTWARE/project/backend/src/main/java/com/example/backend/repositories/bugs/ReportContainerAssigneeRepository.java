package com.example.backend.repositories.bugs;

import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.bugs.ReportContainerAssignee;
import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.WorkspaceMember;
import com.example.backend.repositories.bugs.projections.ReportContainerAssigneeCountDataProjection;
import com.example.backend.repositories.bugs.projections.ReportContainerUserAssigneeProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportContainerAssigneeRepository extends JpaRepository<ReportContainerAssignee, Long> {

    /**
     * Method to check if a user is assigned to a report container
     * @param reportContainerId The ID of the report container
     * @param workspaceMemberId The ID of the user workspace membership object
     * @return true if they are assigned, otherwise false.
     */
    boolean existsByReportContainerIdAndWorkspaceMemberId(Long reportContainerId, Long workspaceMemberId);

    Optional<ReportContainerAssignee> findByReportContainerIdAndWorkspaceMemberId(long reportContainerId, long workspaceMemberId);

    /**
     * Find all report container allocations for member in a workspace.
     * @param workspaceMemberId The ID of the user workspace membership object.
     * @return List of ReportContainerAssignee assignments.
     */
    List<ReportContainerAssignee> findByWorkspaceMemberId(Long workspaceMemberId);

    /**
     * Delete all report container allocations for member in a workspace.
     * @param workspaceMemberId The ID of the user workspace membership object.
     */
    void deleteByWorkspaceMemberId(Long workspaceMemberId);

    @Query("""
        SELECT rca.workspaceMember.user
        FROM ReportContainerAssignee rca
        WHERE rca.reportContainer.id = :report_container_id
    """)
    List<User> findUsersByReportContainerId(
        @Param("report_container_id") Long reportContainerId
    );

    @Query("""
        SELECT rca.workspaceMember.user
        FROM ReportContainerAssignee rca
        WHERE
            rca.reportContainer.id = :report_container_id AND
            rca.workspaceMember.user.id != :user_id
    """)
    List<User> findUsersByReportContainerIdAndUserIdNot(
        @Param("report_container_id") Long reportContainerId,
        @Param("user_id") Long userId
    );

    /**
     * Find all workspace members assigned to a report container.
     * @param reportContainerId The ID of the report container to look for assignments of.
     * @return List of WorkspaceMember objects.
     */
    @Query("""
        SELECT rca.workspaceMember
        FROM ReportContainerAssignee rca
        WHERE rca.reportContainer.id = :report_container_id
    """)
    List<WorkspaceMember> findWorkspaceMembersByReportContainerId(@Param("report_container_id") long reportContainerId);

    @Modifying
    void deleteByReportContainerIdAndWorkspaceMemberId(long reportContainerId, long workspaceMemberId);

    @Modifying
    @Query("""
        DELETE FROM ReportContainerAssignee rca
        WHERE rca.reportContainer.id = :report_container_id AND rca.workspaceMember.id IN (:workspace_member_ids)
    """)
    void deleteAllByReportContainerIdAndWorkspaceMemberIds(
        @Param("report_container_id") Long reportContainerId,
        @Param("workspace_member_ids") List<Long> workspaceMemberIds
    );

    @Query(value = """
        SELECT rc.id AS reportContainerId, COUNT(rca.report_container_id) AS assigneeCount
        FROM report_container rc
        LEFT JOIN report_container_assignee rca ON rc.id = rca.report_container_id
        WHERE rc.workspace_id = :workspace_id AND rc.stability = :stability
        GROUP BY rc.id
    """, nativeQuery = true)
    List<ReportContainerAssigneeCountDataProjection> countByWorkspaceIdAndStability(@Param("workspace_id") Long workspaceId, @Param("stability") String stability);

    @Query(value = """
        SELECT rc.id AS reportContainerId, COUNT(rca.report_container_id) AS assigneeCount
        FROM report_container rc
        LEFT JOIN report_container_assignee rca ON rc.id = rca.report_container_id
        WHERE (:workspace_id IS NULL OR rc.workspace_id = :workspace_id) AND
              rc.stability = :stability AND
              rc.id IN :report_container_ids
        GROUP BY rc.id
    """, nativeQuery = true)
    List<ReportContainerAssigneeCountDataProjection> countByWorkspaceIdAndStabilityAndInReportContainerIds(
        @Param("workspace_id") Long workspaceId,
        @Param("stability") String stability,
        @Param("report_container_ids") List<Long> reportContainerIds);

    @Query(value = """
        SELECT wm.*
        FROM report_container_assignee rca
        LEFT JOIN workspace_member wm ON wm.id = rca.workspace_member_id
        WHERE rca.report_container_id = :report_container_id
        LIMIT 3
    """, nativeQuery = true)
    List<WorkspaceMember> findTop3AssigneesAsWorkspaceMemberByReportContainer(@Param("report_container_id") Long reportContainerId);

    @Query("""
        SELECT rca.id AS reportContainerId, rca.workspaceMember.user AS user
        FROM ReportContainerAssignee rca
        WHERE rca.reportContainer.id IN (:report_container_ids)
    """)
    List<ReportContainerUserAssigneeProjection> findAssignedUsersPerReportContainerId(@Param("report_container_ids") List<Long> reportContainerIds);

    @Query("""
        SELECT rca.reportContainer
        FROM ReportContainerAssignee rca
        WHERE
            rca.workspaceMember.user.id = :user_id AND
            (:progress_filters IS NULL OR rca.reportContainer.progress IN :progress_filters)
    """)
    Page<ReportContainer> findPaginatedReportContainersByWorkspaceMemberUserId(
            @Param("user_id") Long userId,
            @Param("progress_filters") List<ReportContainerProgress> progressFilters,
            Pageable pageable
    );

    @Query("""
        SELECT rca.reportContainer
        FROM ReportContainerAssignee rca
        WHERE
            rca.workspaceMember.user.id = :user_id AND
            rca.workspaceMember.workspace.id = :workspace_id AND
            (:progress_filters IS NULL OR rca.reportContainer.progress IN :progress_filters)
    """)
    Page<ReportContainer> findPaginatedReportContainersByWorkspaceMemberUserIdAndWorkspaceId(
        @Param("user_id") Long userId,
        @Param("workspace_id") Long workspaceId,
        @Param("progress_filters") List<ReportContainerProgress> progressFilters,
        Pageable pageable
    );

    @Modifying
    @Query("""
        DELETE FROM ReportContainerAssignee rca
        WHERE rca.reportContainer.workspace.id = :workspace_id
    """)
    void deleteAllByWorkspaceId(@Param("workspace_id") Long workspaceId);

}