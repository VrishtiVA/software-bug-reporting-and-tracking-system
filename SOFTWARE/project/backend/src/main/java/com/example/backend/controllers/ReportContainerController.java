package com.example.backend.controllers;

import com.example.backend.dto.requests.bugs.FiltersBugSearchRequest;
import com.example.backend.dto.requests.bugs.UpdateBugWorkspaceCustomLabelsRequest;
import com.example.backend.dto.responses.ErrorResponse;
import com.example.backend.dto.responses.PaginatedResponse;
import com.example.backend.dto.responses.bugs.*;
import com.example.backend.dto.responses.reports.*;
import com.example.backend.dto.responses.workspaces.WorkspaceCustomLabelDTO;
import com.example.backend.exceptions.BadRequestException;
import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.bugs.ReportContainerStatus;
import com.example.backend.model.reports.FileType;
import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import com.example.backend.model.workspaces.WorkspaceMember;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.services.bugs.ReportContainerActivityService;
import com.example.backend.services.reports.ReportAttachmentService;
import com.example.backend.services.bugs.ReportContainerService;
import com.example.backend.services.reports.ReportService;
import com.example.backend.services.users.NotificationService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspacePermissionsService;
import com.example.backend.services.workspaces.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("api")
public class ReportContainerController {

    @Autowired
    private UserService userService;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private WorkspacePermissionsService workspacePermissionsService;
    @Autowired
    private ReportContainerService reportContainerService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ReportAttachmentService reportAttachmentService;
    @Autowired
    private ReportContainerActivityService reportContainerActivityService;
    @Autowired
    private NotificationService notificationService;

    private static final Logger log = LoggerFactory.getLogger(ReportContainerController.class);

    /**
     * Request method to search for and receive bugs in a specific workspace,
     * This is a Post Mapping to support multiple different filters through request body without having to define too many parameters here.
     * This is a more maintainable approach, the filters can be controlled through the FiltersBugSearchRequest DTO.
     * @see FiltersBugSearchRequest
     * @param authentication The user authentication object.
     * @param id The workspace ID
     * @param page The number of pages to return (optional)
     * @param size The size of the pages to return (optional)
     * @param sort The attribute to sort by (optional)
     * @param descending The sorting direction (optional)
     * @param query The search query (optional)
     * @param filters The filters to apply (optional)
     * @return The bug search results for the workspace.
     */
    @PostMapping("/workspaces/{id}/bugs")
    public ResponseEntity<?> searchBugs(
            Authentication authentication,
            @PathVariable("id") Long id,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size,
            @RequestParam(value = "sort", required = false) String sort,
            @RequestParam(value = "descending", defaultValue = "true") Boolean descending,
            @RequestParam(value = "query", required = false) String query,
            @RequestBody(required = false) FiltersBugSearchRequest filters
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Check if user is allowed to view workspace
            if (!workspacePermissionsService.canUserViewWorkspace(currentUser, workspace)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;
            log.info("User is allowed to view bugs in workspace {}", workspace.getId());

            //Sanitise query.
            query = (query == null || query.isBlank()) ? null : query.trim().replaceAll("\\s+", " ");

            //Validate sort, otherwise default.
            if (sort == null) sort = "priority";
            else switch (sort) {
                case "firstReportedTimestamp": break;
                case "priority": break;
                case "hotness": break;
                case "relevance":
                    //Relevance depends on a query
                    //If there is no query, the relevance should be based on priority, rather than just being unsorted.
                    sort = (query == null) ? "priority" : null;
                    break;
                case "frequency": sort = "frequency"; break;
                case "suggestions": sort = "incomingDuplicateSuggestionsCount"; break;
                default: sort = "priority"; break;
            }

            //Take filters
            List<ReportContainerProgress> includeProgress;
            List<ReportContainerStatus> includeStatus;
            List<String> includeLabels;
            if (filters == null) {
                includeProgress = null;
                includeStatus = null;
                includeLabels = null;
            } else {
                includeProgress = filters.progress() != null && !filters.progress().isEmpty() ? filters.progress() : null;
                includeStatus = filters.status() != null && !filters.status().isEmpty() ? filters.status() : null;
                includeLabels = filters.labels() != null && !filters.labels().isEmpty() ? filters.labels() : null;
            }

            //Set up pagination configuration.
            Pageable pageable;
            if (sort == null) {
                pageable = PageRequest.of(page, size);
            } else {
                Sort sortConfig = descending ? Sort.by(sort).descending() : Sort.by(sort).ascending();
                pageable = PageRequest.of(page, size, sortConfig);
            }

            //Return stable container details if found.
            Page<ReportContainerOverviewDTO> reportContainerOverviewDTOList = reportContainerService.findPaginatedReportContainerSearchResultsByWorkspaceAsDTO(
                query,
                workspace,
                pageable,
                includeProgress, includeStatus, includeLabels
            );

            //Return with paginated metadata.
            return new ResponseEntity<>(PaginatedBugOverviewResponse.fromPage(reportContainerOverviewDTOList), HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/bugs for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /* ---------- View Bug Endpoints ---------- */

    /**
     * Request method to fetch report container details,
     * only if stable since preliminary containers should not be displayed.
     * @param id The workspace ID
     * @param bugId The report container ID
     * @param authentication The user authentication object
     * @return report container details in ReportContainerDetailsDTO format.
     * @see ReportContainerDetailsDTO
     */
    @GetMapping("/workspaces/{id}/bugs/{bug_id}")
    public ResponseEntity<?> getStableReportContainerDetails(@PathVariable("id") Long id, @PathVariable("bug_id") Long bugId, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Check if user is allowed to view workspace
            if (!workspacePermissionsService.canUserViewWorkspace(currentUser, workspace)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;
            //Check container exists.
            ReportContainer reportContainer = reportContainerService.findReportContainerById(bugId);
            if (reportContainer == null) return ErrorResponse.BUG_NOT_FOUND;
            //Check if user can view container. e.g. if it is stable.
            if (!workspacePermissionsService.canUserViewReportContainer(reportContainer)) return ErrorResponse.BUG_FORBIDDEN_ACCESS;
            //Return container details
            WorkspaceRole workspaceRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            ReportContainerDetailsDTO reportContainerDetailsDTO = reportContainerService.findReportContainerDetailsAsDTO(reportContainer, workspaceRole);
            return new ResponseEntity<>(reportContainerDetailsDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/bugs/{bug_id} for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to find paginated reports for a bug.
     * @param authentication The user authentication object.
     * @param id The workspace ID
     * @param bugId The report container ID
     * @param page The page number
     * @param size The page size
     * @return A page of reports in PaginatedResponse format containing ReportDTO formatted content.
     * @see PaginatedResponse
     * @see ReportDTO
     */
    @GetMapping("/workspaces/{id}/bugs/{bug_id}/reports")
    public ResponseEntity<?> getReportContainerPaginatedReports(
            Authentication authentication,
            @PathVariable("id") Long id,
            @PathVariable("bug_id") Long bugId,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Check if user is allowed to view workspace
            if (!workspacePermissionsService.canUserViewWorkspace(currentUser, workspace)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;
            //Check container exists.
            ReportContainer reportContainer = reportContainerService.findReportContainerById(bugId);
            if (reportContainer == null) return ErrorResponse.BUG_NOT_FOUND;
            //Check if user can view container. e.g. if it is stable.
            if (!workspacePermissionsService.canUserViewReportContainer(reportContainer)) return ErrorResponse.BUG_FORBIDDEN_ACCESS;

            //Find page of reports
            Pageable pageable = PageRequest.of(page, size, Sort.by("creationTimestamp").ascending());
            Page<ReportDTO> reportPage = reportService.findPaginatedReportsInReportContainer(reportContainer, pageable);
            //Return bug reports in paginated reports response.
            return new ResponseEntity<>(PaginatedResponse.fromPage(reportPage), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/bugs/{bug_id}/reports for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /** Request method to fetch paginated attachment metadata. */
    @GetMapping("/workspaces/{id}/bugs/{bug_id}/attachments/{type}")
    public ResponseEntity<?> getReportContainerPaginatedReportAttachmentMetadata(
        Authentication authentication,
        @PathVariable("id") Long id,
        @PathVariable("bug_id") Long bugId,
        @PathVariable("type") String fileType,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Check if user is allowed to view workspace
            if (!workspacePermissionsService.canUserViewWorkspace(currentUser, workspace)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;
            //Check container exists.
            ReportContainer reportContainer = reportContainerService.findReportContainerById(bugId);
            if (reportContainer == null) return ErrorResponse.BUG_NOT_FOUND;
            //Check if user can view container. e.g. if it is stable.
            if (!workspacePermissionsService.canUserViewReportContainer(reportContainer)) return ErrorResponse.BUG_FORBIDDEN_ACCESS;

            //Find report attachment metadata
            Pageable pageable = PageRequest.of(page, size, Sort.by("reference").ascending());
            Page<ReportAttachmentDTO> reportAttachmentsPage = reportAttachmentService.findPaginatedReportAttachmentsMetadata(
                reportContainer,
                (fileType.equals("videos") ? FileType.VIDEO : FileType.IMAGE),
                pageable
            );
            //Return bug reports in paginated reports response.
            return new ResponseEntity<>(PaginatedResponse.fromPage(reportAttachmentsPage), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/bugs/{bug_id}/reports for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /** Request method to fetch paginated report field data. */
    @GetMapping("/workspaces/{id}/bugs/{bug_id}/fields/{field}")
    public ResponseEntity<?> getReportContainerPaginatedReportFieldData(
        Authentication authentication,
        @PathVariable("id") Long id,
        @PathVariable("bug_id") Long bugId,
        @PathVariable("field") String fieldName,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Check if user is allowed to view workspace
            if (!workspacePermissionsService.canUserViewWorkspace(currentUser, workspace)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;
            //Check container exists.
            ReportContainer reportContainer = reportContainerService.findReportContainerById(bugId);
            if (reportContainer == null) return ErrorResponse.BUG_NOT_FOUND;
            //Check if user can view container. e.g. if it is stable.
            if (!workspacePermissionsService.canUserViewReportContainer(reportContainer)) return ErrorResponse.BUG_FORBIDDEN_ACCESS;

            //Validate field option
            if (fieldName == null) throw new BadRequestException("Invalid field provided");
            fieldName = fieldName.toLowerCase();
            if (!(
                fieldName.equals("steps") ||
                fieldName.equals("environment") ||
                fieldName.equals("expected") ||
                fieldName.equals("actual") ||
                fieldName.equals("fix-suggestions") ||
                fieldName.equals("additional-comments")
            )) throw new BadRequestException("Invalid field provided");

            //Find report field data
            Pageable pageable = PageRequest.of(page, size, Sort.by("creationTimestamp").ascending());
            Page<ReportFieldDTO> reportsFieldDataPage = reportService.findPaginatedReportFieldMetadata(
                reportContainer,
                fieldName,
                pageable
            );

            //Return bug field data in paginated reports response.
            return new ResponseEntity<>(PaginatedResponse.fromPage(reportsFieldDataPage), HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Fetch Field Data Failed", e.getMessage(), Instant.now(), true);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/bugs/{bug_id}/fields/{field} for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /* ---------- Update Bug Tracking Fields Related Endpoints ---------- */

    /** Request method to update bug summary. */
    @PatchMapping("/workspaces/{id}/bugs/{bug_id}/summary")
    public ResponseEntity<?> updateBugSummary(
            @PathVariable("id") Long id,
            @PathVariable("bug_id") Long bugId,
            @RequestParam("summary") String summary,
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
            //Ensure user role is high enough to update bug tracking. Developer +
            if (!workspacePermissionsService.canRoleUpdateBugTrackingDetails(workspaceRole)) return ErrorResponse.REQUEST_FORBIDDEN;

            //Clean and validate new summary for bug
            if (summary == null || summary.isBlank()) throw new BadRequestException("Some required inputs were missing.");
            summary = summary.trim().replaceAll("\\s+", " ");

            String oldSummary = reportContainer.getSummary();
            if (!oldSummary.equals(summary)) {
                if (!reportService.isReportSummaryValid(summary)) throw new BadRequestException("An invalid input was provided.");
                //Update summary
                reportContainer = reportContainerService.updateReportContainerSummary(reportContainer.getId(), summary);
                //Add to audit log
                reportContainerActivityService.logReportContainerModification(
                    "summary",
                    reportContainer.getId(),
                    currentUser,
                    workspaceRole,
                    oldSummary,
                    summary
                );
                //Send notification to assigned users.
                List<User> assigneesExcludingSelf = reportContainerService.findAssignedUsersExcludingSelf(reportContainer.getId(), currentUser.getId());
                if (!assigneesExcludingSelf.isEmpty()) notificationService.sendMassBugSummaryUpdateNotification(
                    assigneesExcludingSelf,
                    reportContainer.getId(),
                    workspace,
                    oldSummary,
                    summary
                );
            }

            //Return updated summary.
            return new ResponseEntity<>(reportContainer.getSummary(), HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Failed To Update Bug Summary", e.getMessage(), Instant.now(), false);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in PATCH /workspaces/{id}/bugs/{bug_id}/summary for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /** Request method to update bug status. */
    @PatchMapping("/workspaces/{id}/bugs/{bug_id}/status")
    public ResponseEntity<?> updateBugStatus(
        @PathVariable("id") Long id,
        @PathVariable("bug_id") Long bugId,
        @RequestParam("status") String status,
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
            //Ensure user role is high enough to update bug tracking. Developer +
            if (!workspacePermissionsService.canRoleUpdateBugTrackingDetails(workspaceRole)) return ErrorResponse.REQUEST_FORBIDDEN;

            //Clean and validate status for bug
            if (status == null || status.isBlank()) throw new BadRequestException("Some required inputs were missing.");
            status = status.trim().replaceAll("\\s+", " ");
            ReportContainerStatus newStatus = reportContainerService.validReportContainerStatus(status);
            if (newStatus == null) throw new BadRequestException("An invalid input was provided.");
            ReportContainerStatus oldStatus = reportContainer.getStatus();

            if (oldStatus != newStatus) {
                //Update status
                reportContainer = reportContainerService.updateReportContainerStatus(reportContainer.getId(), newStatus);
                //Add to audit log
                reportContainerActivityService.logReportContainerProgression(
                    "status",
                    reportContainer.getId(),
                    currentUser,
                    workspaceRole,
                    oldStatus.toString(),
                    newStatus.toString()
                );
                //Send notification to assigned users.
                List<User> assigneesExcludingSelf = reportContainerService.findAssignedUsersExcludingSelf(reportContainer.getId(), currentUser.getId());
                if (!assigneesExcludingSelf.isEmpty()) notificationService.sendMassBugProgressionUpdateNotification(
                    assigneesExcludingSelf,
                    reportContainer.getId(),
                    workspace,
                    "status",
                    oldStatus.toString(),
                    newStatus.toString()
                );
            }
            //Return updated status.
            return new ResponseEntity<>(reportContainer.getStatus(), HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Failed To Update Bug Status", e.getMessage(), Instant.now(), false);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in PATCH /workspaces/{id}/bugs/{bug_id}/status for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /** Request method to update bug progress. */
    @PatchMapping("/workspaces/{id}/bugs/{bug_id}/progress")
    public ResponseEntity<?> updateBugProgress(
        @PathVariable("id") Long id,
        @PathVariable("bug_id") Long bugId,
        @RequestParam("progress") String progress,
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
            //Ensure user role is high enough to update bug tracking. Developer +
            if (!workspacePermissionsService.canRoleUpdateBugTrackingDetails(workspaceRole)) return ErrorResponse.REQUEST_FORBIDDEN;

            //Clean and validate progress for bug
            if (progress == null || progress.isBlank()) throw new BadRequestException("Some required inputs were missing.");
            progress = progress.trim().replaceAll("\\s+", " ");
            ReportContainerProgress newProgress = reportContainerService.validReportContainerProgress(progress);
            if (newProgress == null) throw new BadRequestException("An invalid input was provided.");
            ReportContainerProgress oldProgress = reportContainer.getProgress();

            if (oldProgress != newProgress) {
                //Update progress
                reportContainer = reportContainerService.updateReportContainerProgress(reportContainer.getId(), newProgress);
                //Update assignment count for assignees if necessary
                if (oldProgress == ReportContainerProgress.CLOSED || newProgress == ReportContainerProgress.CLOSED) {
                    List<WorkspaceMember> assignees = reportContainerService.findAssignedMembers(reportContainer);
                    reportContainerService.updateAssignmentCountForWorkspaceMembersById(assignees.stream().map(WorkspaceMember::getId).toList());
                }
                //Add to audit log
                reportContainerActivityService.logReportContainerProgression(
                    "progress",
                    reportContainer.getId(),
                    currentUser,
                    workspaceRole,
                    oldProgress.toString(),
                    newProgress.toString()
                );
                //Send notification to assigned users.
                List<User> assigneesExcludingSelf = reportContainerService.findAssignedUsersExcludingSelf(reportContainer.getId(), currentUser.getId());
                if (!assigneesExcludingSelf.isEmpty()) notificationService.sendMassBugProgressionUpdateNotification(
                    assigneesExcludingSelf,
                    reportContainer.getId(),
                    workspace,
                    "progress",
                    oldProgress.toString().toLowerCase(),
                    newProgress.toString().toLowerCase()
                );
            }

            //Return updated progress.
            return new ResponseEntity<>(reportContainer.getProgress(), HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Failed To Update Bug Progress", e.getMessage(), Instant.now(), false);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in PATCH /workspaces/{id}/bugs/{bug_id}/progress for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /** Request method to update bug labels. */
    @PatchMapping("/workspaces/{id}/bugs/{bug_id}/labels")
    public ResponseEntity<?> updateBugLabels(
            @PathVariable("id") Long id,
            @PathVariable("bug_id") Long bugId,
            @RequestBody UpdateBugWorkspaceCustomLabelsRequest requestBody,
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
            //Ensure user role is high enough to update bug tracking. Developer +
            if (!workspacePermissionsService.canRoleUpdateBugTrackingDetails(workspaceRole)) return ErrorResponse.REQUEST_FORBIDDEN;

            //Validate and find label objects.
            List<String> addLabels = requestBody.add();
            List<String> removeLabels = requestBody.remove();
            if (addLabels == null || removeLabels == null) throw new BadRequestException("Some required inputs were missing.");
            Set<WorkspaceCustomLabel> addWorkspaceCustomLabelSet = workspaceService.getValidWorkspaceCustomLabelsSet(workspace, addLabels);
            Set<WorkspaceCustomLabel> removeWorkspaceCustomLabelSet = workspaceService.getValidWorkspaceCustomLabelsSet(workspace, removeLabels);

            //Update report container custom label set.
            reportContainer = reportContainerService.updateWorkspaceCustomLabels(reportContainer.getId(), addWorkspaceCustomLabelSet, removeWorkspaceCustomLabelSet);

            //Add to audit log
            reportContainerActivityService.logReportContainerModification(
                "labels",
                reportContainer.getId(),
                currentUser,
                workspaceRole,
                null,
                null
            );

            //Return updated labels.
            List<WorkspaceCustomLabelDTO> workspaceCustomLabelDTOList = reportContainer.getWorkspaceCustomLabels().stream().map(label -> WorkspaceCustomLabelDTO.fromEntity(label.getWorkspaceCustomLabel())).toList();
            return new ResponseEntity<>(workspaceCustomLabelDTOList, HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Failed To Update Bug Labels", e.getMessage(), Instant.now(), false);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in PATCH /workspaces/{id}/bugs/{bug_id}/labels for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /* --------------- Bug Audit Log Related Endpoints --------------- */

    /** Request method for getting paginated audit logs for a report container/bug. */
    @GetMapping("/workspaces/{id}/bugs/{bug_id}/audit-log")
    public ResponseEntity<?> getPaginatedBugAuditLogs(
        Authentication authentication,
        @PathVariable("id") Long id,
        @PathVariable("bug_id") Long bugId,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Check if user is allowed to view workspace
            if (!workspacePermissionsService.canUserViewWorkspace(currentUser, workspace)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;
            //Check container exists.
            ReportContainer reportContainer = reportContainerService.findReportContainerById(bugId);
            if (reportContainer == null) return ErrorResponse.BUG_NOT_FOUND;
            //Check if user can view container. e.g. if it is stable.
            if (!workspacePermissionsService.canUserViewReportContainer(reportContainer)) return ErrorResponse.BUG_FORBIDDEN_ACCESS;

            //Find audit log notes page.
            Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
            Page<BugAuditLogDTO> auditLogsDataPage = reportContainerActivityService.findPaginatedAuditLogsData(
                reportContainer,
                pageable
            );
            //Return audit log notes page.
            return new ResponseEntity<>(PaginatedResponse.fromPage(auditLogsDataPage), HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/bugs/{bug_id}/audit-log for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

}
