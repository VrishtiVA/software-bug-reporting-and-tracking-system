package com.example.backend.services.bugs;

import com.example.backend.dto.responses.duplicate_detection.DuplicateDetectionResponse;
import com.example.backend.dto.responses.duplicate_detection.DuplicateSuggestionDTO;
import com.example.backend.dto.responses.duplicate_detection.ReportVectorEmbeddingResponse;
import com.example.backend.dto.responses.duplicate_detection.ResolveDuplicatesOutcome;
import com.example.backend.exceptions.ReportContainerNotFoundException;
import com.example.backend.model.reports.Report;
import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.bugs.ReportContainerStability;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.services.microservices.DuplicateDetectionService;
import com.example.backend.services.users.NotificationService;
import com.example.backend.services.workspaces.WorkspaceLocksService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Note:
 * Methods in this service may call methods in ReportContainerService,
 * However, this method should not call methods in ReportService to avoid cyclic dependencies.
 */
@Service
public class ReportContainerAsyncService {

    private static final Logger log = LoggerFactory.getLogger(ReportContainerAsyncService.class);

    @Autowired
    private DuplicateDetectionService duplicateDetectionService;
    @Autowired
    private ReportContainerService reportContainerService;
    @Autowired
    private WorkspaceLocksService workspaceLocksService;
    @Autowired
    private ReportContainerActivityService reportContainerActivityService;
    @Autowired
    private IncomingDuplicateSuggestionsCountUpdateService incomingDuplicateSuggestionsCountUpdateService;
    @Autowired
    private NotificationService notificationService;

    /**
     * Method to compute and save vector embedding
     * This method has to block the async thread to prevent it from finding duplicates without an embedding.
     * @param report The report to compute and save the vector embedding for.
     */
    public void computeAndSaveVectorEmbedding(Report report) {
        try {
            //Compute a vector embedding for the report, need to sync within the async here.
            ReportVectorEmbeddingResponse vectorEmbeddingResponse = duplicateDetectionService.computeVectorEmbedding(report).join();
            log.debug("Received response from FastAPI microservice in computeAndSaveVectorEmbedding(Report report)");
            //Decode and save embedding
            String encodedVectorEmbedding = vectorEmbeddingResponse.vectorEmbedding();
            if (encodedVectorEmbedding == null) {
                log.warn("Report with ID: {}, did not return with a computed vector embedding.", report.getId());
                return;
            }
            byte[] vectorEmbedding = Base64.getDecoder().decode(encodedVectorEmbedding);
            reportContainerService.storeEmbedding(report, vectorEmbedding);
        } catch (Exception e) {
            log.error("Failed to save report vector embedding in computeAndSaveVectorEmbedding(Report report)", e);
        }
    }

    /**
     * Method to detect duplicates and orchestrate part of the duplicate detection lifecycle design, by
     * - calling the microservice service method for duplicate suggestions of a report,
     * - checking if there are any suggestions
     * - if there are no suggestions, auto-contain the report, which depends on if it's not contained at all or leading a preliminary container.
     * - if there are suggestions, is the most similar one confident, in quick case auto-resolve the duplicate suggestion
     * - if it's not confident, save the duplicate suggestions for later, so that users can be given the option to resolve them.
     * @param report The report to detect duplicates for.
     */
    public void detectDuplicates(Report report, Workspace workspace, User reporter) {
        try {
            //Fetch duplicate suggestions, need to wait for response here.
            DuplicateDetectionResponse duplicateDetectionResponse = duplicateDetectionService.detectDuplicates(report).join();
            log.debug("Received response from FastAPI microservice in detectDuplicates(Report report): {}", duplicateDetectionResponse.toString());
            List<DuplicateSuggestionDTO> duplicateSuggestionDTOList = duplicateDetectionResponse.duplicateSuggestions();

            //If no suggestions found, send report to be auto-contained.
            if (duplicateSuggestionDTOList == null || duplicateSuggestionDTOList.isEmpty()) {
                log.debug("No duplicate suggestions were found for report ID {}, therefore it will have its own stable container.", report.getId());
                containReportInStableContainer(report, false);
                return;
            }

            boolean isFirstSuggestion = true;
            boolean hasPotentialSuggestions = false;
            ResolveDuplicatesOutcome resolveDuplicatesOutcome = null;
            List<Long> incomingSuggestionsForReportContainerIds = new ArrayList<>(); //None if toReport doesn't have any parents.
            //For each suggestion
            Report toReport;
            for (DuplicateSuggestionDTO suggestion : duplicateSuggestionDTOList) {
                //Check report exists
                toReport = reportContainerService.findReportWithEagerReportContainer(suggestion.toReportId());
                if (toReport == null) {
                    log.warn("Found and skipped over duplicate suggestion to non-existent report.");
                    continue;
                }

                //If first suggestion is a confident duplicate suggestion.
                if (isFirstSuggestion && suggestion.isConfident()) {
                    log.debug("Duplicate first suggestion fromReport: {}, toReport: {}, similarityScore {} is confident, therefore, will be auto-resolved.", report.getId(), toReport.getId(), suggestion.similarityScore());
                    //Automatically merge
                    resolveDuplicatesOutcome = containReportWithDuplicateSuggestion(report, toReport, false);
                    log.debug("Auto resolve confident duplicate suggestion outcome: {}", resolveDuplicatesOutcome);
                    break;
                }
                isFirstSuggestion = false;

                //Otherwise if they're suggestions that needs confirmation, save duplicate suggestions for later.
                hasPotentialSuggestions = true;
                log.debug("Duplicate suggestion fromReport: {}, toReport: {}, similarityScore: {}, similarityOrder: {}", report.getId(), toReport.getId(), suggestion.similarityScore(), suggestion.similarityOrder());
                //Create duplicate suggestion object
                reportContainerService.createDuplicateSuggestion(
                    report,
                    toReport,
                    suggestion.similarityOrder(),
                    suggestion.similarityScore()
                );
                //Add the report containers of any toReport to list of those that need updates.
                if (toReport.getReportContainer() != null) {
                    log.debug("Report Container ID:{}'s incoming duplicate suggestions count needs updating, from detectDuplicates", toReport.getReportContainer().getId());
                    incomingSuggestionsForReportContainerIds.add(toReport.getReportContainer().getId());
                }
            }

            if (resolveDuplicatesOutcome != null) {
                Long reportContainerId = resolveDuplicatesOutcome.reportContainerId();
                //Add to audit log if was automatically resolved
                reportContainerActivityService.logReportContainerDuplicateAutoResolved(
                    reportContainerId,
                    report.getId(),
                    resolveDuplicatesOutcome.reportsRelocated()
                );
                //Notify any assignees that new duplicates have been added.
                if (reportContainerId != null) {
                    List<User> assignees = reportContainerService.findAssignedUsers(reportContainerId);
                    if (!assignees.isEmpty()) notificationService.sendMassNewDuplicatesAddedNotification(assignees, reportContainerId, workspace);
                }
                //Notify reporter that their bug report was discovered as a duplicate
                notificationService.sendReportDiscoveredAsDuplicateNotification(reporter, reportContainerId, workspace, report.getId(), resolveDuplicatesOutcome.reportContainerStability());

            } else if (hasPotentialSuggestions) {
                //Update status since potential duplicate suggestions were found and saved.
                reportContainerService.updateReportDuplicateDetectionLifecycleStateToOutstanding(report);
                if (!incomingSuggestionsForReportContainerIds.isEmpty()) {
                    //Also send to update suggestions for relevant containers.
                    incomingDuplicateSuggestionsCountUpdateService.markReportContainersDirty(incomingSuggestionsForReportContainerIds);
                    //Notify any assignees of any that new duplicate suggestions are available for their assigned bug.
                    Map<Long, List<User>> reportContainerAssignedUsersMap = reportContainerService.findAssignedUsersPerReportContainerId(incomingSuggestionsForReportContainerIds);
                    List<User> assignees;
                    for (Long reportContainerId : reportContainerAssignedUsersMap.keySet()) {
                        assignees = reportContainerAssignedUsersMap.get(reportContainerId);
                        if (!assignees.isEmpty()) notificationService.sendMassNewRelevantDuplicateSuggestionsNotification(assignees, reportContainerId, workspace);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed in detectDuplicates(Report report)", e);
        }
    }

    /**
     * Method to contain a report in a stable container,
     * This could mean creating a new stable container for the report, or
     * turning a preliminary container into a stable one.
     * This method requires the duplicate detection lock for the workspace, to avoid race conditions.
     * @param report The report that should be contained in a stable container.
     */
    public void containReportInStableContainer(Report report, boolean cleanDuplicateSuggestions) {
        //Get the workspace level lock for resolving duplicates to avoid race conditions when assigning containers.
        ReentrantLock workspaceResolveDuplicatesLock = workspaceLocksService.getResolveDuplicatesLockByWorkspace(report.getWorkspace().getId());
        workspaceResolveDuplicatesLock.lock();
        try {
            report = reportContainerService.findReportWithEagerReportContainerAndReports(report.getId());
            if (report.getReportContainer() != null && report.getReportContainer().getPrimaryReport() == report) {
                //If report is the primary of a preliminary container, make the preliminary container stable.
                reportContainerService.makeReportContainerStable(report.getReportContainer().getId());
            } else {
                //Otherwise, the report needs a new stable container
                reportContainerService.containReportInNewStableReportContainer(report.getId());
            }
            //Handle cleanup of duplicate suggestions if necessary
            if (cleanDuplicateSuggestions) {
                //Find report containers of reports toReports, and save for update.
                List<Long> forReportContainerIds = reportContainerService.findReportContainerIdsOfToReportsByFromReport(report.getId());
                //Delete duplicate suggestions from report
                reportContainerService.deleteDuplicateSuggestionsByReport(report.getId());
                //Send to update necessary containers.
                incomingDuplicateSuggestionsCountUpdateService.markReportContainersDirty(forReportContainerIds);
            }
        } catch (Exception e) {
            log.error("Failed in containReportInStableContainer(Report report)", e);
        } finally {
            //Release the lock if finished here or failed.
            workspaceResolveDuplicatesLock.unlock();
        }
    }

    /**
     * Method to contain report with its duplicate suggestion.
     * This method requires the duplicate detection lock for the workspace, to avoid race conditions.
     * @param fromReport The report that becomes a duplicate
     * @param toReport The report that it was found to be duplicate of, and will be contained with.
     */
    public ResolveDuplicatesOutcome containReportWithDuplicateSuggestion(Report fromReport, Report toReport, boolean cleanDuplicateSuggestions) {
        //Get the workspace level lock for resolving duplicates to avoid race conditions when assigning containers.
        ReentrantLock workspaceResolveDuplicatesLock = workspaceLocksService.getResolveDuplicatesLockByWorkspace(fromReport.getWorkspace().getId());
        workspaceResolveDuplicatesLock.lock();
        ResolveDuplicatesOutcome resolveDuplicatesOutcome = null;
        try {
            fromReport = reportContainerService.findReportWithEagerReportContainerAndReports(fromReport.getId());
            toReport = reportContainerService.findReportWithEagerReportContainerAndReports(toReport.getId());
            boolean hostsPreliminary = false;
            ReportContainer fromPreliminary = null;
            if (fromReport.getReportContainer() != null &&
                fromReport.getReportContainer().getStability() == ReportContainerStability.PRELIMINARY &&
                fromReport.getReportContainer().getPrimaryReport() == fromReport) {
                //If from report is the primary of a preliminary, we know that we need to handle members of its preliminary container too.
                hostsPreliminary = true;
                fromPreliminary = fromReport.getReportContainer();
            }
            if (toReport.getReportContainer() == null) {
                //If toReport is not a member of any container (any status), create a new PRELIMINARY container with toReport as its primary, and mark fromReport as STAGED.
                ReportContainer toPreliminary = reportContainerService.containReportsInNewPreliminaryReportContainer(fromReport.getId(), toReport.getId());
                if (toPreliminary == null) throw new ReportContainerNotFoundException("Didn't receive a newly created PRELIMINARY report container.");
                //If fromReport was the primary of a PRELIMINARY container, move its members too.
                if (hostsPreliminary && fromPreliminary != null) {
                    resolveDuplicatesOutcome = ResolveDuplicatesOutcome.fromData(toPreliminary.getId(), fromPreliminary.getReports().size(), ReportContainerStability.PRELIMINARY);
                    reportContainerService.moveReportsFromPreliminaryToPreliminaryReportContainer(fromPreliminary.getId(), toPreliminary.getId());
                } else {
                    resolveDuplicatesOutcome = ResolveDuplicatesOutcome.fromData(toPreliminary.getId(), 1, ReportContainerStability.PRELIMINARY);
                }
            } else if (toReport.getReportContainer().getStability() == ReportContainerStability.PRELIMINARY) {
                //If toReport belongs to a PRELIMINARY report container, add fromReport to this container and mark it as STAGED.
                ReportContainer toPreliminary = toReport.getReportContainer();
                reportContainerService.stageReportInPreliminaryContainer(fromReport.getId(), toPreliminary.getId());
                //If fromReport was the primary of a PRELIMINARY container, move its members too.
                if (hostsPreliminary && fromPreliminary != null) {
                    resolveDuplicatesOutcome = ResolveDuplicatesOutcome.fromData(toPreliminary.getId(), fromPreliminary.getReports().size(), ReportContainerStability.PRELIMINARY);
                    reportContainerService.moveReportsFromPreliminaryToPreliminaryReportContainer(fromPreliminary.getId(), toPreliminary.getId());
                } else {
                    resolveDuplicatesOutcome = ResolveDuplicatesOutcome.fromData(toPreliminary.getId(), 1, ReportContainerStability.PRELIMINARY);
                }
            } else if (toReport.getReportContainer().getStability() == ReportContainerStability.STABLE) {
                //If toReport belongs to a STABLE report container, add fromReport to this container and mark it as CONTAINED.
                ReportContainer toStable = toReport.getReportContainer();
                reportContainerService.containReportInStableContainer(fromReport.getId(), toStable.getId());
                //If fromReport was the primary of a STABLE container, move its members too, and also mark them as CONTAINED.
                if (hostsPreliminary && fromPreliminary != null) {
                    resolveDuplicatesOutcome = ResolveDuplicatesOutcome.fromData(toStable.getId(), fromPreliminary.getReports().size(), ReportContainerStability.STABLE);
                    reportContainerService.moveReportsFromPreliminaryToStableReportContainer(fromPreliminary.getId(), toStable.getId());
                } else {
                    resolveDuplicatesOutcome = ResolveDuplicatesOutcome.fromData(toStable.getId(), 1, ReportContainerStability.STABLE);
                }
            }
            //Handle cleanup of duplicate suggestions if necessary
            if (cleanDuplicateSuggestions) {
                //Find report containers of reports toReports, and save for update.
                List<Long> forReportContainerIds = reportContainerService.findReportContainerIdsOfToReportsByFromReport(fromReport.getId());
                //Delete duplicate suggestions from report
                reportContainerService.deleteDuplicateSuggestionsByReport(fromReport.getId());
                //Send to update necessary containers.
                incomingDuplicateSuggestionsCountUpdateService.markReportContainersDirty(forReportContainerIds);
            }
            return resolveDuplicatesOutcome;
        } catch (Exception e) {
            log.warn("Failed in containReportWithDuplicateSuggestion(Report fromReport, Report toReport)", e);
            return resolveDuplicatesOutcome;
        } finally {
            //Release the lock if finished here or failed.
            workspaceResolveDuplicatesLock.unlock();
        }
    }

    /**
     * Method to start off the duplicate detection lifecycle, which is when a new report arrives.
     * It orchestrates computing and storing the vector embedding, before sending it off for duplicate detection.
     * @param report The new report that just came in.
     */
    @Async
    public void handleNewReport(Report report, Workspace workspace, User reporter) {
        try {
            //A new report has just come in, its state in lifecycle is new.
            //First compute a vector embedding for the new report, and save it.
            computeAndSaveVectorEmbedding(report);
            log.debug("Flow returned to handle new report after computing and saving vector embedding.");
            //Duplicate detection
            detectDuplicates(report, workspace, reporter);
            log.debug("Finished handling new report after duplicate detection.");
        } catch (Exception e) {
            log.error("Failed in handleNewReport(Report report)", e);
        }
    }

    /**
     * Method to accept a duplicate suggest for a report.
     * This method runs asynchronously to allow controller to return while this sends request to method that waits for lock.
     * @param fromReportId The ID of the report to contain
     * @param toReportId The ID of the report to contain with.
     */
    @Async
    public void acceptADuplicateSuggestionForReport(Long fromReportId, Long toReportId, User performer, WorkspaceRole performerRole, Workspace workspace) {
        try {
            Report fromReport = reportContainerService.findReportByIdWithUser(fromReportId);
            Report toReport = reportContainerService.findReportById(toReportId);
            User reporter = fromReport.getUser();
            //Merge
            ResolveDuplicatesOutcome resolveDuplicatesOutcome = containReportWithDuplicateSuggestion(fromReport, toReport, true);
            log.debug("Accept duplicate suggestion outcome: {}", resolveDuplicatesOutcome);
            Long reportContainerId = resolveDuplicatesOutcome.reportContainerId();

            //Add to audit log
            reportContainerActivityService.logReportContainerDuplicateResolved(
                reportContainerId,
                performer,
                performerRole,
                fromReportId,
                resolveDuplicatesOutcome.reportsRelocated()
            );
            //Notify any assignees that new duplicates have been added.
            if (reportContainerId != null) {
                List<User> assignees = reportContainerService.findAssignedUsers(reportContainerId);
                if (!assignees.isEmpty()) notificationService.sendMassNewDuplicatesAddedNotification(assignees, reportContainerId, workspace);
            }
            //Notify reporter that their bug report was discovered as a duplicate
            notificationService.sendReportDiscoveredAsDuplicateNotification(reporter, reportContainerId, workspace, fromReportId, resolveDuplicatesOutcome.reportContainerStability());

            log.info("Finished handling accept report duplicate suggestion request for Report ID:{}.", fromReport.getId());
        } catch (Exception e) {
            log.error("Failed in acceptADuplicateSuggestionForReport(Report report)", e);
        }
    }

    /**
     * Method to reject all duplicate suggestions for a report, and to allow the report to lead its own container.
     * This method runs asynchronously to allow controller to return while this sends request to method that waits for lock.
     * @param reportId The ID of the report
     */
    @Async
    public void rejectAllDuplicateSuggestionsForReport(Long reportId) {
        try {
            Report report = reportContainerService.findReportById(reportId);
            containReportInStableContainer(report, true);
            log.info("Finished handling reject report duplicate suggestions request for Report ID:{}.", report.getId());
        } catch (Exception e) {
            log.error("Failed in rejectAllDuplicateSuggestionsForReport(Report report)", e);
        }
    }

}
