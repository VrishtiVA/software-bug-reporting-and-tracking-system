package com.example.backend.repositories.workspaces;

import com.example.backend.dto.responses.workspaces.ShortWorkspaceOverviewDTO;
import com.example.backend.dto.responses.workspaces.WorkspaceMemberDTO;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceMember;
import com.example.backend.dto.responses.workspaces.MemberWorkspaceOverviewDTO;
import com.example.backend.model.workspaces.WorkspaceRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceMemberRepository extends JpaRepository<WorkspaceMember, Long> {

    /**
     * Check if user is a member in a workspace.
     * @param workspace The workspace
     * @param user The user
     * @return true if the user is a member, otherwise false.
     */
    boolean existsByUserAndWorkspace(User user, Workspace workspace);

    /**
     * Method to find a WorkspaceMember object by the member user and workspace.
     * @param user The user
     * @param workspace The workspace
     * @return The WorkspaceMember or null, in an Optional object.
     */
    Optional<WorkspaceMember> findByUserAndWorkspace(User user, Workspace workspace);

    /**
     * Method to find a WorkspaceMember object by the user ID and workspace.
     * @param userId The user ID of the user to find the WorkspaceMember object for.
     * @param workspaceId The workspace ID of the workspace to check in.
     * @return The WorkspaceMember or null, in an Optional object, depending on existence.
     */
    @Query("""
        SELECT wm
        FROM WorkspaceMember wm
        JOIN FETCH wm.user
        WHERE wm.user.id = :user_id AND wm.workspace.id = :workspace_id
    """)
    Optional<WorkspaceMember> findByUserIdAndWorkspace(
        @Param("user_id") Long userId,
        @Param("workspace_id") Long workspaceId
    );

    @Query("""
        SELECT wm.user
        FROM WorkspaceMember wm
        WHERE wm.workspace.id = :workspace_id AND wm.user.id != :user_id
    """)
    List<User> findByWorkspaceIdAndUserIdNot(@Param("workspace_id") Long workspaceId, @Param("user_id") Long userId);

    /**
     * Find the role of a user in a workspace.
     * @param user The user
     * @param workspace The workspace.
     * @return The workspace role of the user, or null if they aren't a member in an optional object.
     */
    @Query("""
        SELECT wm.role
        FROM WorkspaceMember wm
        WHERE wm.user = :user AND wm.workspace = :workspace
    """)
    Optional<WorkspaceRole> findRoleByUserAndWorkspace(@Param("user") User user, @Param("workspace") Workspace workspace);

    /**
     * Get the workspace summary data for all workspaces a user is a member of.
     * @param member The user
     * @return All the workspace summary data in MemberWorkspaceOverviewDTO format.
     */
    @Query("""
        SELECT new com.example.backend.dto.responses.workspaces.MemberWorkspaceOverviewDTO(
            w.id,
            w.name,
            w.colour,
            w.isPublic,
            wm.role,
            (
                SELECT COUNT(*)
                FROM WorkspaceMember wm_sub
                WHERE wm_sub.workspace = w
            ),
            (
                SELECT COUNT(*)
                FROM ReportContainer rc_sub
                WHERE rc_sub.workspace = w
            )
        )
        FROM WorkspaceMember wm
        JOIN wm.workspace w
        WHERE wm.user = :member AND wm.role != com.example.backend.model.workspaces.WorkspaceRole.OWNER
    """)
    List<MemberWorkspaceOverviewDTO> findWorkspaceSummariesByMemberAsDTO(@Param("member") User member);

    @Query("""
        SELECT new com.example.backend.dto.responses.workspaces.ShortWorkspaceOverviewDTO(
            w.id,
            w.name,
            w.colour,
            w.isPublic,
            wm.role
        )
        FROM WorkspaceMember wm
        JOIN wm.workspace w
        WHERE wm.user = :user
    """)
    List<ShortWorkspaceOverviewDTO> findShortWorkspaceSummariesByUserAsDTO(@Param("user") User user);

    /**
     * Get all workspaces members for a specific workspace
     * @param workspace The workspace
     * @return The list of workspace members data in WorkspaceMembersDTO format.
     */
    @Query("""
        SELECT new com.example.backend.dto.responses.workspaces.WorkspaceMemberDTO(
            wm.user.id,
            wm.user.username,
            wm.role,
            wm.user.colour,
            wm.assignmentCount
        )
        FROM WorkspaceMember wm
        WHERE wm.workspace = :workspace
    """)
    List<WorkspaceMemberDTO> findWorkspaceMembersByWorkspaceAsDTO(@Param("workspace") Workspace workspace);

    /**
     * Get all assignable workspaces members for a specific workspace
     * @param workspace The workspace
     * @return The list of assignable workspace members data in WorkspaceMembersDTO format.
     */
    @Query("""
        SELECT new com.example.backend.dto.responses.workspaces.WorkspaceMemberDTO(
            wm.user.id,
            wm.user.username,
            wm.role,
            wm.user.colour,
            wm.assignmentCount
        )
        FROM WorkspaceMember wm
        WHERE wm.workspace = :workspace AND wm.role IN :valid_roles
    """)
    List<WorkspaceMemberDTO> findAssignableWorkspaceMembersByWorkspaceAsDTO(
        @Param("workspace") Workspace workspace,
        @Param("valid_roles") List<WorkspaceRole> validRoles
    );


    /**
     * Method to find workspace members that can be assigned to a report container.
     * @param workspaceId The workspace ID to find workspace members in.
     * @param assignedWorkspaceMembers The workspace members that have already been assigned and can be excluded.
     * @param permittedRoles The roles that the workspace member should have.
     * @return The list of assignable WorkspaceMember objects.
     */
    @Query("""
        SELECT wm
        FROM WorkspaceMember wm
        WHERE
            wm.workspace.id = :workspace_id AND
            wm NOT IN :assignees AND
            wm.role IN :permitted_roles
    """)
    List<WorkspaceMember> findByWorkspaceIdAndNotInAssigneesAndRoleInPermittedRoles(
        @Param("workspace_id") Long workspaceId,
        @Param("assignees") List<WorkspaceMember> assignedWorkspaceMembers,
        @Param("permitted_roles") List<WorkspaceRole> permittedRoles
    );

    /**
     * Bulk update assignment count for members.
     * @param workspaceMemberIds The workspace member IDs.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """          
          UPDATE workspace_member wm
          SET wm.assignment_count = (
              SELECT COUNT(*)
              FROM report_container_assignee rca
              JOIN report_container rc ON rca.report_container_id = rc.id
              WHERE
                  rca.workspace_member_id = wm.id AND
                  rc.progress != 'CLOSED'
          )
          WHERE wm.id IN (:workspace_member_ids)
    """, nativeQuery = true)
    void updateAssignmentCountForMemberIds(@Param("workspace_member_ids") List<Long> workspaceMemberIds);
}