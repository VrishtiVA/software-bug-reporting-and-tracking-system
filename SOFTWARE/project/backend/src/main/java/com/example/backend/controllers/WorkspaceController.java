package com.example.backend.controllers;

import com.example.backend.dto.requests.workspaces.UpdateWorkspaceDetailsRequest;
import com.example.backend.dto.responses.workspaces.metrics.PriorityFrequencyBugCountBucketsDTO;
import com.example.backend.dto.responses.workspaces.metrics.WorkspaceCustomLabelUsageDTO;
import com.example.backend.dto.responses.users.UserOverviewDTO;
import com.example.backend.dto.responses.workspaces.*;
import com.example.backend.dto.responses.workspaces.metrics.WorkspaceMetricsResponse;
import com.example.backend.exceptions.BadRequestException;
import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import com.example.backend.model.workspaces.WorkspaceMember;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.dto.requests.workspaces.CreateWorkspaceRequest;
import com.example.backend.dto.requests.workspaces.CustomLabelDTO;
import com.example.backend.dto.requests.workspaces.MemberRoleDTO;
import com.example.backend.dto.responses.ErrorResponse;
import com.example.backend.services.bugs.ReportContainerPriorityService;
import com.example.backend.services.bugs.ReportContainerService;
import com.example.backend.services.reports.ReportService;
import com.example.backend.services.users.NotificationService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspacePermissionsService;
import com.example.backend.services.workspaces.WorkspaceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api")
public class WorkspaceController {

    @Autowired
    private UserService userService;
    @Autowired
    private WorkspacePermissionsService workspacePermissionsService;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ReportContainerPriorityService reportContainerPriorityService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ReportContainerService reportContainerService;

    private static final Logger log = LoggerFactory.getLogger(WorkspaceController.class);

    /**
     * Request method to get all workspaces that the user owns summaries
     * @param authentication The user authentication object
     * @return
     * <ul>
     *     <li> {@code (200) OK} if successfully found owned workspaces </li>
     *     <li> {@code (204) NO CONTENT} if successfully found no owned workspaces. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces/owned")
    public ResponseEntity<?> getOwnedWorkspaces(Authentication authentication) {

        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Fetch workspaces owned by user.
            List<OwnedWorkspaceOverviewDTO> ownedWorkspaceSummaries = workspaceService.findWorkspaceSummariesByOwnerAsDTO(currentUser);

            if (ownedWorkspaceSummaries.isEmpty()) {
                //If no workspaces found, return 204 no content success response.
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                //Otherwise return workspaces found.
                return new ResponseEntity<>(ownedWorkspaceSummaries, HttpStatus.OK);
            }

        } catch (Exception e) {
            log.error("Failed in GET /workspaces/owned", e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to get all workspaces where the user is a member summaries
     * @param authentication The user authentication object
     * @return
     * <ul>
     *     <li> {@code (200) OK} if successfully found member workspaces </li>
     *     <li> {@code (204) NO CONTENT} if successfully found no member workspaces. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces/member")
    public ResponseEntity<?> getMemberWorkspaces(Authentication authentication) {

        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Fetch workspaces the user is a member in.
            List<MemberWorkspaceOverviewDTO> memberWorkspaceSummaries = workspaceService.findWorkspaceSummariesByMemberAsDTO(currentUser);

            if (memberWorkspaceSummaries.isEmpty()) {
                //If no workspaces found, return 204 no content success response.
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                //Otherwise return workspaces found.
                return new ResponseEntity<>(memberWorkspaceSummaries, HttpStatus.OK);
            }

        } catch (Exception e) {
            log.error("Failed in GET /workspaces/member", e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to get all workspaces summaries that the user is a member or owner of.
     * @param authentication The user authentication object
     * @return
     * <ul>
     *     <li> {@code (200) OK} if successfully found workspaces </li>
     *     <li> {@code (204) NO CONTENT} if successfully found no workspaces. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces")
    public ResponseEntity<?> getWorkspaces(Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Fetch workspaces the user is a part of.
            List<ShortWorkspaceOverviewDTO> userWorkspaceSummaries = workspaceService.findShortWorkspaceOverviewByUserAsDTO(currentUser);
            if (userWorkspaceSummaries.isEmpty()) {
                //If no workspaces found, return 204 no content success response.
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                //Otherwise return workspaces found.
                return new ResponseEntity<>(
                    ShortWorkspaceOverviewForUserResponse.fromData(userWorkspaceSummaries, currentUser.getId()),
                    HttpStatus.OK
                );
            }
        } catch (Exception e) {
            log.error("Failed in GET /workspaces", e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to get information for a specific workspace
     * @see WorkspaceDTO
     * @param id The id of a specific workspace
     * @param authentication The user authentication object
     * @return the workspace details in workspace dto format.
     * <ul>
     *     <li> {@code (200) OK} if successfully found workspace details. </li>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to view the workspace.
     *          e.g. if the workspace is private and the user is not a member.
     *     </li>
     *     <li> {@code (404) NOT FOUND} if workspace doesn't exist. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces/{id}")
    public ResponseEntity<?> getWorkspace(@PathVariable("id") long id, Authentication authentication) {

        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceByIdWithCustomLabels(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Ensure user can access the workspace, otherwise forbid access.
            boolean canView = workspacePermissionsService.canUserViewWorkspace(currentUser, workspace);
            if (!canView) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;

            //Fetch user role in workspace, otherwise null.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);

            //Fetch and return workspace details response
            WorkspaceDTO workspaceResponse = WorkspaceDTO.fromEntity(workspace, userRole);
            return new ResponseEntity<>(workspaceResponse, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id} for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to get workspace members collection for a specific workspace.
     * @see WorkspaceMemberDTO
     * @param id The workspace id
     * @param authentication The user authentication object
     * @return The workspace member details in WorkspaceMemberDTO format
     * <ul>
     *     <li> {@code (200) OK} if successfully found workspace member details. </li>
     *     <li> {@code (204) NO CONTENT} if successfully found no workspace member details. </li>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to view the workspace.
     *          e.g. if the workspace is private and the user is not a member.
     *     </li>
     *     <li> {@code (404) NOT FOUND} if workspace doesn't exist. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces/{id}/members")
    public ResponseEntity<?> getWorkspaceMembers(@PathVariable("id") long id, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Ensure user can access the workspace, otherwise forbid access.
            boolean canView = workspacePermissionsService.canUserViewWorkspace(currentUser, workspace);
            if (!canView) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;

            //Fetch and return workspace member details response
            List<WorkspaceMemberDTO> workspaceMembersResponse = workspaceService.findWorkspaceMembersAsDTO(workspace);
            if (workspaceMembersResponse.isEmpty()) {
                //This is unlikely to occur, but could be a possibility in the far future.
                return new ResponseEntity<>(HttpStatus.NO_CONTENT);
            } else {
                //Successfully return workspace member details response.
                return new ResponseEntity<>(workspaceMembersResponse, HttpStatus.OK);
            }

        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/members for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to check if user is allowed to create a new workspace.
     * This is usually if they haven't reached the maximum number of workspaces yet.
     * @param authentication The user authentication object
     * @return true or false depending on if the user can create a new workspace.
     * <ul>
     *     <li> {@code (200) OK} if successfully decided if they can create a new workspace.
     *          This may be true or false.
     *     </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces/create/access")
    public ResponseEntity<?> accessCreateWorkspace(Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Check if user is below maximum created workspaces,
            //and return access granted if they are.
            if (workspacePermissionsService.hasUserReachedMaxOwnedWorkspaces(currentUser)) {
                return new ResponseEntity<>(false, HttpStatus.OK);
            } else {
                return new ResponseEntity<>(true, HttpStatus.OK);
            }

        } catch (Exception e) {
            log.error("Failed in GET /workspaces/create/access", e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to check for the existence of a user by their username, and
     * if they can be added to the workspace.
     * A user can only be added to the workspace if they are not the owner.
     * Admins can only add users with developer and reporter roles anyway.
     * Error responses here don't redirect, as they will be handled in the form.
     * @see UserOverviewDTO
     * @param username The username the find the user by.
     * @return A summary DTO object for the user if success.
     * <ul>
     *     <li> {@code (200) OK} with user summary DTO if successfully found user. </li>
     *     <li> {@code (400) BAD REQUEST} if user is trying to add themselves to the workspace. </li>
     *     <li> {@code (404) NOT FOUND} if couldn't find user. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces/create/validate/member/{username}")
    public ResponseEntity<?> canUserBecomeWorkspaceMember(@PathVariable String username, Authentication authentication) {

        try {
            //Ensure current user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Try to fetch the user with the username.
            User user = userService.findUserByUsername(username);
            //If user doesn't exist, return 404.
            if (user == null) {
                return new ResponseEntity<>(
                    new ErrorResponse("User not found", "This user doesn't exist", Instant.now()),
                    HttpStatus.NOT_FOUND
                );
            } else if (user == currentUser) {
                //Ensure user is not trying to add themselves.
                return new ResponseEntity<>(
                    new ErrorResponse("Bad request", "You cannot add yourself here", Instant.now()),
                    HttpStatus.BAD_REQUEST
                );
            } else {
                return new ResponseEntity<>(UserOverviewDTO.fromEntity(user), HttpStatus.OK);
            }
        } catch (Exception e) {
            log.error("Failed in GET /workspaces/create/validate/member/{username}", e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to create a new
     * @see CreateWorkspaceRequest
     * @param authentication The user authentication object
     * @return A location header with the uri to access the newly created workspace.
     * <ul>
     *     <li> {@code (201) CREATED} if successfully created workspace. </li>
     *     <li> {@code (400) BAD REQUEST} if no/invalid inputs provided,
     *          if member provided too many labels or members,
     *          or if user already has maximum number of owned workspaces.
     *     </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @PostMapping("/workspaces")
    public ResponseEntity<?> createWorkspace(@RequestBody CreateWorkspaceRequest requestBody, Authentication authentication) {

        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Check if user is allowed to create workspace: below maximum created workspaces,
            if (workspacePermissionsService.hasUserReachedMaxOwnedWorkspaces(currentUser)) {
                ErrorResponse errorResponse = new ErrorResponse(
                    "Bad request",
                    "You have already reached the maximum number of owned workspaces.",
                    Instant.now()
                );
                return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
            }

            //Extract inputs
            String name = requestBody.name();
            Boolean visibility = requestBody.visibility();
            String description = requestBody.description();
            String colour = requestBody.colour();
            List<CustomLabelDTO> labelsDTO = requestBody.labels();
            List<MemberRoleDTO> membersDTO = requestBody.members();

            // ----- Validate inputs ----- //
            String invalidMessage = "Invalid inputs were provided.";
            //Presence check on required inputs
            if (
                name == null || colour == null || visibility == null ||
                name.isBlank() || colour.isBlank()
            ) {
                throw new BadRequestException("Some required inputs were missing.");
            }

            //Sanitise inputs, removing spaces and ensuring any whitespace is just 1 space
            name = name.trim().replaceAll("\\s+", " ");
            description = description == null || description.isBlank() ? null : description.trim().replaceAll("\\s+", " ");
            colour = colour.trim();

            if (!workspaceService.isWorkspaceNameValid(name)) throw new BadRequestException(invalidMessage);
            else if (!workspaceService.isWorkspaceDescriptionValid(description)) throw new BadRequestException(invalidMessage);
            else if (!workspaceService.isWorkspaceColourValid(colour)) throw new BadRequestException(invalidMessage);

            //Collect and validate members
            Map<User, WorkspaceRole> members = null;
            if (!membersDTO.isEmpty() && !(membersDTO.size() > WorkspacePermissionsService.WORKSPACE_MEMBERS_LIMIT)) {
                try { members = workspaceService.validateMembersDtoToMap(membersDTO, currentUser); }
                catch (Exception e) { throw new BadRequestException("Invalid inputs were provided."); }
            }

            //Collect and validate custom labels
            Map<String, String> customLabels = null;
            if (!labelsDTO.isEmpty() && !(labelsDTO.size() > WorkspacePermissionsService.WORKSPACE_CUSTOM_LABEL_LIMIT)) {
                try { customLabels = workspaceService.validateCustomLabelsDtoToMap(labelsDTO);}
                catch (Exception e) { throw new BadRequestException("Invalid inputs were provided."); }
            }

            // ----- Create workspace ----- //
            Workspace workspace = workspaceService.createWorkspace(
                currentUser,
                name,
                description,
                visibility,
                colour,
                members,
                customLabels
            );

            //Notify members that they were added to this new workspace.
            if (members != null && !members.isEmpty()) {
                notificationService.sendMassAddedToWorkspaceNotification(
                    members.keySet().stream().toList(),
                    workspace
                );
            }

            //Return created response with location header.
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.add("Location", "/workspaces/" + workspace.getId());
            return new ResponseEntity<>(responseHeaders, HttpStatus.CREATED);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Workspace creation failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in POST /workspaces", e);
            ErrorResponse errorResponse = new ErrorResponse("Workspace creation failed", "Sorry, something went wrong.", Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Request method to check if user is allowed to configure a workspace.
     * Only the workspace owner or admin would be allowed to edit a workspace.
     * @param authentication The user authentication object
     * @param id The workspace ID.
     * @return The configurable workspace details in WorkspaceWithMembersAndCustomLabelsDTO format.
     * <ul>
     *     <li> {@code (200) OK} if user can access configure workspace, with the workspace details. </li>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to edit workspace. </>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces/{id}/configure/access")
    public ResponseEntity<?> accessConfigureWorkspace(@PathVariable("id") long id, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Fetch user role in workspace, otherwise null.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            //Check if user can edit workspace
            if (!workspacePermissionsService.canUserConfigureWorkspace(userRole)) return ErrorResponse.WORKSPACE_FORBIDDEN_CONFIGURE;

            //Fetch and return workspace details response
            List<WorkspaceMemberDTO> workspaceMembersResponse = workspaceService.findWorkspaceMembersAsDTO(workspace);
            List<WorkspaceCustomLabelDTO> workspaceCustomLabelsResponse = workspaceService.findWorkspaceCustomLabelsAsDTO(workspace);
            WorkspaceWithMembersAndCustomLabelsDTO workspaceResponse = WorkspaceWithMembersAndCustomLabelsDTO.fromEntity(workspace, userRole, workspaceMembersResponse, workspaceCustomLabelsResponse);
            return new ResponseEntity<>(workspaceResponse, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/configure/access for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to update workspace details.
     * Only the workspace owner or admin can perform this action, and
     * admins can only perform this partially as enforced.
     * @see UpdateWorkspaceDetailsRequest
     * @param id The workspace ID.
     * @param requestBody The UpdateWorkspaceDetailsRequest request body.
     * @param authentication The user authentication object.
     * @return
     * <ul>
     *     <li> {@code (200) OK} if successfully updated workspace details. </li>
     *     <li> {@code (400) BAD REQUEST} if invalid inputs were provided where needed. </>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to edit workspace. </>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @PatchMapping("/workspaces/{id}/configure/details")
    public ResponseEntity<?> editWorkspaceDetails(@PathVariable("id") long id, @RequestBody UpdateWorkspaceDetailsRequest requestBody, Authentication authentication) {

        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceByIdWithCustomLabels(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Fetch user role in workspace, otherwise null.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            //Check if user can edit workspace
            if (!workspacePermissionsService.canUserConfigureWorkspace(userRole)) return ErrorResponse.WORKSPACE_FORBIDDEN_CONFIGURE;

            //Extract inputs
            String name = requestBody.name();
            Boolean visibility = requestBody.visibility();
            String description = requestBody.description();
            String colour = requestBody.colour();

            //Sanitise inputs, removing spaces and ensuring any whitespace is just 1 space
            if (name != null) name = name.trim().replaceAll("\\s+", " ");
            description = description == null || description.isBlank() ? null : description.trim().replaceAll("\\s+", " ");
            if (colour != null) colour = colour.trim();

            //Check and update according to role, only consider name and visibility update if owner.
            String invalidMessage = "Invalid inputs were provided.";
            if (userRole == WorkspaceRole.OWNER) {
                //Check if name present and update required
                String workspaceName = workspace.getName();
                if (name != null && !name.isBlank() && !name.equals(workspaceName)) {
                    //Validate
                    if (!workspaceService.isWorkspaceNameValid(name)) throw new BadRequestException(invalidMessage);
                    //Update workspace name.
                    workspace = workspaceService.updateWorkspaceName(workspace, name);
                    //Notify name update
                    List<User> members = workspaceService.findWorkspaceMemberUsersByWorkspaceAndNotCurrentUser(workspace, currentUser);
                    notificationService.sendMassWorkspaceRenamedNotification(members, workspaceName, workspace);
                }
                //Check if visibility present and update required.
                if (visibility != null && visibility != workspace.isPublic()) {
                    //Update workspace visibility.
                    workspace = workspaceService.updateWorkspaceVisibility(workspace, visibility);
                }
            }
            //Check if colour present and update required.
            if (colour != null && !colour.isBlank() && !colour.equals(workspace.getColour())) {
                //Validate
                if (!workspaceService.isWorkspaceColourValid(colour)) throw new BadRequestException(invalidMessage);
                //Update workspace colour.
                workspace = workspaceService.updateWorkspaceColour(workspace, colour);
            }
            //Check if description update required (null safe check)
            if (!Objects.equals(description, workspace.getDescription())) {
                //Validate
                if (!workspaceService.isWorkspaceDescriptionValid(description))
                    throw new BadRequestException(invalidMessage);
                //Update workspace description.
                workspace = workspaceService.updateWorkspaceDescription(workspace, description);
            }

            //Fetch and return workspace details response
            WorkspaceDTO workspaceResponse = WorkspaceDTO.fromEntity(workspace, userRole);
            return new ResponseEntity<>(workspaceResponse, HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Workspace Update Failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in PATCH /workspaces/{id}/configure/details for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to delete a workspace.
     * Only the workspace owner should be able to delete a workspace.
     * @param id The id of the workspace to delete.
     * @param authentication The user authentication object.
     * @return
     * <ul>
     *     <li> {@code (200) OK} if successfully deleted workspace. </li>
     *     <li> {@code (403) FORBIDDEN} if not allowed to delete this workspace. </li>
     *     <li> {@code (404) NOT FOUND} if workspace doesn't exist. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @DeleteMapping("/workspaces/{id}")
    public ResponseEntity<?> deleteWorkspace(@PathVariable("id") long id, Authentication authentication) {

        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Check if user is allowed to delete workspace that they are owner.
            if (workspacePermissionsService.canUserDeleteWorkspace(currentUser, workspace)) {

                //Fetch workspace members that need to be notified
                List<User> formerMembers = workspaceService.findWorkspaceMemberUsersByWorkspaceAndNotCurrentUser(workspace, currentUser);
                String workspaceName = workspace.getName();

                //Delete the workspace
                workspaceService.deleteWorkspace(workspace);
                //Notify former members that workspace was deleted.
                notificationService.sendMassWorkspaceDeletedNotification(formerMembers, workspaceName);

                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                //Otherwise if they are not allowed to, return error.
                return new ResponseEntity<>(
                    new ErrorResponse(
                        "Forbidden Request",
                        "Sorry, you aren't allowed to delete this workspace.",
                        Instant.now(),
                        true
                    ),
                    HttpStatus.FORBIDDEN
                );
            }
        } catch (Exception e) {
            log.error("Failed in DELETE /workspaces/{id} for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to add a new workspace custom label to a workspace.
     * @param id The id of the workspace
     * @param authentication The user authentication object
     * @param requestBody The custom label response body
     * @see CustomLabelDTO
     * @return collection of all workspace custom labels in workspace if successful, otherwise error response.
     * <ul>
     *     <li> {@code (201) CREATED} if successfully added workspace custom label. </li>
     *     <li> {@code (400) BAD REQUEST} if invalid inputs were provided where needed. </>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to configure this workspace. </>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @PostMapping("/workspaces/{id}/configure/labels")
    public ResponseEntity<?> addWorkspaceCustomLabel(@PathVariable("id") long id, @RequestBody CustomLabelDTO requestBody, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Fetch role and ensure user is allowed to configure the workspace.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            if (!workspacePermissionsService.canUserConfigureWorkspace(userRole)) return ErrorResponse.WORKSPACE_FORBIDDEN_CONFIGURE;

            //Ensure not reached maximum custom labels already
            if (workspace.getWorkspaceCustomLabels().size() >= WorkspacePermissionsService.WORKSPACE_CUSTOM_LABEL_LIMIT) {
                throw new BadRequestException("Workspace has already reached maximum custom labels.");
            }

            //Fetch inputs
            String labelName = requestBody.name();
            String labelColour = requestBody.colour();

            //Validate presence
            if (labelName == null || labelColour == null || labelName.isBlank() || labelColour.isBlank()) {
                throw new BadRequestException("Some inputs were missing.");
            }
            //Sanitise
            labelName = labelName.trim().replaceAll("\\s+", " ");
            labelColour = labelColour.trim();
            //Validate format
            boolean valid = workspaceService.isWorkspaceCustomLabelNameValid(labelName);
            valid = valid && workspaceService.isWorkspaceCustomLabelColourValid(labelColour);
            if (!valid) throw new BadRequestException("An invalid workspace label was provided.");
            //Check unique
            if (workspaceService.doesWorkspaceCustomLabelExist(labelName, workspace)) {
                throw new BadRequestException("The workspace label already exists.");
            }

            //Add new custom workspace label
            workspaceService.addCustomWorkspaceLabelToWorkspace(workspace, labelName, labelColour);

            //Return response with all workspace custom labels.
            List<WorkspaceCustomLabelDTO> customLabelsResponse = workspaceService.findWorkspaceCustomLabelsAsDTO(workspace);
            return new ResponseEntity<>(customLabelsResponse, HttpStatus.CREATED);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Add Label Failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in POST /workspaces/{id}/configure/labels for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to remove a workspace custom label from a workspace.
     * Note: Trying to delete a label here that doesn't exist won't throw an error - idempotency
     * @param id The ID of the workspace
     * @param authentication The user authentication object
     * @param labelName The custom label name to know which to remove. Workspace custom labels are unique.
     * @return collection of all workspace custom labels in workspace if successful, otherwise error response.
     * <ul>
     *     <li> {@code (200) OK} if successfully deleted workspace custom label. </li>
     *     <li> {@code (400) BAD REQUEST} if invalid inputs were provided where needed. </>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to edit workspace. </>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @DeleteMapping("/workspaces/{id}/configure/labels/{labelName}")
    public ResponseEntity<?> deleteWorkspaceCustomLabel(@PathVariable("id") long id, @PathVariable String labelName, Authentication authentication) {
        try {
            System.out.println("Entered delete request method");

            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Fetch user role in workspace, and ensure they are allowed to configure the workspace.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            if (!workspacePermissionsService.canUserConfigureWorkspace(userRole)) return ErrorResponse.WORKSPACE_FORBIDDEN_CONFIGURE;

            System.out.println("Found label input: " + labelName);

            //Validate presence
            if (labelName == null || labelName.isBlank()) {
                throw new BadRequestException("No workspace label was provided.");
            }
            //Sanitise
            labelName = labelName.trim().replaceAll("\\s+", " ");
            //Validate format
            if (!workspaceService.isWorkspaceCustomLabelNameValid(labelName)) throw new BadRequestException("An invalid custom workspace label was provided.");

            //Remove workspace label
            workspaceService.removeCustomWorkspaceLabelFromWorkspace(workspace, labelName);

            //Return response with all workspace custom labels.
            List<WorkspaceCustomLabelDTO> customLabelsResponse = workspaceService.findWorkspaceCustomLabelsAsDTO(workspace);
            return new ResponseEntity<>(customLabelsResponse, HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Delete Label Failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in DELETE /workspaces/{id}/configure/labels/{labelName} for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to update the colour of a workspace label in a workspace.
     * @param id The ID of the workspace
     * @param requestBody The custom workspace label input
     * @see CustomLabelDTO
     * @param authentication The user authentication object
     * @return collection of all workspace custom labels in workspace if successful, otherwise error response.
     * <ul>
     *     <li> {@code (200) OK} if successfully updated workspace custom label colour. </li>
     *     <li> {@code (400) BAD REQUEST} if invalid inputs were provided where needed. </>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to edit workspace. </>
     *     <li> {@code (404) NOT FOUND} if workspace not found, or custom label not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @PatchMapping("/workspaces/{id}/configure/labels")
    public ResponseEntity<?> updateWorkspaceCustomLabelColour(@PathVariable("id") long id, @RequestBody CustomLabelDTO requestBody, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Fetch user role in workspace, and ensure that they are allowed to configure the workspace.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            if (!workspacePermissionsService.canUserConfigureWorkspace(userRole)) return ErrorResponse.WORKSPACE_FORBIDDEN_CONFIGURE;

            //Fetch input
            String labelName = requestBody.name();
            String labelColour = requestBody.colour();
            //Validate presence
            if (labelName == null || labelColour == null || labelName.isBlank() || labelColour.isBlank()) {
                throw new BadRequestException("Invalid inputs were provided.");
            }
            //Sanitise
            labelName = labelName.trim().replaceAll("\\s+", " ");
            labelColour = labelColour.trim();
            //Validate format
            boolean valid = workspaceService.isWorkspaceCustomLabelNameValid(labelName);
            valid = valid && workspaceService.isWorkspaceCustomLabelColourValid(labelColour);
            if (!valid) throw new BadRequestException("An invalid workspace label was provided.");

            //Check label exists.
            WorkspaceCustomLabel wcl = workspaceService.findCustomWorkspaceLabelInWorkspace(workspace, labelName);
            if (wcl == null) {
                return new ResponseEntity<>(
                    new ErrorResponse(
                        "Workspace Update Failed",
                        "The custom workspace label could not found.",
                        Instant.now(),
                        false
                    ), HttpStatus.NOT_FOUND
                );
            } else {
                //Update workspace label.
                workspaceService.updateCustomWorkspaceLabelColourInWorkspace(workspace, wcl, labelColour);
            }

            //Return response with all workspace custom labels.
            List<WorkspaceCustomLabelDTO> customLabelsResponse = workspaceService.findWorkspaceCustomLabelsAsDTO(workspace);
            return new ResponseEntity<>(customLabelsResponse, HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Update Label Failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in PATCH /workspaces/{id}/configure/labels for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to add a new workspace member to a workspace.
     * @param id The workspace ID
     * @param requestBody The workspace member request body
     * @see MemberRoleDTO
     * @param authentication The user authentication object.
     * @return collection of all workspace members in workspace if successful, otherwise error response.
     * <ul>
     *     <li> {@code (200) OK} if successfully added workspace member. </li>
     *     <li> {@code (400) BAD REQUEST} if invalid inputs were provided where needed, or if the user doesn't exist, or is already a member. </>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to edit workspace, or if their role prevents them from adding a member with a certain role. </>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @PostMapping("/workspaces/{id}/configure/members")
    public ResponseEntity<?> addWorkspaceMember(@PathVariable("id") long id, @RequestBody MemberRoleDTO requestBody, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Fetch user role in workspace, and ensure that they are allowed to configure the workspace.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            if (!workspacePermissionsService.canUserConfigureWorkspace(userRole)) return ErrorResponse.WORKSPACE_FORBIDDEN_CONFIGURE;

            //Ensure not reached maximum members already
            if (workspace.getWorkspaceMembers().size() >= WorkspacePermissionsService.WORKSPACE_MEMBERS_LIMIT) {
                throw new BadRequestException("Workspace has already reached maximum members.");
            }

            //Fetch inputs
            String usernameInput = requestBody.username();
            String roleInput = requestBody.role();

            //Presence check validation
            if (usernameInput == null || usernameInput.isBlank() || roleInput == null || roleInput.isBlank()) {
                throw new BadRequestException("Some inputs were missing.");
            }

            //Fetch inputs
            usernameInput = usernameInput.trim();
            roleInput = roleInput.trim();

            String invalidMessage = "Invalid inputs were provided";

            //Validate member
            //Validate format
            if (!workspaceService.isWorkspaceMemberNameFormatValid(usernameInput)) throw new BadRequestException(invalidMessage);
            //Check if user exists
            User memberUser = userService.findUserByUsername(usernameInput);
            if (memberUser == null) throw new BadRequestException("This user does not exist.");
            //Ensure user is not already a member
            if (workspaceService.isUserWorkspaceMember(memberUser, workspace)) {
                throw new BadRequestException("User is already a workspace member.");
            }

            //Validate role
            WorkspaceRole memberRole = workspaceService.getValidWorkspaceMemberRole(roleInput);
            if (memberRole == null) throw new BadRequestException(invalidMessage);
            //Ensure user allowed to add with role
            if ((memberRole == WorkspaceRole.ADMIN || memberRole == WorkspaceRole.OWNER) && userRole != WorkspaceRole.OWNER) {
                return new ResponseEntity<>(
                    new ErrorResponse(
                        "Forbidden Request",
                        "Sorry, you aren't allowed to add admins to this workspace.",
                        Instant.now(),
                        true
                    ),
                    HttpStatus.FORBIDDEN
                );
            }

            //Add Workspace Member
            workspaceService.addWorkspaceMemberToWorkspace(workspace, memberUser, memberRole);
            //Notify member that they were added to workspace
            notificationService.sendAddedToWorkspaceNotification(memberUser, memberRole, workspace);
            //Return all workspace members.
            List<WorkspaceMemberDTO> workspaceMembersResponse = workspaceService.findWorkspaceMembersAsDTO(workspace);
            return new ResponseEntity<>(workspaceMembersResponse, HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Add Member Failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in POST /workspaces/{id}/configure/members for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to remove a workspace member from a workspace.
     * @param id The workspace ID
     * @param memberUsername The workspace member username to indicate the member to be removed.
     * @param authentication The user authentication object
     * @return collection of all workspace members in workspace if successful, otherwise error response.
     * <ul>
     *     <li> {@code (200) OK} if successfully removed workspace member. </li>
     *     <li> {@code (400) BAD REQUEST} if invalid inputs were provided where needed, user doesn't exist, or user wasn't a member. </>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to edit workspace, or if their role prevents them from removing a user with a certain role. </>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @DeleteMapping("/workspaces/{id}/configure/members/{memberUsername}")
    public ResponseEntity<?> deleteWorkspaceMember(@PathVariable("id") long id, @PathVariable String memberUsername, Authentication authentication) {

        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Fetch user role in workspace, and ensure that they are allowed to configure the workspace.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            if (!workspacePermissionsService.canUserConfigureWorkspace(userRole)) return ErrorResponse.WORKSPACE_FORBIDDEN_CONFIGURE;

            //Presence check validation
            if (memberUsername == null || memberUsername.isBlank()) throw new BadRequestException("Some inputs were missing.");
            memberUsername = memberUsername.trim();

            String invalidMessage = "Invalid inputs were provided";
            //Validate member
            if (!workspaceService.isWorkspaceMemberNameFormatValid(memberUsername)) throw new BadRequestException(invalidMessage);
            //Check if user exists
            User memberUser = userService.findUserByUsername(memberUsername);
            if (memberUser == null) throw new BadRequestException("This user does not exist.");

            //Find member role in workspace.
            WorkspaceRole memberRole = workspaceService.findUserRoleInWorkspace(workspace, memberUser);
            if (memberRole == null) throw new BadRequestException("This user was not a member.");
            //Ensure user allowed to remove member with this role.
            if ((memberRole == WorkspaceRole.ADMIN || memberRole == WorkspaceRole.OWNER) && userRole != WorkspaceRole.OWNER) {
                return new ResponseEntity<>(
                    new ErrorResponse(
                        "Forbidden Request",
                        "Sorry, you aren't allowed to remove admins from this workspace.",
                        Instant.now(),
                        true
                    ),
                    HttpStatus.FORBIDDEN
                );
            }

            //Remove workspace member
            workspaceService.removeWorkspaceMemberFromWorkspace(workspace, memberUser);
            //Notify member that they were removed from workspace member
            notificationService.sendRemovedFromWorkspaceNotification(memberUser, workspace);

            //Return all workspace members.
            List<WorkspaceMemberDTO> workspaceMembersResponse = workspaceService.findWorkspaceMembersAsDTO(workspace);
            return new ResponseEntity<>(workspaceMembersResponse, HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Remove Member Failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in DELETE /workspaces/{id}/configure/members/{memberUsername} for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to update workspace member role in a workspace.
     * @param id The workspace ID
     * @param requestBody The member and role request body
     * @see MemberRoleDTO
     * @param authentication The user authentication object
     * @return collection of all workspace members in workspace if successful, otherwise error response.
     * <ul>
     *     <li> {@code (200) OK} if successfully updated workspace member role. </li>
     *     <li> {@code (400) BAD REQUEST} if invalid inputs were provided where needed, user doesn't exist, or user isn't a member. </>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to edit workspace, or their role prevents them from updating the role of a certain user or to a certain role. </>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @PatchMapping("/workspaces/{id}/configure/members")
    public ResponseEntity<?> updateWorkspaceMemberRole(@PathVariable("id") long id, @RequestBody MemberRoleDTO requestBody, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);

            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Fetch user role in workspace, and ensure that they are allowed to configure the workspace.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            if (!workspacePermissionsService.canUserConfigureWorkspace(userRole)) return ErrorResponse.WORKSPACE_FORBIDDEN_CONFIGURE;

            //Fetch inputs
            String usernameInput = requestBody.username();
            String roleInput = requestBody.role();

            //Presence check validation
            if (usernameInput == null || usernameInput.isBlank() || roleInput == null || roleInput.isBlank()) {
                throw new BadRequestException("Some inputs were missing.");
            }

            //Fetch inputs
            usernameInput = usernameInput.trim();
            roleInput = roleInput.trim();

            String invalidMessage = "Invalid inputs were provided";
            // ----- Validate member -----
            //Validate format
            if (!workspaceService.isWorkspaceMemberNameFormatValid(usernameInput)) throw new BadRequestException(invalidMessage);
            //Check if user exists
            User memberUser = userService.findUserByUsername(usernameInput);
            if (memberUser == null) throw new BadRequestException("This user does not exist.");

            //Find member role in workspace, and ensure they are a member.
            WorkspaceRole memberCurrentRole = workspaceService.findUserRoleInWorkspace(workspace, memberUser);
            if (memberCurrentRole == null) throw new BadRequestException("This user was not a member.");

            // ----- Validate role -----
            WorkspaceRole memberRole = workspaceService.getValidWorkspaceMemberRole(roleInput);
            if (memberRole == null) throw new BadRequestException(invalidMessage);

            //Ensure user allowed to add with role, unless role is unchanged.
            if ((memberRole != memberCurrentRole) &&
                    (memberRole == WorkspaceRole.ADMIN || memberCurrentRole == WorkspaceRole.ADMIN ||
                    memberRole == WorkspaceRole.OWNER || memberCurrentRole == WorkspaceRole.OWNER) &&
                        userRole != WorkspaceRole.OWNER) {
                return new ResponseEntity<>(
                    new ErrorResponse(
                        "Forbidden Request",
                        "Sorry, you aren't allowed to configure the roles of admins within this workspace.",
                        Instant.now(),
                        true
                    ),
                    HttpStatus.FORBIDDEN
                );
            }

            //Update Workspace Member
            WorkspaceMember wm = workspaceService.findWorkspaceMemberInWorkspace(workspace, memberUser);
            workspaceService.updateWorkspaceMemberRoleInWorkspace(workspace, wm, memberRole);

            //Return all workspace members.
            List<WorkspaceMemberDTO> workspaceMembersResponse = workspaceService.findWorkspaceMembersAsDTO(workspace);
            return new ResponseEntity<>(workspaceMembersResponse, HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Add Member Failed", e.getMessage(), Instant.now());
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in PATCH /workspaces/{id}/configure/members for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to get workspace custom labels
     * @param id The workspace ID of the workspace to fetch labels for.
     * @param authentication The user authentication
     * @return The workspace custom labels in workspace custom label DTO format.
     * <ul>
     *     <li> {@code (200) OK} if successfully found and returned the workspace custom labels. </li>
     *     <li> {@code (403) FORBIDDEN} if user is not allowed to view the workspace. </>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces/{id}/labels")
    public ResponseEntity<?> getWorkspaceCustomLabels(@PathVariable("id") long id, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Ensure user is allowed to view the workspace.
            if (!workspacePermissionsService.canUserViewWorkspace(currentUser, workspace)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;
            //Return response with all workspace custom labels.
            List<WorkspaceCustomLabelDTO> customLabelsResponse = workspaceService.findWorkspaceCustomLabelsAsDTO(workspace);
            return new ResponseEntity<>(customLabelsResponse, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/labels for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /* ---------- Workspace Metrics Endpoints ---------- */

    /**
     * Request method to get the workspace metrics.
     * This includes: <ul>
     *     <li> Total Stable Bugs </li>
     *     <li> Outstanding Duplicate Suggestions Count - This can be recognised by the number of reports marked as OUTSTANDING in the duplicate detection lifecycle. </li>
     *     <li> Staged Reports Count - This can be recognised by the number of reports marked as STAGED in the duplicate detection lifecycle. </li>
     *     <li> Labels and their Counts </li>
     *     <li> Progress and their Counts </li>
     *     <li> Bugs Resolved Count </li>
     *     <li> Bug Priority Category Distribution </li>
     *     <li> Priority Frequency Bubbles </li>
     * </ul>
     * Some of the data is bucketed, to prevent returning too many results as a whole list of all possible items, saving time while providing human-digestable data.
     * @param id The workspace ID
     * @return The total potential duplicates count.
     * <ul>
     *     <li> {@code (200) OK} if successfully found metrics for workspace. </li>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces/{id}/metrics")
    public ResponseEntity<?> workspaceMetrics(Authentication authentication, @PathVariable long id) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Ensure user is allowed to view the workspace.
            if (!workspacePermissionsService.canUserViewWorkspace(currentUser, workspace)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;

            //Find total bugs in workspace
            Long totalActiveBugs = reportContainerService.countActiveReportContainersByWorkspace(workspace);
            //Find total bugs resolved.
            Long totalBugsResolved = reportContainerService.countByWorkspaceAndReportContainerStatusIsResolved(workspace);
            //Find progress distribution.
            Map<ReportContainerProgress, Integer> progressDistribution = reportContainerService.findProgressDistributionByWorkspace(workspace);
            //Find workspace custom label usage in bugs distribution.
            List<WorkspaceCustomLabelUsageDTO> workspaceCustomLabelUsageDistribution = reportContainerService.countActiveWorkspaceCustomLabelUsageInWorkspace(workspace);
            //Find outstanding duplicate suggestions and staged reports in the workspace.
            int countOutstandingDuplicateSuggestionReports = reportService.countOutstandingDuplicateSuggestedReportsInWorkspace(workspace);
            int countStagedDuplicateReports = reportService.countStagedDuplicateReportsInWorkspace(workspace);
            //Find priority category distribution
            Map<String, Integer> priorityCategoryDistribution = reportContainerPriorityService.findPriorityCategoryDistributionByWorkspace(workspace);
            List<PriorityFrequencyBugCountBucketsDTO> priorityFrequencyBugCountBuckets = reportContainerPriorityService.findPriorityFrequencyBugCountBucketsByWorkspace(workspace);

            return new ResponseEntity<>(
                WorkspaceMetricsResponse.fromData(
                    totalActiveBugs,
                    countOutstandingDuplicateSuggestionReports,
                    countStagedDuplicateReports,
                    workspaceCustomLabelUsageDistribution,
                    progressDistribution,
                    totalBugsResolved,
                    priorityCategoryDistribution,
                    priorityFrequencyBugCountBuckets
                ),
                HttpStatus.OK
            );
        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/metrics for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

}
