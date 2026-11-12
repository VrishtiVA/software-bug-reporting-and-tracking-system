package com.example.backend.services.workspaces;

import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.bugs.ReportContainerStability;
import com.example.backend.model.reports.Report;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.repositories.workspaces.WorkspaceRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/** Service to manage workspace permission logic. */
@Service
public class WorkspacePermissionsService {

    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private WorkspaceRepository workspaceRepository;

    /* ---------- Workspaces ---------- */

    public static final int WORKSPACES_OWNED_LIMIT = 5;
    public static final int WORKSPACE_CUSTOM_LABEL_LIMIT = 15;
    public static final int WORKSPACE_MEMBERS_LIMIT = 50;

    /**
     * Method to check if user has reached the maximum number of workspaces
     * that they are allowed to create.
     * @param user The user.
     * @return true if they have reached maximum, otherwise false.
     */
    public boolean hasUserReachedMaxOwnedWorkspaces(User user) {
        return workspaceRepository.countByOwner(user) >= WORKSPACES_OWNED_LIMIT;
    }

    /**
     * Method to find out if user is allowed to view a workspace page or not.
     * The user is allowed to view if the workspace is public, otherwise
     * if they are a member of the workspace.
     * @param user The user
     * @param workspace The workspace
     * @return true if they are, otherwise false.
     */
    public boolean canUserViewWorkspace(User user, Workspace workspace) {
        if (workspace.isPublic()) {
            return true;
        } else {
            return workspaceService.isUserWorkspaceMember(user, workspace);
        }
    }

    /**
     * Method to find out if the user is allowed to delete a workspace or not.
     * They can only delete a workspace if they are the owner of the workspace.
     * @param user The user
     * @param workspace The workspace
     * @return true if they can, otherwise false.
     */
    public boolean canUserDeleteWorkspace(User user, Workspace workspace) {
        return workspace.getOwner().equals(user);
    }

    /**
     * Method to find out if the user is allowed to configure a workspace or not.
     * They can only configure a workspace if they are the owner or an admin of the workspace.
     * @param userRole The users role in the workspace.
     * @return true if they can, otherwise false.
     */
    public boolean canUserConfigureWorkspace(WorkspaceRole userRole) {
        return userRole == WorkspaceRole.OWNER || userRole == WorkspaceRole.ADMIN;
    }

    /**
     * Method to find out if user can make a report in a workspace.
     * This is usually if the workspace is public, they can, or if they are a workspace member.
     * @param workspace The workspace
     * @param role The role of the user in the workspace or null if they aren't a member.
     * @return true if they can make a report in the workspace, otherwise false.
     */
    public boolean canUserMakeReportInWorkspace(Workspace workspace, WorkspaceRole role) {
        if (workspace.isPublic()) return true;
        return role != null;
    }

    /**
     * Method to check if a user is allowed to resolve duplicates in a workspace.
     * This is usually if the user is the OWNER, and ADMIN, or DEVELOPER.
     * @param userRole The users role in the workspace.
     * @return true if they can, otherwise false
     */
    public boolean canUserResolveDuplicatesInWorkspace(WorkspaceRole userRole) {
        return (
            userRole == WorkspaceRole.OWNER ||
            userRole == WorkspaceRole.ADMIN ||
            userRole == WorkspaceRole.DEVELOPER
        );
    }

    /**
     * Method to check if a user is allowed to resolve duplicates in a workspace.
     * @param workspace The workspace
     * @param user The user
     * @return true if they can, otherwise false
     */
    public boolean canUserResolveDuplicatesInWorkspace(Workspace workspace, User user) {
        WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, user);
        return canUserResolveDuplicatesInWorkspace(userRole);
    }

    /* ---------- Workspaces via Reports ---------- */

    /**
     * Method to check if the user can visit the workspace for their report.
     * This is if the user is a member of the workspace or the workspace of the report is public.
     * @param user The user to check for.
     * @param report The report to check if the user can visit the workspace of.
     * @return true if they can, otherwise false.
     */
    public boolean isReportWorkspaceVisitableByUser(User user, Report report) {
        return canUserViewWorkspace(user, report.getWorkspace());
    }

    /* ---------- Workspace Report Containers ---------- */

    public boolean canUserViewReportContainer(ReportContainer reportContainer) {
        return reportContainer.getStability() == ReportContainerStability.STABLE;
    }

    /**
     * Method to dictate whether a certain workspace role can update bug details of a bug.
     * @param workspaceRole The workspace role in question.
     * @return true if they can, otherwise false.
     */
    public boolean canRoleUpdateBugTrackingDetails(WorkspaceRole workspaceRole) {
        return !(workspaceRole == null || workspaceRole == WorkspaceRole.REPORTER);
    }

    /**
     * Method to find roles that a user can edit assignments for.
     * Using a switch case statement without breaks, to allow fall through and add the other roles to the list too for certain roles.
     * This is to dictate the following business logic:
     * <ul>
     *     <li> Developers can only edit developer assignments, but not admin/owner assignments. </li>
     *     <li> Admins can edit developer or admin assignments, but not owner assignments. </li>
     *     <li> Owners can edit all assignments. </li>
     * </ul>
     * @param workspaceRole The workspace role to find roles that they can edit.
     * @return A list of workspace roles that a user can edit assignments for.
     */
    public List<WorkspaceRole> findRolesUserCanEditAssignmentsFor(WorkspaceRole workspaceRole) {
        List<WorkspaceRole> roles = new ArrayList<>();
        switch (workspaceRole) {
            case WorkspaceRole.OWNER:
                roles.add(WorkspaceRole.OWNER);
            case WorkspaceRole.ADMIN:
                roles.add(WorkspaceRole.ADMIN);
            case WorkspaceRole.DEVELOPER:
                roles.add(WorkspaceRole.DEVELOPER);
            default:
                break;
        }
        return roles;
    }

}
