package com.example.backend.repositories.workspaces;

import com.example.backend.dto.responses.workspaces.WorkspaceCustomLabelDTO;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.dto.responses.workspaces.OwnedWorkspaceOverviewDTO;
import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkspaceRepository extends JpaRepository<Workspace, Long> {

    List<Workspace> findByOwner(User user);

    @Query("""
        SELECT w
        FROM Workspace w
        LEFT JOIN FETCH w.workspaceCustomLabels
        WHERE w.id = :workspace_id
    """)
    Optional<Workspace> findByIdWithCustomLabels(@Param("workspace_id") Long id);

    /**
     * Method to find out if a methods already has a custom label with a certain name.
     * @param workspaceId The workspace ID
     * @param labelName The name of the custom workspace label.
     * @return true if it does exist, otherwise false.
     */
    boolean existsByIdAndWorkspaceCustomLabelsName(long workspaceId, String labelName);

    /**
     * Method to find out how many workspaces a user owns.
     * @param user The user
     * @return The number of workspaces owned by the user.
     */
    long countByOwner(User user);

    /* ---------- Fetching in DTO formats ---------- */

    /**
     * Get the workspace summary data for all workspaces a user is the owner of.
     * Using subqueries and indexing to find quickly.
     * @param owner The user
     * @return All the workspace summary data in OwnedWorkspaceOverviewDTO format.
     */
    @Query("""
        SELECT new com.example.backend.dto.responses.workspaces.OwnedWorkspaceOverviewDTO(
            w.id,
            w.name,
            w.colour,
            w.isPublic,
            (
                SELECT COUNT(*)
                FROM WorkspaceMember wm
                WHERE wm.workspace = w
            ),
            (
                SELECT COUNT(*)
                FROM ReportContainer rc
                WHERE rc.workspace = w
            )
        )
        FROM Workspace w
        WHERE w.owner = :owner
    """)
    List<OwnedWorkspaceOverviewDTO> findWorkspaceSummariesByOwnerAsDTO(@Param("owner") User owner);

    /**
     * Get a list of all workspace custom labels for a workspace.
     * @param workspace The workspace
     * @return A list of all workspace custom labels in WorkspaceCustomLabelDTO format.
     */
    @Query(value = """
        SELECT new com.example.backend.dto.responses.workspaces.WorkspaceCustomLabelDTO(
            wcl.name,
            wcl.colour
        )
        FROM Workspace w
        JOIN w.workspaceCustomLabels wcl
        WHERE w = :workspace
    """)
    List<WorkspaceCustomLabelDTO> findWorkspaceCustomLabelsByWorkspace(@Param("workspace") Workspace workspace);

    @Query("""
        SELECT wcl
        FROM Workspace w
        JOIN w.workspaceCustomLabels wcl
        WHERE w = :workspace AND wcl.name = :labelName
    """)
    Optional<WorkspaceCustomLabel> findWorkspaceCustomLabelByWorkspaceAndWorkspaceCustomLabelName(@Param("workspace") Workspace workspace, @Param("labelName") String labelName);

    @Query("""
        SELECT w.id
        FROM Workspace w
    """)
    List<Long> findAllIds();

}
