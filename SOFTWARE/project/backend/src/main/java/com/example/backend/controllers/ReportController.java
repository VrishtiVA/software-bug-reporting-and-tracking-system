package com.example.backend.controllers;

import com.example.backend.dto.requests.reports.CreateBugReportRequest;
import com.example.backend.dto.responses.ErrorResponse;
import com.example.backend.dto.responses.PaginatedResponse;
import com.example.backend.dto.responses.reports.CreateReportFormDTO;
import com.example.backend.dto.responses.reports.TemporaryReportAttachments;
import com.example.backend.dto.responses.reports.UserReportOverviewDTO;
import com.example.backend.dto.responses.workspaces.WorkspaceCustomLabelDTO;
import com.example.backend.exceptions.BadRequestException;
import com.example.backend.model.reports.Report;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.services.reports.ReportAttachmentService;
import com.example.backend.services.bugs.ReportContainerAsyncService;
import com.example.backend.services.reports.ReportService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspacePermissionsService;
import com.example.backend.services.workspaces.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api")
public class ReportController {

    @Autowired
    private UserService userService;
    @Autowired
    private WorkspacePermissionsService workspacePermissionsService;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private ReportAttachmentService reportAttachmentService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ReportContainerAsyncService reportContainerAsyncService;

    private static final Logger log = LoggerFactory.getLogger(ReportController.class);

    /**
     * Request method to gain access to report a bug in a specific workspace, and pass the user role.
     * @param id The workspace ID
     * @param authentication The user authentication object
     * @return The user role.
     * <ul>
     *     <li> {@code (200) OK} if allowed to create a report and successfully found details for report. </li>
     *     <li> {@code (403) FORBIDDEN} if user not allowed to make a bug report in this workspace. </li>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @GetMapping("/workspaces/{id}/bugs/report")
    public ResponseEntity<?> accessReportBug(@PathVariable long id, Authentication authentication) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Fetch user role in workspace, otherwise null.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            //Check if user is allowed to make report in this workspace
            if (!workspacePermissionsService.canUserMakeReportInWorkspace(workspace, userRole)) return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;

            //Return successful response, with user roles, and custom workspace labels if user is a member.
            List<WorkspaceCustomLabelDTO> customLabelsDTO = userRole == null ? new ArrayList<>() : workspaceService.findWorkspaceCustomLabelsAsDTO(workspace);
            CreateReportFormDTO createReportFormDTO = CreateReportFormDTO.fromData(userRole, customLabelsDTO, workspace);
            return new ResponseEntity<>(createReportFormDTO, HttpStatus.OK);

        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/bugs/report for workspace ID:{}", id, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to handle submitting a new bug report in a workspace.
     * @param id The workspace ID
     * @param authentication The user authentication object
     * @param attachments List of attachments
     * @param requestBody Note: ratings should be provided in zero-index.
     * @see CreateBugReportRequest
     * @return
     * <ul>
     *     <li> {@code (201) CREATED} if successfully created bug report. </li>
     *     <li> {@code (400) BAD REQUEST} if some values were missing or not provided. </li>
     *     <li> {@code (403) FORBIDDEN} if user not allowed to make a bug report in this workspace. </li>
     *     <li> {@code (404) NOT FOUND} if workspace not found. </li>
     *     <li> {@code (500) INTERNAL SERVER ERROR} if something else goes wrong. </li>
     * </ul>
     */
    @PostMapping(value = "/workspaces/{id}/bugs/report", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createReportBug(
        @PathVariable long id,
        @RequestPart("reportData") CreateBugReportRequest requestBody,
        @RequestPart(value = "attachments", required = false) List<MultipartFile> attachments,
        Authentication authentication
    ) {
        TemporaryReportAttachments temporaryReportAttachments = null;
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(id).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;
            //Fetch user role in workspace, otherwise null.
            WorkspaceRole userRole = workspaceService.findUserRoleInWorkspace(workspace, currentUser);
            //Check if user can make a report in this workspace
            if (!workspacePermissionsService.canUserMakeReportInWorkspace(workspace, userRole))
                return ErrorResponse.WORKSPACE_FORBIDDEN_ACCESS;

            //Sanitise and Validate
            String summary = requestBody.summary();
            String steps = requestBody.steps();
            String environment = requestBody.environment();
            String expected = requestBody.expected();
            String actual = requestBody.actual();
            Integer userImpact = requestBody.userImpact();
            Integer severity = requestBody.severity();

            String additionalComments = requestBody.additionalComments();
            List<String> captions = requestBody.captions();

            //Fields only for developers, admins and owners
            String fixSuggestions;
            List<String> labels;
            if (userRole != null && userRole != WorkspaceRole.REPORTER) {
                fixSuggestions = requestBody.fixSuggestions();
                labels = requestBody.labels();
            } else {
                fixSuggestions = null;
                labels = new ArrayList<>();
            }
            log.info("Create report request received");

            //Presence check on required inputs
            if (
                summary == null || summary.isBlank() ||
                steps == null || steps.isBlank() ||
                environment == null || environment.isBlank() ||
                expected == null || expected.isBlank() ||
                actual == null || actual.isBlank() ||
                userImpact == null ||
                severity == null
            ) {
                log.info("Presence check validation discovered missing inputs.");
                throw new BadRequestException("Some required inputs were missing");
            }

            //Sanitise
            summary = summary.trim().replaceAll("\\s+", " ");
            steps = steps.trim().replaceAll("\\s+", " ");
            environment = environment.trim().replaceAll("\\s+", " ");
            expected = expected.trim().replaceAll("\\s+", " ");
            actual = actual.trim().replaceAll("\\s+", " ");

            fixSuggestions = fixSuggestions == null ? null : fixSuggestions.trim().replaceAll("\\s+", " ");
            additionalComments = additionalComments == null ? null : additionalComments.trim().replaceAll("\\s+", " ");

            //Validate
            boolean valid = true;
            validation: {
                //Validate under maximum attachments
                if (attachments != null && attachments.size() > (userRole == null ? 1 : 3)) {
                    log.info("Validation discovered too many attachments");
                    valid = false;
                    break validation;
                }
                //Validate summary
                if (!reportService.isReportSummaryValid(summary)) {
                    log.info("Validation discovered invalid report summary");
                    valid = false;
                    break validation;
                }
                //Validate required long input fields
                for (String text : List.of(steps, environment, expected, actual)) {
                    if (!reportService.isLongInputTextFieldValid(text, false)) {
                        log.info("Validation discovered invalid required report long text.");
                        valid = false;
                        break validation;
                    }
                }
                //Validate optional long input fields
                for (String text : Arrays.asList(fixSuggestions, additionalComments)) {
                    if (!reportService.isLongInputTextFieldValid(text, true)) {
                        log.info("Validation discovered invalid optional report long text.");
                        valid = false;
                        break validation;
                    }
                }
                //Validate rating inputs
                for (int rating : List.of(userImpact, severity)) {
                    if (!reportService.isRatingInputValid(rating, true)) {
                        log.info("Validation discovered an invalid rating value.");
                        valid = false;
                        break validation;
                    }
                }
            }
            if (!valid) {
                throw new BadRequestException("Invalid inputs were provided");
            }

            //Validate labels and convert into a set of labels.
            Set<WorkspaceCustomLabel> labelsCollection = workspaceService.getValidWorkspaceCustomLabelsSet(workspace, labels);

            //Get attachment objects and store temporarily
            if (attachments != null) {
                temporaryReportAttachments = reportAttachmentService.validateAttachmentsAndCaptionsIntoReportAttachments(attachments, captions, userRole, workspace);
                if (temporaryReportAttachments == null) {
                    log.info("Report attachments or captions were found invalid.");
                    throw new BadRequestException("Invalid inputs were provided");
                }
            }

            //Create new report
            Report report = reportService.createReport(
                workspace,
                currentUser,
                userRole,
                summary,
                steps,
                environment,
                expected,
                actual,
                (userImpact + 1),
                (severity + 1),
                fixSuggestions,
                additionalComments,
                labelsCollection,
                (attachments == null ? new HashSet<>() : temporaryReportAttachments.reportAttachments())
            );

            //Async move into main folder.
            if (attachments != null) {
                reportAttachmentService.moveAttachmentsFromTemporaryToPermanent(temporaryReportAttachments.temporaryLocation(), workspace);
            }

            //Send async to handle duplicate detection and bug allocation in background.
            reportContainerAsyncService.handleNewReport(report, workspace, currentUser);

            //Return successful response.
            return new ResponseEntity<>(HttpStatus.CREATED);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Create New Report Failed", e.getMessage(), Instant.now(), false);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in POST /workspaces/{id}/bugs/report for workspace ID:{}", id, e);
            //Clean up if failed.
            if (temporaryReportAttachments != null) reportAttachmentService.cleanTemporaryLocation(temporaryReportAttachments.temporaryLocation());
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method to fetch an attachment.
     * @return The report attachments.
     * Parts of this endpoint implementation were influenced by
     * <ul>
     *      <li><a href="https://howtodoinjava.com/spring-boot/spring-boot-file-upload-with-react/">Spring Boot Multiple File Uploads with React</a></li>
     *      <li>https://mime-type.com/application/octet-stream/</li>
     * </ul>
     * These links are provided as references.
     */
    @GetMapping("/workspaces/{id}/bugs/reports/attachments/{a_id}")
    public ResponseEntity<?> fetchReportAttachmentsForReport(
        @PathVariable("id") Long workspaceId,
        @PathVariable("a_id") Long attachmentId,
        @RequestParam(value="for_masonry", defaultValue="false") boolean forMasonry
    ) {
        try {
            //Ensure workspace exists, otherwise return not found.
            Workspace workspace = workspaceService.findWorkspaceById(workspaceId).orElse(null);
            if (workspace == null) return ErrorResponse.WORKSPACE_NOT_FOUND;

            //Fetch attachment resource if can
            if (attachmentId == null) throw new BadRequestException("An attachment ID is required.");
            Resource attachmentResource = (forMasonry ?
                reportAttachmentService.fetchAttachmentThumbnailAsResource(attachmentId, workspaceId)
                : reportAttachmentService.fetchAttachmentAsResource(attachmentId, workspaceId)
            );
            if (attachmentResource == null) return ErrorResponse.RESOURCE_NOT_FOUND;

            //Find mime-type or fallback to octet stream - a default binary data type for when filetype is unknown.
            String contentType = Files.probeContentType(attachmentResource.getFile().toPath());
            if (contentType == null) contentType = "application/octet-stream";

            //Return attachment
            return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, (forMasonry ? "inline" : "attachment") + "; filename=\"" + attachmentResource.getFilename() + "\"")
                .body(attachmentResource);

        } catch (BadRequestException e) {
            ErrorResponse errorResponse = new ErrorResponse("Fetch Attachments Failed", e.getMessage(), Instant.now(), false);
            return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
        } catch (Exception e) {
            log.error("Failed in GET /workspaces/{id}/bugs/reports/attachments/{a_id} for workspace ID:{}", workspaceId, e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    @GetMapping("/reports")
    public ResponseEntity<?> getPaginatedReportsByUser(
        Authentication authentication,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "15") int size,
        @RequestParam(value = "creation_timestamp_descending", defaultValue = "true") Boolean creationTimestampDescending
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Find and return paginated reports by user.
            String sortBy = "creationTimestamp";
            Sort sortConfig = creationTimestampDescending ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
            Pageable pageable = PageRequest.of(page, size, sortConfig);
            Page<UserReportOverviewDTO> userReportsPage = reportService.findPaginatedReportsByUser(currentUser, pageable);
            return new ResponseEntity<>(PaginatedResponse.fromPage(userReportsPage), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed in GET /reports", e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

}
