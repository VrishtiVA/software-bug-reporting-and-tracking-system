package com.example.backend.controllers;

import com.example.backend.dto.requests.duplicate_detection.ClaimReportToResolveDuplicatesForConditionsRequest;
import com.example.backend.dto.requests.duplicate_detection.ResolveDuplicateSuggestionRequest;
import com.example.backend.dto.responses.ErrorResponse;
import com.example.backend.dto.responses.duplicate_detection.DuplicateSuggestionsToResolveDTO;
import com.example.backend.model.reports.Report;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.services.bugs.ReportContainerAsyncService;
import com.example.backend.services.bugs.ReportContainerService;
import com.example.backend.services.reports.ReportService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspacePermissionsService;
import com.example.backend.services.workspaces.WorkspaceService;
import org.apache.coyote.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;

@RestController
@RequestMapping("/api")
public class ReportDuplicateController {

    private static final Logger log = LoggerFactory.getLogger(ReportDuplicateController.class);

    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private WorkspacePermissionsService workspacePermissionsService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private UserService userService;
    @Autowired
    private ReportContainerService reportContainerService;
    @Autowired
    private ReportContainerAsyncService reportContainerAsyncService;

    /**
     * Request method to claim a report to resolve duplicate suggestions for.
     * @param id The workspace ID
     * @param requestBody The conditions of reports that can be claimed, which are which reports to include and exclude.
     * @param authentication The user authentication object.
     * @return successful response if successfully claimed a report to resolve duplicates for.
     */
    @PostMapping("/workspaces/{id}/duplicates/resolve/claim")
    public ResponseEntity<?> claimReportToResolveDuplicateSuggestionsFor(
        @PathVariable long id,
        @RequestBody ClaimReportToResolveDuplicatesForConditionsRequest requestBody,
        Authentication authentication
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Check if user is allowed to resolve a duplicate in this workspace.
            if (!workspacePermissionsService.canUserResolveDuplicatesInWorkspace(workspace, currentUser))
                return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;

            //Try to claim report to resolve atomically
            log.debug("Received Report Container ID: {}. Exclude Report IDs: {}", requestBody.reportContainerId(), requestBody.excludeReportIds());
            boolean reportClaimed = false;
            try {
                reportClaimed = reportContainerService.claimReport(workspace.getId(), currentUser.getId(), requestBody.excludeReportIds(), requestBody.reportContainerId());
            } catch (DataIntegrityViolationException e) {
                log.warn("Unable to claim report to resolve. Issue: {}", e.getCause().getMessage());
            }
            if (reportClaimed) {
                //If successful, return OK to signify access to resolve duplicates.
                return new ResponseEntity<>(HttpStatus.OK);
            } else {
                //Otherwise, check if there were any suggestions, or return busy.
                int countOutstandingPotentialDuplicates = reportService.countOutstandingDuplicateSuggestedReportsInWorkspace(workspace);
                if (countOutstandingPotentialDuplicates == 0) {
                    //If there weren't even any outstanding suggestions
                    return new ResponseEntity<>(
                            new ErrorResponse(
                                "No Duplicate Suggestions Found",
                                "There are no duplicate suggestions available at the moment.",
                                Instant.now(),
                                false
                            ),
                            HttpStatus.NOT_FOUND
                    );
                } else {
                    //The duplicate suggestions were busy.
                    return new ResponseEntity<>(
                            new ErrorResponse(
                                "Duplicate Suggestions Busy",
                                "Others are resolving duplicate suggestions at the moment.",
                                Instant.now(),
                                false
                            ),
                            HttpStatus.CONFLICT
                    );
                }
            }
        } catch (Exception e) {
            log.error("Failed in POST /workspaces/{id}/duplicates/resolve/claim for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to fetch the duplicate suggestions for the users claim.
     * @param id The workspace ID
     * @param authentication The user authentication object
     * @return The duplicate suggestions to resolve in DuplicateSuggestionsToResolveDTO format.
     */
    @GetMapping("/workspaces/{id}/duplicates/resolve")
    public ResponseEntity<?> getDuplicateSuggestionsForReport(@PathVariable long id, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Check if user is allowed to resolve a duplicate in this workspace.
            if (!workspacePermissionsService.canUserResolveDuplicatesInWorkspace(workspace, currentUser)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;

            //Find the duplicate suggestion that was claimed by the user.
            DuplicateSuggestionsToResolveDTO duplicateSuggestionsToResolveDTO = reportContainerService.constructDuplicateSuggestionsToResolveDTO(workspace, currentUser);
            //If not found, return error.
            if (duplicateSuggestionsToResolveDTO ==  null) return ErrorResponse.RESOURCE_NOT_FOUND;
            //Otherwise successfully return suggestions in format
            return new ResponseEntity<>(duplicateSuggestionsToResolveDTO, HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/duplicates/resolve for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to cancel a claim to resolve.
     * @param id The workspace ID
     * @param reportId The claimed report to unclaim.
     * @param authentication The user authentication object.
     */
    @PostMapping("/workspaces/{id}/duplicates/resolve/{r_id}/cancel")
    public ResponseEntity<?> cancelResolveDuplicatesForReport(@PathVariable long id, @PathVariable("r_id") long reportId, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Check if user is allowed to resolve a duplicate in this workspace.
            if (!workspacePermissionsService.canUserResolveDuplicatesInWorkspace(workspace, currentUser)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;
            //Find report
            Report report = reportContainerService.findReportById(reportId);
            if (report == null) {
                //If report doesn't exist
                return new ResponseEntity<>(
                    new ErrorResponse(
                        "Resource not found",
                        "Sorry, unable to complete action as this resource doesn't exist.",
                        Instant.now(),
                        false
                    ),
                    HttpStatus.NOT_FOUND
                );
            }
            if (reportContainerService.validateFromReportFromWorkspaceAndIsOutstanding(report, workspace)) {
                //If report was an actual valid suggestion, un-claim to cancel, to allow someone else to handle.
                //Only allow un-claim if it is claimed by this user.
                reportContainerService.unclaimReport(reportId, currentUser.getId());
            }
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed in POST /workspaces/{id}/duplicates/resolve/{r_id}/cancel for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to resolve the duplicate suggestion for a report.
     * This could be used to accept a suggestion, reject a suggestion, or reject all suggestions.
     * @param id The workspace ID
     * @param requestBody The ResolveDuplicateSuggestionRequest request body.
     *                    If toReport is null, then all suggestions for fromReport must have been rejected.
     * @see ResolveDuplicateSuggestionRequest
     * @param authentication The user authentication object.
     * @return successful response to indicate acknowledgement, and to let the user know that it will be dealt with.
     * <ul>
     *     <li> {@code (200) OK} if successfully acknowledged duplicate suggestion resolve. </li>
     *     <li> {@code (403) FORBIDDEN} if user not allowed to resolve duplicate suggestions in this workspace. </li>
     *     <li> {@code (404) NOT FOUND} if workspace or relevant reports not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @PostMapping("/workspaces/{id}/duplicates/resolve")
    public ResponseEntity<?> duplicatesResolve(
        Authentication authentication,
        @PathVariable long id,
        @RequestBody ResolveDuplicateSuggestionRequest requestBody
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Get user role in workspace
            WorkspaceRole workspaceRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            //Check if user is allowed to resolve a duplicate in this workspace.
            if (!workspacePermissionsService.canUserResolveDuplicatesInWorkspace(workspaceRole)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;

            //Validate and process
            Long fromReportId = requestBody.fromReportId();
            Long toReportId = requestBody.toReportId();
            Boolean isAccepted = requestBody.isAccepted();
            Report fromReport;
            Report toReport;
            if (fromReportId != null) {
                //Check fromReport exists.
                fromReport = reportContainerService.findReportById(fromReportId);
                if (fromReport == null) {
                    //If fromReport doesn't exist, can't proceed here.
                    log.debug("Report (from side) with ID {} not found.", fromReportId);
                    return ErrorResponse.RESOLVE_DUPLICATES_REPORT_NOT_FOUND;
                } else if (toReportId != null) {
                    //If fromReport exists, check if toReport exists
                    toReport = reportContainerService.findReportById(toReportId);
                    if (toReport == null) {
                        //If toReport with ID not found, shouldn't proceed.
                        log.debug("Report (to side) with ID {} not found.", toReportId);
                        return ErrorResponse.RESOLVE_DUPLICATES_REPORT_NOT_FOUND;
                    } else {
                        //Validate fromReport and toReport, that the suggestion between them exists, and is from this workspace.
                        if (!reportContainerService.validateSuggestionExistsAndFromReportAndToReportFromWorkspaceAndFromReportIsOutstanding(fromReport, toReport, workspace)) {
                            throw new BadRequestException("Invalid reports were provided.");
                        }
                        //Check accept or reject.
                        if (isAccepted == null || isAccepted.equals(Boolean.FALSE)) {
                            //Reject individual duplicate suggestion
                            reportContainerService.rejectIndividualDuplicateSuggestion(fromReportId, toReportId);
                            //Check if no suggestions leftover and need to contain
                            if (!reportContainerService.existsDuplicateSuggestionsFromReport(fromReportId)) {
                                //Set lifecycle status to RESOLVED and send async request to handle where it should go.
                                reportContainerService.updateReportDuplicateDetectionLifecycleStateToResolved(fromReport);
                                //Reset claimed state of report.
                                reportContainerService.unclaimReport(fromReportId, currentUser.getId());
                                reportContainerAsyncService.rejectAllDuplicateSuggestionsForReport(fromReportId);
                            }
                        } else if (isAccepted.equals(Boolean.TRUE)) {
                            //Accept duplicate suggestion from fromReport to toReport
                            //Set lifecycle status to RESOLVED and send async request to handle where it should go.
                            reportContainerService.updateReportDuplicateDetectionLifecycleStateToResolved(fromReport);
                            //Reset claimed state of report.
                            reportContainerService.unclaimReport(fromReportId, currentUser.getId());
                            reportContainerAsyncService.acceptADuplicateSuggestionForReport(fromReportId, toReportId, currentUser, workspaceRole, workspace);
                        } else {
                            log.warn("Reached unexpected position in GET /workspaces/{id}/duplicates/resolve");
                        }
                    }
                } else {
                    //Validate fromReport is OUTSTANDING, and from this workspace.
                    if (!reportContainerService.validateFromReportAndToReportFromWorkspaceAndFromReportIsOutstanding(fromReport, null, workspace)) {
                        throw new BadRequestException("Invalid reports were provided.");
                    }
                    //If fromReport exists but toReport doesn't, then all duplicate suggestions for fromReport must have been rejected.
                    //Set lifecycle status to RESOLVED and send async request to handle where it should go.
                    reportContainerService.updateReportDuplicateDetectionLifecycleStateToResolved(fromReport);
                    //Reset claimed state of report.
                    reportContainerService.unclaimReport(fromReportId, currentUser.getId());
                    reportContainerAsyncService.rejectAllDuplicateSuggestionsForReport(fromReportId);
                }
            } else {
                log.debug("Report ID (from side) was not provided");
                return ErrorResponse.RESOLVE_DUPLICATES_REPORT_NOT_FOUND;
            }
            //Return successful response
            return new ResponseEntity<>(HttpStatus.OK);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Resolve Duplicates Failed", e.getMessage(), Instant.now(), true);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in POST /workspaces/{id}/duplicates/resolve for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

}
