package com.example.backend.controllers;

import com.example.backend.dto.requests.bugs.FiltersBugSearchRequest;
import com.example.backend.dto.requests.bugs.UpdateAssigneesRequest;
import com.example.backend.dto.responses.ErrorResponse;
import com.example.backend.dto.responses.bugs.*;
import com.example.backend.dto.responses.workspaces.WorkspaceMemberDTO;
import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.bugs.ReportContainerAssignee;
import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceMember;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.services.bugs.ReportContainerActivityService;
import com.example.backend.services.bugs.ReportContainerService;
import com.example.backend.services.users.NotificationService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspacePermissionsService;
import com.example.backend.services.workspaces.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("api")
public class ReportContainerAssigneeController {

    @Autowired
    private UserService userService;
    @Autowired
    private WorkspacePermissionsService workspacePermissionsService;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private ReportContainerService reportContainerService;
    @Autowired
    private ReportContainerActivityService reportContainerActivityService;
    @Autowired
    private NotificationService notificationService;

    private static final Logger log = LoggerFactory.getLogger(ReportContainerAssigneeController.class);

    /* --------------- View Assignments Related Endpoints --------------- */

    /**
     * Request method to get all report containers that are assigned to a user potentially in a workspace.
     * This method is a POST method so that it can make use of the request body.
     * @param authentication The user authentication object
     * @param workspaceId The workspace ID, which may be null, to fetch assignable members in too.
     * @param assigneeUserId The ID of the user to fetch assignments for.
     * @param progressFilters The progress filters
     * @param page The page to return
     * @param size The size of the page to return
     * @return A page of report container/bug results.
     */
    @PostMapping("/bugs/assignments")
    public ResponseEntity<?> getAssignedReportContainers(
        Authentication authentication,
        @RequestParam(value = "workspace_id", required = false) Long workspaceId,
        @RequestParam(value = "user_id", required = false) Long assigneeUserId,
        @RequestBody(required = false) FiltersBugSearchRequest progressFilters,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Take filters
            List<ReportContainerProgress> includeProgress;
            if (progressFilters == null) includeProgress = null;
            else includeProgress = progressFilters.progress() != null && !progressFilters.progress().isEmpty() ? progressFilters.progress() : null;
            Pageable pageable = PageRequest.of(page, size, Sort.by("reportContainer.priority").descending());

            if (workspaceId == null) {
                //If no workspace ID is provided, fetch all assignments for current user paginated.
                Page<ReportContainerOverviewDTO> assignmentResults = reportContainerService.findPaginatedReportContainersAssignedToUserAsDTO(
                    null,
                    currentUser.getId(),
                    includeProgress,
                    pageable
                );
                //Return assigned bug results, there is no need to additionally return workspace members here, as no workspace was selected.
                return new ResponseEntity<>(
                    AssignedReportContainersResponse.fromData(
                        PaginatedBugOverviewResponse.fromPage(assignmentResults),
                        null
                    ),
                    HttpStatus.OK
                );
            } else {
                //If workspace ID is provided, check that it exists, otherwise return not found.
                Workspace workspace = workspaceService.findWorkspaceById(workspaceId).orElse(null);
                if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
                //Ensure user is a member of this workspace, and can view its allocations.
                if (!workspacePermissionsService.canUserViewWorkspace(currentUser, workspace)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;

                Page<ReportContainerOverviewDTO> assignmentResults;
                if (assigneeUserId == null) {
                    //If no user id provided, fetch bugs assigned to current user in this workspace page.
                    assignmentResults = reportContainerService.findPaginatedReportContainersAssignedToUserAsDTO(
                        workspaceId,
                        currentUser.getId(),
                        includeProgress,
                        pageable
                    );
                } else {
                    //Fetch bugs assigned to provided user in this workspace page.
                    assignmentResults = reportContainerService.findPaginatedReportContainersAssignedToUserAsDTO(
                        workspaceId,
                        assigneeUserId,
                        includeProgress,
                        pageable
                    );
                }
                //Fetch the other assignable members of the workspace.
                List<WorkspaceMemberDTO> workspaceAssignableMembers = workspaceService.findAssignableWorkspaceMembersAsDTO(workspace);

                //Return with paginated assignments, and selectable members.
                return new ResponseEntity<>(
                    AssignedReportContainersResponse.fromData(
                        PaginatedBugOverviewResponse.fromPage(assignmentResults),
                        workspaceAssignableMembers
                    ),
                    HttpStatus.OK
                );
            }

        } catch (Exception e) {
            log.error("Failed in GET /bugs/assignments", e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /* --------------- Bug Assignment Related Endpoints --------------- */

    @GetMapping("/workspaces/{id}/bugs/{bug_id}/assignees")
    public ResponseEntity<?> getReportContainerAssigneeAndAssignableMembers(
            @PathVariable("id") Long id,
            @PathVariable("bug_id") Long bugId,
            Authentication authentication
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Ensure bug exists.
            ReportContainer reportContainer = reportContainerService.findReportContainerById(bugId);
            if (reportContainer == null) return ErrorResponse.BUG_NOT_FOUND;
            //Get user role in workspace
            WorkspaceRole workspaceRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            //Ensure user role is high enough to assign any members at all.
            if (!reportContainerService.canRoleEditBugAssignees(workspaceRole)) return ErrorResponse.REQUEST_FORBIDDEN;

            //Return details response.
            ReportContainerAssigneeDetailsResponse reportContainerAssigneeDetailsResponse = reportContainerService.getReportContainerAssigneeDetailsResponseAsDTO(reportContainer, workspaceRole);
            return new ResponseEntity<>(reportContainerAssigneeDetailsResponse, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/bugs/{bug_id}/assign for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to update bug assignees by responding to a list of changes (users to assign and unassign).
     * The method ensures only allowed and possible changes can be made, by skipping over those which are not permitted for a better UX.
     * It skips over non-permitted to demonstrate some idempotence.
     * @param id The workspace ID
     * @param bugId The report container ID
     * @param requestBody The UpdateAssigneesRequest request body.
     * @see UpdateAssigneesRequest
     * @param authentication The user authentication object.
     * @return A list of updated assignees.
     */
    @PatchMapping("/workspaces/{id}/bugs/{bug_id}/assignees")
    public ResponseEntity<?> editAssignees(
            @PathVariable("id") Long id,
            @PathVariable("bug_id") Long bugId,
            @RequestBody UpdateAssigneesRequest requestBody,
            Authentication authentication
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Get user workspace membership object, and ensure is member.
            WorkspaceMember workspaceMember = workspaceService.findWorkspaceMemberInWorkspace(workspace, currentUser);
            if (workspaceMember == null) return ErrorResponse.REQUEST_FORBIDDEN;
            //Ensure bug exists.
            ReportContainer reportContainer = reportContainerService.findReportContainerById(bugId);
            if (reportContainer == null) return ErrorResponse.BUG_NOT_FOUND;
            //Ensure user role is high enough to assign any members at all.
            WorkspaceRole workspaceRole = workspaceMember.getRole();
            if (!reportContainerService.canRoleEditBugAssignees(workspaceRole)) return ErrorResponse.REQUEST_FORBIDDEN;
            //Get roles which user can edit assignments for.
            List<WorkspaceRole> permittedRoles = workspacePermissionsService.findRolesUserCanEditAssignmentsFor(workspaceRole);

            //Validate request body, which provides 2 lists of user IDs.
            List<Long> toAssign = requestBody.usersToAssign();
            List<Long> toUnassign = requestBody.usersToUnassign();
            log.debug("Request provided is: toAssign= {}, toUnassign= {}", toAssign, toUnassign);

            WorkspaceMember member;
            ReportContainerAssignee assignment;
            if (!( (toAssign == null || toAssign.isEmpty()) && (toUnassign == null || toUnassign.isEmpty()) )) {

                //Handle un-assignments first.
                List<WorkspaceMember> membersToUnassign = new ArrayList<>();
                List<User> unassignedUsersToNotify = new ArrayList<>();
                if (toUnassign != null) for (Long userId : toUnassign) {
                    //Find workspace member object.
                    member = workspaceService.findWorkspaceMemberInWorkspaceByUserId(workspace, userId);
                    //If not member, invalid, therefore skip.
                    if (member == null) continue;
                    //Check if user is allowed to edit their role, otherwise skip.
                    if (!permittedRoles.contains(member.getRole())) continue;
                    //Mark member to be unassigned, (idempotent) therefore don't have to fetch and ensure assignment exists first.
                    membersToUnassign.add(member);
                    //Mark user to be notified that they were unassigned from bug.
                    if (member.getUser().getId() != currentUser.getId()) unassignedUsersToNotify.add(member.getUser());
                }
                //Unassign users that need to be unassigned.
                if (!membersToUnassign.isEmpty()) reportContainerService.unassignWorkspaceMembersFromBug(
                    reportContainer.getId(),
                    membersToUnassign.stream().map(WorkspaceMember::getId).toList()
                );
                //Notify newly unassigned users.
                if (!unassignedUsersToNotify.isEmpty()) notificationService.sendMassUnassignedFromBugNotification(unassignedUsersToNotify, reportContainer.getId(), workspace);

                //Handle new assignments.
                List<WorkspaceMember> membersToAssign = new ArrayList<>();
                List<User> assignedUsersToNotify = new ArrayList<>();
                if (toAssign != null) for (Long userId : toAssign) {
                    log.trace("Handling assignment of User ID:{}", userId);
                    member = workspaceService.findWorkspaceMemberInWorkspaceByUserId(workspace, userId);
                    if (member == null) continue;
                    log.trace("Membership found of User ID:{}", userId);
                    if (!permittedRoles.contains(member.getRole())) continue;
                    log.trace("Assignment edit permitted of User ID:{}", userId);
                    //Check user is not already assigned, otherwise skip.
                    assignment = reportContainerService.findReportContainerAssignee(reportContainer.getId(), member.getId());
                    if (assignment != null) {
                        log.warn("Assignment already exists for User ID:{} to Bug ID:{}", userId, bugId);
                        continue;
                    }
                    //Mark member to be assigned
                    membersToAssign.add(member);
                    log.info("Assigned User ID:{} to Report Container:{}", userId, bugId);
                    //Mark user to be notified that they were assigned to bug.
                    if (member.getUser().getId() != currentUser.getId()) assignedUsersToNotify.add(member.getUser());
                }
                //Assign the users that need to be assigned.
                if (!membersToAssign.isEmpty()) reportContainerService.assignWorkspaceMembersToBug(reportContainer, membersToAssign);
                //Notify newly assigned users.
                if (!assignedUsersToNotify.isEmpty()) notificationService.sendMassAssignedToBugNotification(assignedUsersToNotify, reportContainer.getId(), workspace);

                //Add to audit log
                reportContainerActivityService.logReportContainerAssignmentChanges(
                    false,
                    reportContainer.getId(),
                    currentUser,
                    workspaceRole
                );
            }

            //Return updated assignees.
            List<AssigneeDetailsOverviewDTO> updatedAssignees = reportContainerService.findAssigneeDetailsAsDTO(reportContainer);
            return new ResponseEntity<>(updatedAssignees, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed in PATCH /workspaces/{id}/bugs/{bug_id}/assign for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Endpoint to quickly assign user themselves too, only if they are developer or above.
     */
    @PostMapping("/workspaces/{id}/bugs/{bug_id}/assignees/join")
    public ResponseEntity<?> addSelfToAssignees(
            @PathVariable("id") Long id,
            @PathVariable("bug_id") Long bugId,
            Authentication authentication
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Get user workspace membership object, and ensure is member.
            WorkspaceMember workspaceMember = workspaceService.findWorkspaceMemberInWorkspace(workspace, currentUser);
            if (workspaceMember == null) return ErrorResponse.REQUEST_FORBIDDEN;
            //Ensure bug exists.
            ReportContainer reportContainer = reportContainerService.findReportContainerById(bugId);
            if (reportContainer == null) return ErrorResponse.BUG_NOT_FOUND;
            //Ensure user role is high enough to edit assignments at all.
            WorkspaceRole workspaceRole = workspaceMember.getRole();
            if (!reportContainerService.canRoleEditBugAssignees(workspaceRole)) return ErrorResponse.REQUEST_FORBIDDEN;

            //Check not already assignee, otherwise return conflict error.
            if (reportContainerService.isUserBugAssignee(reportContainer, workspaceMember)) {
                return new ResponseEntity<>(
                    new ErrorResponse("Already Assignee", "You have already been assigned to this bug.", Instant.now(), false),
                    HttpStatus.CONFLICT
                );
            }

            //Add self as assignee to report container/bug
            reportContainerService.assignWorkspaceMemberToBug(reportContainer, workspaceMember);
            //Add to audit log
            reportContainerActivityService.logReportContainerAssignmentChanges(
                true,
                reportContainer.getId(),
                currentUser,
                workspaceRole
            );

            //Return updated assignees.
            List<AssigneeDetailsOverviewDTO> updatedAssignees = reportContainerService.findAssigneeDetailsAsDTO(reportContainer);
            return new ResponseEntity<>(updatedAssignees, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed in POST /workspaces/{id}/bugs/{bug_id}/assign for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

}
