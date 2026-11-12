package com.example.backend.services.reports;

import com.example.backend.dto.responses.reports.ReportDTO;
import com.example.backend.dto.responses.reports.ReportFieldDTO;
import com.example.backend.dto.responses.reports.UserReportOverviewDTO;
import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.reports.*;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.repositories.reports.ReportRepository;
import com.example.backend.services.workspaces.WorkspacePermissionsService;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class ReportService {

    private static final Logger log = LoggerFactory.getLogger(ReportService.class);

    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private WorkspacePermissionsService workspacePermissionsService;

    /* ----- Character Limits ----- */
    public static final int SUMMARY_MIN_LENGTH = 10;
    public static final int SUMMARY_MAX_LENGTH = 50;
    public static final int LONG_TEXT_MIN_LENGTH = 10;
    public static final int LONG_TEXT_MAX_LENGTH = 500;
    public static final int ATTACHMENT_CAPTION_MAX_LENGTH = 100;

    /** Method to create a new report. */
    @Transactional
    public Report createReport(
        Workspace workspace,
        User user,
        WorkspaceRole userRole,
        String summary,
        String steps,
        String environment,
        String expected,
        String actual,
        int userImpact,
        int severity,
        String fixSuggestions,
        String additionalComments,
        Set<WorkspaceCustomLabel> labels,
        Set<ReportAttachment> attachments
    ) {
        Set<ReportWorkspaceCustomLabelMapping> labelMappings = new HashSet<>();

        //Create new report
        Report report = new Report(
            workspace,
            user,
            userRole,
            summary,
            steps,
            environment,
            expected,
            actual,
            userImpact,
            severity,
            fixSuggestions,
            additionalComments,
            labelMappings,
            attachments
        );

        //Make report mapping object for labels and add to report.
        for (WorkspaceCustomLabel label : labels) {
            labelMappings.add(new ReportWorkspaceCustomLabelMapping(
                report, label
            ));
        }
        report.setWorkspaceCustomLabels(labelMappings);

        //Save created report
        report = reportRepository.save(report);
        return report;
    }

    /**
     * Method to check if sample report initialisation is complete.
     * @return true if it is, otherwise false.
     */
    public boolean completedSampleReportsInitialisation() {
        return reportRepository.isSampleReportInitialisationComplete(
            List.of(1L, 2L, 3L, 4L, 5L, 6L),
            List.of(
                DuplicateDetectionLifecycleState.OUTSTANDING,
                DuplicateDetectionLifecycleState.CONTAINED,
                DuplicateDetectionLifecycleState.STAGED,
                DuplicateDetectionLifecycleState.RESOLVED
            )
        );
    }

    /** Method to find report by its ID. */
    public Report findById(Long reportId) {
        return reportRepository.findById(reportId).orElse(null);
    }

    /* ---------- Report Duplicate Related Methods ---------- */

    public int countOutstandingDuplicateSuggestedReportsInWorkspace(Workspace workspace) {
        return reportRepository.countByWorkspaceAndDuplicateDetectionLifecycleState(workspace, DuplicateDetectionLifecycleState.OUTSTANDING);
    }
    public int countStagedDuplicateReportsInWorkspace(Workspace workspace) {
        return reportRepository.countByWorkspaceAndDuplicateDetectionLifecycleState(workspace, DuplicateDetectionLifecycleState.STAGED);
    }

    /* ---------- Report Query Methods ---------- */

    /**
     * Get page of bug reports from a specific report container
     * @param reportContainer The report container to fetch bug reports from.
     * @param pageable The pagination configuration
     * @return The page of bug reports, formatted as a ReportDTO
     * @see ReportDTO
     */
    public Page<ReportDTO> findPaginatedReportsInReportContainer(ReportContainer reportContainer, Pageable pageable) {
        return reportRepository.findByReportContainerId(reportContainer.getId(), pageable).map(ReportDTO::fromEntity);
    }

    /**
     * Get page of field bug metadata from a specific report container
     * @param reportContainer The report container to fetch from.
     * @param pageable The pagination configuration
     * @return The page of field bug metadata, formatted as a ReportFieldDTO
     * @see ReportFieldDTO
     */
    public Page<ReportFieldDTO> findPaginatedReportFieldMetadata(ReportContainer reportContainer, String field, Pageable pageable) {
        //Figure out which field is wanted, and fetch reports page where the field not null or blank.
        Page<Report> reportsPage = null;
        switch (field) {
            case "steps": reportsPage = reportRepository.findByReportContainerIdAndStepsToReproduceIsNotNullAndStepsToReproduceIsNot(reportContainer.getId(), "", pageable); break;
            case "environment": reportsPage =  reportRepository.findByReportContainerIdAndEnvironmentIsNotNullAndEnvironmentIsNot(reportContainer.getId(), "", pageable); break;
            case "expected": reportsPage = reportRepository.findByReportContainerIdAndExpectedResultIsNotNullAndExpectedResultIsNot(reportContainer.getId(), "", pageable); break;
            case "actual": reportsPage = reportRepository.findByReportContainerIdAndActualResultIsNotNullAndActualResultIsNot(reportContainer.getId(), "", pageable); break;
            case "fix-suggestions": reportsPage = reportRepository.findByReportContainerIdAndFixSuggestionsIsNotNullAndFixSuggestionsIsNot(reportContainer.getId(), "", pageable); break;
            case "additional-comments": reportsPage = reportRepository.findByReportContainerIdAndAdditionalCommentsIsNotNullAndAdditionalCommentsIsNot(reportContainer.getId(), "", pageable); break;
            default: return null;
        }
        //Map into ReportFieldDTO and return.
        return reportsPage.map(report -> ReportFieldDTO.fromEntity(report, field));
    }

    /* ---------- Finding Users Reports ---------- */

    /**
     * Method to find page of reports by user in UserReportOverviewDTO format.
     * @see UserReportOverviewDTO
     * @param user The user to find reports from.
     * @param pageable The pagination configuration.
     * @return The page of report overviews in UserReportOverviewDTO format.
     */
    public Page<UserReportOverviewDTO> findPaginatedReportsByUser(User user, Pageable pageable) {
        return reportRepository.findPaginatedReportsByUserWithWorkspaceAndReportContainer(user.getId(), pageable).map(report -> UserReportOverviewDTO.fromEntity(
            report,
            isReportContained(report),
            workspacePermissionsService.isReportWorkspaceVisitableByUser(user, report)
        ));
    }

    /**
     * Method to check if a report has been successfully contained in a report container.
     * @param report The report to check
     * @return true if it has, otherwise false.
     */
    public boolean isReportContained(Report report) {
        return report.getDuplicateDetectionLifecycleState() == DuplicateDetectionLifecycleState.CONTAINED && report.getReportContainer() != null;
    }

    /* ---------- Report Validation Methods ---------- */

    /**
     * Method to check if report summary is valid.
     * This is if it is
     * - not null or blank
     * - maximum 50 characters,
     * - minimum 10 characters
     * @param summary The input to test
     * @return true if valid, otherwise false.
     */
    public boolean isReportSummaryValid(String summary) {
        if (summary == null || summary.isBlank()) return false;
        return summary.length() >= SUMMARY_MIN_LENGTH && summary.length() <= SUMMARY_MAX_LENGTH;
    }

    /**
     * Method to check if report form long text is valid.
     * - maximum 500 characters,
     * - minimum 10 characters (unless optional),
     * - not null / blank (unless optional)
     * @param text The input to test
     * @param optional boolean true if minimum bound does not have to be enforced.
     * @return true if it is valid, otherwise false.
     */
    public boolean isLongInputTextFieldValid(String text, boolean optional) {
        if (text == null || text.isBlank()) {
            //Not optional means required, therefore if text is null then invalid.
            return optional;
        }
        return (optional || text.length() >= LONG_TEXT_MIN_LENGTH) && text.length() <= LONG_TEXT_MAX_LENGTH;
    }

    /**
     * Rating must be an integer from 0 to 4 or 1 to 5, depending on zeroIndexed.
     * @param rating The input to test (must not be null)
     * @param zeroIndexed boolean true if considering zero indexed, otherwise false.
     * @return true if it is valid, otherwise false.
     */
    public boolean isRatingInputValid(int rating, boolean zeroIndexed) {
        return rating >= (zeroIndexed ? 0 : 1) && rating <= (zeroIndexed ? 4 : 5);
    }

    /**
     * Method to check if attachment caption is valid.
     * This is if it is within 100 characters.
     * Attachment caption is allowed to be empty,
     * this could help promote more meaningful captions.
     * @param caption The caption to validate.
     * @return true if it is valid, otherwise false.
     */
    public boolean isAttachmentCaptionValid(String caption) {
        if (caption == null) return true;
        return caption.trim().length() <= ATTACHMENT_CAPTION_MAX_LENGTH;
    }

}
