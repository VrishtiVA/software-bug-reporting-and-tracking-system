package com.example.backend.services.bugs;

import com.example.backend.dto.responses.bugs.*;
import com.example.backend.dto.responses.duplicate_detection.DuplicateSuggestionsToResolveDTO;
import com.example.backend.dto.responses.workspaces.WorkspaceCustomLabelDTO;
import com.example.backend.dto.responses.workspaces.metrics.WorkspaceCustomLabelUsageDTO;
import com.example.backend.exceptions.ReportNotFoundException;
import com.example.backend.model.bugs.*;
import com.example.backend.model.reports.*;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import com.example.backend.model.workspaces.WorkspaceMember;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.repositories.bugs.*;
import com.example.backend.repositories.bugs.projections.ProgressDistributionProjection;
import com.example.backend.repositories.bugs.projections.ReportContainerAssigneeCountDataProjection;
import com.example.backend.repositories.bugs.projections.ReportContainerUserAssigneeProjection;
import com.example.backend.repositories.bugs.projections.StatusDistributionProjection;
import com.example.backend.repositories.reports.DuplicateSuggestionRepository;
import com.example.backend.repositories.reports.ReportRepository;
import com.example.backend.repositories.workspaces.ReportContainerWorkspaceCustomLabelMappingRepository;
import com.example.backend.repositories.workspaces.WorkspaceMemberRepository;
import com.example.backend.services.workspaces.WorkspacePermissionsService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReportContainerService {

    private static final Logger log = LoggerFactory.getLogger(ReportContainerService.class);

    @Autowired
    private ReportRepository reportRepository;
    @Autowired
    private ReportContainerRepository reportContainerRepository;
    @Autowired
    private ReportContainerPriorityService reportContainerPriorityService;
    @Autowired
    private DuplicateSuggestionRepository duplicateSuggestionRepository;
    @Autowired
    private ReportContainerAssigneeRepository reportContainerAssigneeRepository;
    @Autowired
    private WorkspaceMemberRepository workspaceMemberRepository;
    @Autowired
    private ReportContainerWorkspaceCustomLabelMappingRepository reportContainerWorkspaceCustomLabelMappingRepository;
    @Autowired
    private ReportContainerActivityService reportContainerActivityService;
    @Autowired
    private ReportContainerSearchService reportContainerSearchService;
    @Autowired
    private IncomingDuplicateSuggestionsCountUpdateService incomingDuplicateSuggestionsCountUpdater;

    @PersistenceContext
    private EntityManager entityManager;
    @Autowired
    private WorkspacePermissionsService workspacePermissionsService;

    /* ---------- Report Container Query Methods ---------- */

    /**
     * Method to find stable report containers by workspace.
     * @param workspace The workspace to find stable report containers in.
     * @return The list of stable report containers.
     */
    public List<ReportContainer> findStableReportContainersByWorkspace(Workspace workspace) {
        return reportContainerRepository.findByWorkspaceIdAndStability(workspace.getId(), ReportContainerStability.STABLE);
    }

    /**
     * Method to compose a page of report containers into a report container overview page with fuller details.
     * @param bugResults The page to convert
     * @param workspaceId The workspace ID where the report containers are from. This could be null here.
     * @return The page of report container overview DTOs.
     */
    public Page<ReportContainerOverviewDTO> composeReportContainerOverviewResultsAsPage(
            Page<ReportContainer> bugResults,
            Long workspaceId
    ) {
        List<ReportContainerOverviewDTO> reportContainerOverviewDTOList = new ArrayList<>();

        //Figure out which bugs were returned.
        List<Long> reportContainerIds = new ArrayList<>();
        for (ReportContainer rc : bugResults) { reportContainerIds.add(rc.getId()); }

        //Find assignee count data only for all the bugs returned (1 query)
        List<ReportContainerAssigneeCountDataProjection> reportContainerAssigneeCount = reportContainerAssigneeRepository.countByWorkspaceIdAndStabilityAndInReportContainerIds(
                workspaceId,
                ReportContainerStability.STABLE.name(), //Native queries have to handle enum as string, hence .name()
                reportContainerIds
        );
        log.debug("Received report assignee count data: {}", reportContainerAssigneeCount);
        Map<Long, Long> assigneesCountMap = new HashMap<>();
        for (ReportContainerAssigneeCountDataProjection assigneeCountData : reportContainerAssigneeCount) {
            assigneesCountMap.put(assigneeCountData.getReportContainerId(), assigneeCountData.getAssigneeCount());
        }
        log.debug("Mapped report assignee count data: {}", assigneesCountMap);

        //Compose data together into DTO, with n queries for limited assignee details (this is acceptable as data is paginated).
        for (ReportContainer rc : bugResults) {
            reportContainerOverviewDTOList.add(ReportContainerOverviewDTO.fromEntity(
                    rc,
                    reportContainerAssigneeRepository.findTop3AssigneesAsWorkspaceMemberByReportContainer(rc.getId()),
                    assigneesCountMap.get(rc.getId()),
                    reportContainerPriorityService.convertToPriorityCategory(rc.getPriority())
            ));
        }

        //Wrap up complete response in page
        return new PageImpl<>(
                reportContainerOverviewDTOList,
                bugResults.getPageable(),
                bugResults.getTotalElements()
        );
    }

    public Page<ReportContainerOverviewDTO> findPaginatedReportContainerSearchResultsByWorkspaceAsDTO(
            String query,
            Workspace workspace,
            Pageable pageable,
            List<ReportContainerProgress> includeProgress,
            List<ReportContainerStatus> includeStatus,
            List<String> includeLabels
    ) {
        //Fetch resulting bugs (1 query)
        Page<ReportContainer> bugSearchResults = reportContainerSearchService.findPaginatedStableBugResultsByWorkspaceId(query, workspace.getId(), pageable, includeProgress, includeStatus, includeLabels);
        log.debug("Received search results: Total Elements: {}", bugSearchResults.getTotalElements());

        //Compose results into a page with fuller details, and return.
        return composeReportContainerOverviewResultsAsPage(bugSearchResults, workspace.getId());
    }

    public ReportContainer findReportContainerById(Long reportContainerId) {
        return reportContainerRepository.findById(reportContainerId).orElse(null);
    }

    public ReportContainerDetailsDTO findReportContainerDetailsAsDTO(ReportContainer reportContainer, WorkspaceRole workspaceRole) {
        List<AssigneeDetailsOverviewDTO> assignees = findAssigneeDetailsAsDTO(reportContainer);
        return ReportContainerDetailsDTO.fromEntity(
                reportContainer,
                workspaceRole,
                assignees,
                reportContainerPriorityService.convertToPriorityCategory(reportContainer.getPriority())
        );
    }

    public Long countActiveReportContainersByWorkspace(Workspace workspace) {
        return reportContainerRepository.countActiveReportContainersByWorkspaceId(workspace.getId(), ReportContainerProgress.CLOSED, ReportContainerStability.STABLE);
    }

    public List<WorkspaceCustomLabelUsageDTO> countActiveWorkspaceCustomLabelUsageInWorkspace(Workspace workspace) {
        return reportContainerRepository.findWorkspaceCustomLabelUsageInReportContainersDistributionByWorkspaceId(
                workspace.getId(), ReportContainerProgress.CLOSED, ReportContainerStability.STABLE
        ).stream().map(label -> WorkspaceCustomLabelUsageDTO.fromData(
                WorkspaceCustomLabelDTO.fromData(label.getWorkspaceCustomLabelName(), label.getWorkspaceCustomLabelColour()),
                label.getUsageCount()
        )).toList();
    }

    public Map<ReportContainerProgress, Integer> findProgressDistributionByWorkspace(Workspace workspace) {
        return reportContainerRepository.findReportContainerProgressDistributionByWorkspaceId(
                workspace.getId(), ReportContainerProgress.CLOSED, ReportContainerStability.STABLE
        ).stream().collect(Collectors.toMap(
                ProgressDistributionProjection::getProgress,
                ProgressDistributionProjection::getBugCount
        ));
    }
    public Map<ReportContainerStatus, Integer> findStatusDistributionByWorkspace(Workspace workspace) {
        return reportContainerRepository.findReportContainerStatusDistributionByWorkspaceId(
                workspace.getId(), ReportContainerProgress.CLOSED, ReportContainerStability.STABLE
        ).stream().collect(Collectors.toMap(
                StatusDistributionProjection::getStatus,
                StatusDistributionProjection::getBugCount
        ));
    }

    public Long countByWorkspaceAndReportContainerStatusIsResolved(Workspace workspace) {
        return reportContainerRepository.countByWorkspaceAndStatusAndStability(workspace, ReportContainerStatus.RESOLVED, ReportContainerStability.STABLE);
    }

    /* ---------- Report Containerisation Methods ---------- */

    @Transactional
    public void containReportInNewStableReportContainer(Long reportId) {
        try {
            //Find the report
            Report report = reportRepository.findById(reportId).orElse(null);
            if (report == null) {
                throw new ReportNotFoundException("Report not found");
            }

            //Create new container
            ReportContainer newReportContainer = new ReportContainer(
                report.getWorkspace(),
                report.getSummaryTitle(),
                report,
                ReportContainerStability.STABLE
            );
            newReportContainer.setFirstReportedTimestamp(report.getCreationTimestamp());
            newReportContainer = reportContainerRepository.save(newReportContainer);
            log.debug("New STABLE report container created.");

            //Contain report
            newReportContainer = containReportInStableContainer(reportId, newReportContainer.getId());
            log.debug("Report contained in a new stable report container.");
            //Add to audit log.
            reportContainerActivityService.logReportContainerCreation(newReportContainer.getId(), reportId);
            //Mark as dirty to update suggestions count later.
            incomingDuplicateSuggestionsCountUpdater.markReportContainerDirty(newReportContainer.getId());

        } catch (Exception e) {
            log.error("Failed to contain report in a new STABLE container in containReportInNewStableReportContainer(Long reportId)", e);
        }
    }

    @Transactional
    public void makeReportContainerStable(Long reportContainerId) {
        //Update report container stability to STABLE.
        reportContainerRepository.updateStability(reportContainerId, ReportContainerStability.STABLE);
        log.info("Report Container with ID {} is now stable", reportContainerId);
        //Make status of all reports in the container CONTAINED too.
        reportRepository.bulkUpdateDuplicateDetectionLifecycleStateByReportContainer(reportContainerId, DuplicateDetectionLifecycleState.CONTAINED);
        log.debug("Reports in Report Container with ID {} now set to CONTAINED in Duplicate Detection Lifecycle", reportContainerId);

        //Since bulk updates can bypass persistence context, this ensures the next find loads it fresh from the database.
        entityManager.flush();
        entityManager.clear();

        reportContainerSearchService.updateSearchIndexForReportContainer(reportContainerId);
    }

    /**
     * Method to create a new PRELIMINARY container for the reports, where
     * - the primary report is toReport, and
     * - the duplicate detection lifecycle state of fromReport is set to STAGED.
     * @param fromReportId The report the suggestion was for
     * @param toReportId The report to lead the preliminary container.
     */
    @Transactional
    public ReportContainer containReportsInNewPreliminaryReportContainer(Long fromReportId, Long toReportId) {
        try {
            //Find the reports
            Report fromReport = reportRepository.findById(fromReportId).orElse(null);
            if (fromReport == null) throw new ReportNotFoundException("fromReport not found");
            Report toReport = reportRepository.findById(toReportId).orElse(null);
            if (toReport == null) throw new ReportNotFoundException("toReport not found");

            //Create new container
            ReportContainer newReportContainer = new ReportContainer(
                fromReport.getWorkspace(),
                toReport.getSummaryTitle(),
                toReport,
                ReportContainerStability.PRELIMINARY
            );
            //Set first report timestamp to timestamp of oldest report.
            newReportContainer.setFirstReportedTimestamp(Collections.min(List.of(
                fromReport.getCreationTimestamp(),
                toReport.getCreationTimestamp())
            ));
            newReportContainer = reportContainerRepository.save(newReportContainer);
            log.debug("New PRELIMINARY report container created.");

            //Contain the reports
            newReportContainer = holdReportInPreliminaryContainer(toReport, newReportContainer);
            newReportContainer = stageReportInPreliminaryContainer(fromReportId, newReportContainer.getId());

            //Add to audit log
            reportContainerActivityService.logReportContainerCreation(newReportContainer.getId(), toReportId);
            //Mark as dirty to update suggestions count later.
            incomingDuplicateSuggestionsCountUpdater.markReportContainerDirty(newReportContainer.getId());

            //Return new preliminary container
            return newReportContainer;
        } catch (Exception e) {
            log.error("Failed to container reports in a new PRELIMINARY container in containReportsInNewPreliminaryReportContainer(Long fromReportId, Long toReportId)", e);
            return null;
        }
    }

    @Transactional
    public ReportContainer addLabelsToReportContainer(ReportContainer reportContainer, Report report) {

        //Fetch labels from report not in report container.
        List<WorkspaceCustomLabel> newLabels = reportContainerRepository.findWorkspaceCustomLabelsInReportNotInReportContainer(report.getId(), reportContainer.getId());

        //Then add labels.
        for (WorkspaceCustomLabel label : newLabels) {
            reportContainer.getWorkspaceCustomLabels().add(new ReportContainerWorkspaceCustomLabelMapping(
                reportContainer,
                label
            ));
        }
        return reportContainerRepository.save(reportContainer);
    }

    @Transactional
    public ReportContainer mergeReportContainerLabels(ReportContainer fromReportContainer, ReportContainer toReportContainer) {

        //Find all unique label mapping objects from labels in fromReportContainer not in toReportContainer
        List<WorkspaceCustomLabel> newLabels = reportContainerRepository.findWorkspaceCustomLabelsInReportContainerNotInReportContainer(fromReportContainer.getId(), toReportContainer.getId());

        //Add new labels to toReportContainer.
        for (WorkspaceCustomLabel label : newLabels) {
            toReportContainer.getWorkspaceCustomLabels().add(new ReportContainerWorkspaceCustomLabelMapping(
                toReportContainer,
                label
            ));
        }
        return reportContainerRepository.save(toReportContainer);
    }

    /**
     * Method to move reports from a preliminary container to another preliminary container, and
     * then discard the earlier preliminary container since it's now empty.
     * @param fromPreliminaryId The ID of the container to move reports from and then delete.
     * @param toPreliminaryId The ID of the container to move reports to.
     */
    @Transactional
    public void moveReportsFromPreliminaryToPreliminaryReportContainer(Long fromPreliminaryId, Long toPreliminaryId) {
        ReportContainer fromPreliminary = reportContainerRepository.findById(fromPreliminaryId).orElse(null);
        ReportContainer toPreliminary = reportContainerRepository.findById(toPreliminaryId).orElse(null);
        if (fromPreliminary == null || toPreliminary == null) {
            log.error("fromPreliminary container or toPreliminary container not found in moveReportsFromPreliminaryToPreliminaryReportContainer(Long fromPreliminaryId, Long toPreliminaryId)");
            return;
        }
        toPreliminary = mergeReportContainerLabels(fromPreliminary, toPreliminary);
        reportRepository.updateReportContainerFromReportContainer(fromPreliminary, toPreliminary);
        log.debug("Moved reports from preliminary container to preliminary container.");
        reportContainerRepository.updateFirstReportedTimestamp(toPreliminaryId);
        log.debug("Updated first reported timestamp of toPreliminary report container: {}.", toPreliminaryId);
        //This should not cause hibernate issues as working through database.
        reportContainerPriorityService.updateReportContainerMetrics(toPreliminary);

        //Update search index for toPreliminary report container
        entityManager.flush(); //Flush now so hibernate can know of the changes and update the index.
        reportContainerSearchService.updateSearchIndexForReportContainer(toPreliminaryId);
        log.debug("Updated lucene search index for toPreliminary report container: {}.", toPreliminaryId);

        //Delete old report container now, since merged.
        reportContainerRepository.delete(fromPreliminary);
        log.debug("Discarded empty preliminary container moveReportsFromPreliminaryToPreliminaryReportContainer(ReportContainer fromPreliminary, ReportContainer toPreliminary)");
    }

    /**
     * Method to move reports from a preliminary container to a stable container, whilst
     * marking these reports as CONTAINED,
     * and then discarding the earlier preliminary container since it's now empty.
     * @param fromPreliminaryId The ID of the container to move the reports from and then delete.
     * @param toStableId The ID of the container to move the reports to.
     */
    @Transactional
    public void moveReportsFromPreliminaryToStableReportContainer(Long fromPreliminaryId, Long toStableId) {
        ReportContainer fromPreliminary = reportContainerRepository.findById(fromPreliminaryId).orElse(null);
        ReportContainer toStable = reportContainerRepository.findById(toStableId).orElse(null);
        if (fromPreliminary == null || toStable == null) {
            log.error("fromPreliminary container or toStable container not found in moveReportsFromPreliminaryToStableReportContainer(Long fromPreliminaryId, Long toPreliminaryId)");
            return;
        }
        toStable = mergeReportContainerLabels(fromPreliminary, toStable);
        reportRepository.updateReportContainerAndDuplicateDetectionLifecycleStateFromReportContainer(fromPreliminary, toStable, DuplicateDetectionLifecycleState.CONTAINED);
        log.debug("Moved reports from preliminary container to stable container, and marked as CONTAINED.");
        reportContainerRepository.updateFirstReportedTimestamp(toStableId); //Has flush auto, helping to avoid hibernate issues in the remaining of this method.
        log.debug("Updated first reported timestamp of toStable report container: {}.", toStableId);
        //This should not cause hibernate issues as working through database.
        reportContainerPriorityService.updateReportContainerMetrics(toStable);

        //Update search index for toStable report container
        entityManager.flush(); //Flush now so hibernate can know of the changes and update the index.
        reportContainerSearchService.updateSearchIndexForReportContainer(toStableId);
        log.debug("Updated lucene search index for toStable report container: {}.", toStableId);

        reportContainerRepository.delete(fromPreliminary);
        log.debug("Discarded empty preliminary container in moveReportsFromPreliminaryToStableReportContainer(ReportContainer fromPreliminary, ReportContainer toStable)");
    }

    /* ---------- Report Related Actions ---------- */

    /**
     * Method to store the vector embedding for a specific report.
     * propagation = Propagation.REQUIRES_NEW is used to ensure the transaction commits before this method returns,
     * this ensures that other service methods that need to read the data will have the data visible for them (e.g. the microservice).
     * <a href="https://wynnt3o.hashnode.dev/understanding-spring-transaction-propagation#heading-propagationrequiresnew">Propagation.REQUIRES_NEW</a>
     * @param report The report to store the vector embedding for.
     * @param vectorEmbedding The vector embedding in byte form to store.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void storeEmbedding(Report report, byte[] vectorEmbedding) {
        reportRepository.updateVectorEmbedding(report.getId(), vectorEmbedding, Instant.now());
        reportRepository.updateDuplicateDetectionLifecycleState(report.getId(), DuplicateDetectionLifecycleState.READY);
        log.info("Report ID {} is ready for duplicate detection with report embedding stored", report.getId());
    }

    /**
     * Method to contain a report in a stable container by
     *      settings its DuplicateDetectionLifecycleState to CONTAINED
     *      and setting its report_container to the specific container.
     * @param reportId The ID of the report to contain
     * @param reportContainerId The ID of the container to contain the report in.
     */
    @Transactional
    public ReportContainer containReportInStableContainer(Long reportId, Long reportContainerId) {
        //Find report container
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElse(null);
        if (reportContainer == null) {
            log.warn("No report container with ID:{} found in containReportInStableContainer(Long reportId, Long reportContainerId)", reportContainerId);
            return null;
        }
        //Find report
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            log.warn("No report with ID:{} found in containReportInStableContainer(Long reportId, Long reportContainerId)", reportId);
            return null;
        }
        //Add reports labels to container
        reportContainer = addLabelsToReportContainer(reportContainer, report);
        //Update report location and states
        reportRepository.updateReportContainerAndDuplicateDetectionLifecycleState(reportId, reportContainer, DuplicateDetectionLifecycleState.CONTAINED);
        log.debug("Report ID {} contained and marked as CONTAINED in duplicate detection lifecycle", reportId);
        //This should not cause hibernate issues as working through database.
        reportContainerPriorityService.updateReportContainerMetrics(reportContainer);
        entityManager.clear();
        entityManager.flush();

        //Update search index for report container
        reportContainerSearchService.updateSearchIndexForReportContainer(reportContainerId);
        log.debug("Updated lucene search index for report container: {}, in containReportInStableContainer.", reportContainerId);

        return reportContainer;
    }

    /**
     * Method to hold report in a preliminary container by
     *      setting its report_container to the specific container and
     *      without updating its DuplicateDetectionLifecycleState to STAGED.
     * @param report The report to contain
     * @param reportContainer The container to hold the report in.
     */
    @Transactional
    public ReportContainer holdReportInPreliminaryContainer(Report report, ReportContainer reportContainer) {
        //Add reports labels to report container
        Long reportContainerId = reportContainer.getId();
        reportContainer = addLabelsToReportContainer(reportContainer, report);
        reportRepository.updateReportContainer(report.getId(), reportContainer);
        log.debug("Report ID {} contained in preliminary report container", report.getId());
        //This should not cause hibernate issues as working through database.
        reportContainerPriorityService.updateReportContainerMetrics(reportContainer);
        entityManager.flush();
        entityManager.clear();

        //Update search index for report container
        reportContainerSearchService.updateSearchIndexForReportContainer(reportContainerId);
        log.debug("Updated lucene search index for report container: {}, in holdReportInPreliminaryContainer.", reportContainer.getId());

        return reportContainer;
    }

    /**
     * Method to hold report in a preliminary container by
     *      updating its report_container to the specific container and
     *      setting its DuplicateDetectionLifecycleState to STAGED.
     * @param reportId The ID of the report to contain.
     * @param reportContainerId The ID of the container to stage the report in.
     */
    @Transactional
    public ReportContainer stageReportInPreliminaryContainer(Long reportId, Long reportContainerId) {
        //Find report container
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElse(null);
        if (reportContainer == null) {
            log.warn("No report container with ID:{} found in stageReportInPreliminaryContainer(Long reportId, Long reportContainerId)", reportContainerId);
            return null;
        }
        //Find report
        Report report = reportRepository.findById(reportId).orElse(null);
        if (report == null) {
            log.warn("No report with ID:{} found in stageReportInPreliminaryContainer(Long reportId, Long reportContainerId)", reportId);
            return null;
        }
        //Add labels from report to container.
        reportContainer = addLabelsToReportContainer(reportContainer, report);
        //Update report location and state.
        reportRepository.updateReportContainerAndDuplicateDetectionLifecycleState(reportId, reportContainer, DuplicateDetectionLifecycleState.STAGED);
        log.debug("Report ID {} staged and marked as STAGED in duplicate detection lifecycle", reportId);
        //This should not cause hibernate issues as working through database.
        reportContainerPriorityService.updateReportContainerMetrics(reportContainer);
        entityManager.flush();
        entityManager.clear();

        //Update search index for report container
        reportContainerSearchService.updateSearchIndexForReportContainer(reportContainerId);
        log.debug("Updated lucene search index for report container: {}, in stageReportInPreliminaryContainer.", reportContainer.getId());

        return reportContainer;
    }

    @Transactional
    public void updateReportDuplicateDetectionLifecycleStateToOutstanding(Report report) {
        reportRepository.updateDuplicateDetectionLifecycleState(report.getId(), DuplicateDetectionLifecycleState.OUTSTANDING);
        log.info("Report ID {} marked as OUTSTANDING in duplicate detection lifecycle", report.getId());
    }

    @Transactional
    public void updateReportDuplicateDetectionLifecycleStateToResolved(Report report) {
        reportRepository.updateDuplicateDetectionLifecycleState(report.getId(), DuplicateDetectionLifecycleState.RESOLVED);
        log.info("Report ID {} marked as RESOLVED in duplicate detection lifecycle", report.getId());
    }

    @Transactional
    public void createDuplicateSuggestion(Report fromReport, Report toReport, int similarityOrder, double similarityScore) {
        DuplicateSuggestion duplicateSuggestion = new DuplicateSuggestion(
            fromReport,
            toReport,
            similarityOrder,
            similarityScore
        );
        duplicateSuggestionRepository.save(duplicateSuggestion);
    }

    /* ---------- Report Related Queries for Report Containers ---------- */

    public Report findReportById(Long reportId) {
        return reportRepository.findById(reportId).orElse(null);
    }

    public Report findReportByIdWithUser(Long reportId) {
        return reportRepository.findByIdWithUser(reportId).orElse(null);
    }

    /**
     * Method to fetch report with report container details loaded.
     * This is to help maintain performance by not making the field always FetchType.EAGER instead.
     * @param reportId The report ID of the report to fetch
     * @return The report
     */
    public Report findReportWithEagerReportContainer(Long reportId) {
        return reportRepository.findWithReportContainer(reportId).orElse(null);
    }

    public Report findReportWithEagerReportContainerAndReports(Long reportId) {
        return reportRepository.findWithReportContainerAndReports(reportId).orElse(null);
    }

    public List<Long> findReportContainerIdsOfToReportsByFromReport(Long fromReportId) {
        return duplicateSuggestionRepository.findReportContainerIdsOfToReportsByFromReport(fromReportId);
    }

    public boolean validateReportFromWorkspace(Report report, Workspace workspace) {
        return report.getWorkspace() == workspace;
    }

    /* ---------- Duplicate Suggestion Related ---------- */

    @Transactional
    public void deleteDuplicateSuggestionsByReport(Long reportId) {
        duplicateSuggestionRepository.deleteByFromReportId(reportId);
    }

    public DuplicateSuggestion findDuplicateSuggestionByFromReportAndToReport(Report fromReport, Report toReport) {
        return duplicateSuggestionRepository.findByFromReportAndToReport(fromReport, toReport).orElse(null);
    }

    public boolean validateFromReportAndToReportFromWorkspaceAndFromReportIsOutstanding(Report fromReport, Report toReport, Workspace workspace) {
        return validateReportFromWorkspace(fromReport, workspace) &&
            (toReport == null || validateReportFromWorkspace(toReport, workspace)) &&
            fromReport.getDuplicateDetectionLifecycleState() == DuplicateDetectionLifecycleState.OUTSTANDING;
    }

    public boolean validateSuggestionExistsAndFromReportAndToReportFromWorkspaceAndFromReportIsOutstanding(Report fromReport, Report toReport, Workspace workspace) {
        return findDuplicateSuggestionByFromReportAndToReport(fromReport, toReport) != null &&
            validateFromReportAndToReportFromWorkspaceAndFromReportIsOutstanding(fromReport, toReport, workspace);
    }

    public boolean validateFromReportFromWorkspaceAndIsOutstanding(Report fromReport, Workspace workspace) {
        return fromReport != null && validateReportFromWorkspace(fromReport, workspace);
    }

    /**
     * Method to claim a report to handle resolving duplicate suggestions for.
     * @return true if successfully claimed, else false e.g. if busy right now.
     */
    @Transactional
    public boolean claimReport(Long workspaceId, Long userId, List<Long> excludeReportIds, Long reportContainerId) throws DataIntegrityViolationException {
        Instant currentTime = Instant.now();

        //Sanitise exclusions
        int excludeSize = 0;
        if (excludeReportIds == null || excludeReportIds.isEmpty()) {
            excludeReportIds = List.of(-1L); //Passing -1L to avoid empty list issues in native SQL query.
        } else {
            excludeSize = excludeReportIds.size(); //Will be used by query for checking if the list is empty.
        }

        //Sanitise inclusions
        int includeSize = 0;
        List<Long> includeReportIds = null;
        if (reportContainerId != null) {
            includeReportIds = duplicateSuggestionRepository.findReportsToIncludeInClaimSelection(reportContainerId);
        }
        if (includeReportIds == null || includeReportIds.isEmpty()) {
            includeReportIds = List.of(-1L);
        } else {
            includeSize = includeReportIds.size();
        }

        //Make claim
        return duplicateSuggestionRepository.claimToResolve(
            workspaceId,
            currentTime,
            userId,
            currentTime.minusSeconds(60 * 5), //5 minutes claims expiration
            excludeReportIds,
            includeReportIds,
            excludeSize,
            includeSize
        ) == 1;
    }

    @Transactional
    public void unclaimReport(Long fromReportId, Long userId) {
        duplicateSuggestionRepository.unclaimToResolve(fromReportId, userId);
        log.debug("Unclaimed User ID:{} from Report ID:{} for manually resolving duplicates.", userId, fromReportId);
    }

    public DuplicateSuggestionsToResolveDTO constructDuplicateSuggestionsToResolveDTO(Workspace workspace, User user) {
        //Fetch duplicate suggestions, ensure not null.
        List<DuplicateSuggestion> duplicateSuggestionsToResolve = duplicateSuggestionRepository.findSuggestionsToResolveByClaim(workspace.getId(), user.getId());
        if (duplicateSuggestionsToResolve.isEmpty()) {
            return null;
        }
        //Construct into DTO.
        return DuplicateSuggestionsToResolveDTO.fromEntity(duplicateSuggestionsToResolve);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void rejectIndividualDuplicateSuggestion(Long fromReportId, Long toReportId) {
        duplicateSuggestionRepository.deleteByFromReportIdAndToReportId(fromReportId, toReportId);
        //Update suggestions count for report container of toReport if exists.
        reportRepository.findReportContainerIdByReport(toReportId).ifPresent(reportContainerId ->
            incomingDuplicateSuggestionsCountUpdater.markReportContainerDirty(reportContainerId)
        );
    }

    /**
     * Method to check if a report has any leftover duplicate suggestions.
     * @param fromReportId The report ID of the report to check for.
     * @return true if there are, otherwise false.
     */
    public boolean existsDuplicateSuggestionsFromReport(Long fromReportId) {
        return duplicateSuggestionRepository.existsByFromReportId(fromReportId);
    }

    /* ---------- Assignment Related ---------- */

    /**
     * Method to dictate whether a certain workspace role can edit assignees of a bug at all.
     * @param workspaceRole The workspace role in question.
     * @return true if they can, otherwise false.
     */
    public boolean canRoleEditBugAssignees(WorkspaceRole workspaceRole) {
        return !(workspaceRole == null || workspaceRole == WorkspaceRole.REPORTER);
    }

    /**
     * Method to check if user has already been assigned to a bug
     * @param reportContainer The report container/bug to check in
     * @param workspaceMember The user workspaceMembership object to check for
     * @return true if they have been, otherwise false.
     */
    public boolean isUserBugAssignee(ReportContainer reportContainer, WorkspaceMember workspaceMember) {
        return reportContainerAssigneeRepository.existsByReportContainerIdAndWorkspaceMemberId(reportContainer.getId(), workspaceMember.getId());
    }

    /**
     * Method to add user as bug assignee.
     * @param reportContainer The report container/bug to assign the user in.
     * @param workspaceMember The user workspace membership object to assign.
     */
    @Transactional
    public void assignWorkspaceMemberToBug(ReportContainer reportContainer, WorkspaceMember workspaceMember) {
        ReportContainerAssignee newAssignee = new ReportContainerAssignee(reportContainer, workspaceMember);
        reportContainerAssigneeRepository.save(newAssignee);
        workspaceMemberRepository.updateAssignmentCountForMemberIds(List.of(workspaceMember.getId()));
    }

    /**
     * Method to add users as bug assignee.
     * @param reportContainer The report container/bug to assign the users to.
     * @param workspaceMembers The user workspace membership objects to assign.
     */
    @Transactional
    public void assignWorkspaceMembersToBug(ReportContainer reportContainer, List<WorkspaceMember> workspaceMembers) {
        try {
            List<ReportContainerAssignee> newAssignees = new ArrayList<>();
            for (WorkspaceMember wm : workspaceMembers) {
                newAssignees.add(new ReportContainerAssignee(reportContainer, wm));
            }
            reportContainerAssigneeRepository.saveAll(newAssignees);
            workspaceMemberRepository.updateAssignmentCountForMemberIds(workspaceMembers.stream().map(WorkspaceMember::getId).toList());
        } catch (Exception e) {
            log.error("Failed in assignWorkspaceMembersToBug(ReportContainer reportContainer, List<WorkspaceMember> workspaceMembers)", e);
        }
    }

    @Transactional
    public void unassignWorkspaceMemberFromBug(Long reportContainerId, Long workspaceMemberId) {
        try {
            reportContainerAssigneeRepository.deleteByReportContainerIdAndWorkspaceMemberId(reportContainerId, workspaceMemberId);
        } catch (Exception e) {
            log.error("Failed in unassignWorkspaceMemberFromBug(Long reportContainerId, Long workspaceMemberId)", e);
        }
    }

    @Transactional
    public void unassignWorkspaceMembersFromBug(Long reportContainerId, List<Long> workspaceMemberIds) {
        try {
            reportContainerAssigneeRepository.deleteAllByReportContainerIdAndWorkspaceMemberIds(reportContainerId, workspaceMemberIds);
            workspaceMemberRepository.updateAssignmentCountForMemberIds(workspaceMemberIds);
        } catch (Exception e) {
            log.error("Failed in unassignWorkspaceMembersFromBug(Long reportContainerId, List<Long> workspaceMemberIds)", e);
        }
    }

    @Transactional
    public void updateAssignmentCountForWorkspaceMembersById(List<Long> workspaceMemberIds) {
        workspaceMemberRepository.updateAssignmentCountForMemberIds(workspaceMemberIds);
    }

    public List<User> findAssignedUsers(Long reportContainerId) {
        return reportContainerAssigneeRepository.findUsersByReportContainerId(reportContainerId);
    }

    public List<User> findAssignedUsersExcludingSelf(Long reportContainerId, Long excludingUserId) {
        return reportContainerAssigneeRepository.findUsersByReportContainerIdAndUserIdNot(reportContainerId, excludingUserId);
    }

    /**
     * Method to find a list of users assigned to each report container.
     * @param reportContainerIds List of report container IDs to find the assignees for in each.
     * @return A map of report containers and a list of their assignees each.
     */
    public Map<Long, List<User>> findAssignedUsersPerReportContainerId(List<Long> reportContainerIds) {
        List<ReportContainerUserAssigneeProjection> assignedUsersPerReport = reportContainerAssigneeRepository.findAssignedUsersPerReportContainerId(reportContainerIds);
        Map<Long, List<User>> assignedUsersPerReportMap = new HashMap<>();
        for (ReportContainerUserAssigneeProjection rcuap : assignedUsersPerReport) {
            assignedUsersPerReportMap.computeIfAbsent(rcuap.getReportContainerId(), f -> new ArrayList<>()).add(rcuap.getUser());
        }
        return assignedUsersPerReportMap;
    }

    /**
     * Method to find a list of workspace members assigned to a report container.
     * @param reportContainer The report container to find assignments for.
     * @return A list of WorkspaceMember objects
     */
    public List<WorkspaceMember> findAssignedMembers(ReportContainer reportContainer) {
        return reportContainerAssigneeRepository.findWorkspaceMembersByReportContainerId(reportContainer.getId());
    }

    /**
     * Method to find workspace members that could be assigned to a report container/bug.
     * @param reportContainer The report container to find assignable workspace members for.
     * @param assignees A list of workspace members who have already been assigned.
     * @param userRole The users role in the workspace.
     * @return A list of assignable workspace members.
     */
    public List<WorkspaceMember> findAssignableMembers(ReportContainer reportContainer, List<WorkspaceMember> assignees, WorkspaceRole userRole) {
        return workspaceMemberRepository.findByWorkspaceIdAndNotInAssigneesAndRoleInPermittedRoles(reportContainer.getWorkspaceId(), assignees, workspacePermissionsService.findRolesUserCanEditAssignmentsFor(userRole));
    }

    /**
     * Method to return a list of assignees and assignable workspace members for a bug, depending on the users role in the workspace.
     * @param reportContainer The report container to find details for.
     * @param userRole The users role in the workspace.
     * @return A map containing labelled lists of workspace members. e.g.
     * {@code
     *      "assignees": [...],
     *      "assignable": [...]
     * }
     */
    public Map<String, List<WorkspaceMember>> findAssignedAndAssignableMembers(ReportContainer reportContainer, WorkspaceRole userRole) {
        List<WorkspaceMember> assignedMembers = findAssignedMembers(reportContainer);
        List<WorkspaceMember> assignableMembers = findAssignableMembers(reportContainer, assignedMembers, userRole);
        return Map.of(
            "assigned", assignedMembers,
            "assignable", assignableMembers
        );
    }

    public ReportContainerAssigneeDetailsResponse getReportContainerAssigneeDetailsResponseAsDTO(
        ReportContainer reportContainer,
        WorkspaceRole workspaceRole
    ) {
        //Find details
        Map<String, List<WorkspaceMember>> assignedAndAssignableMembers = findAssignedAndAssignableMembers(reportContainer, workspaceRole);
        //Return details response.
        return ReportContainerAssigneeDetailsResponse.fromData(
            reportContainer.getId(),
            workspaceRole,
            assignedAndAssignableMembers.get("assigned"),
            assignedAndAssignableMembers.get("assignable")
        );
    }

    /**
     * Method to find list of assignees for a bug in a DTO format.
     * @param reportContainer The report container to find assignees for.
     * @return A list of assignees in AssigneeDetailsOverviewDTO format.
     * @see AssigneeDetailsOverviewDTO
     */
    public List<AssigneeDetailsOverviewDTO> findAssigneeDetailsAsDTO(ReportContainer reportContainer) {
        return findAssignedMembers(reportContainer).stream().map(AssigneeDetailsOverviewDTO::fromEntity).toList();
    }

    public ReportContainerAssignee findReportContainerAssignee(Long reportContainerId, Long workspaceMemberId) {
        return reportContainerAssigneeRepository.findByReportContainerIdAndWorkspaceMemberId(reportContainerId, workspaceMemberId).orElse(null);
    }

    public Page<ReportContainerOverviewDTO> findPaginatedReportContainersAssignedToUserAsDTO(
            Long workspaceId,
            Long assigneeUserId,
            List<ReportContainerProgress> progressFilters,
            Pageable pageable
    ) {
        //Check what to fetch and fetch appropriate page. (1 query)
        Page<ReportContainer> bugResults;
        if (workspaceId == null) {
            //If no workspace ID, find bugs page assigned to user.
            bugResults = reportContainerAssigneeRepository.findPaginatedReportContainersByWorkspaceMemberUserId(
                assigneeUserId,
                progressFilters,
                pageable
            );
        } else {
            //If workspace ID provided, fetch bugs assigned to provided user in workspace.
            bugResults = reportContainerAssigneeRepository.findPaginatedReportContainersByWorkspaceMemberUserIdAndWorkspaceId(
                assigneeUserId,
                workspaceId,
                progressFilters,
                pageable
            );
        }
        log.debug("Received assigned bugs results: Total Elements: {}", bugResults.getTotalElements());

        //Compose results into a page with fuller details, and return.
        return composeReportContainerOverviewResultsAsPage(bugResults, workspaceId);
    }

    /* ---------- Bug Details Related Actions ---------- */

    @Transactional
    public ReportContainer updateReportContainerSummary(Long reportContainerId, String newSummary) {
        //Fetching here to prevent detachment issues.
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElseThrow();
        reportContainer.setSummary(newSummary);
        reportContainerRepository.save(reportContainer);
        return reportContainer;
    }

    @Transactional
    public ReportContainer updateWorkspaceCustomLabels(Long reportContainerId, Set<WorkspaceCustomLabel> addLabels, Set<WorkspaceCustomLabel> removeLabels) {
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElseThrow();

        //Get current workspace label mappings
        List<ReportContainerWorkspaceCustomLabelMapping> rcwclmList = reportContainerWorkspaceCustomLabelMappingRepository.findByReportContainerId(reportContainerId);
        Set<Long> reportContainerWCLIds = rcwclmList.stream().map(rcwclm -> rcwclm.getWorkspaceCustomLabel().getId()).collect(Collectors.toSet());

        //Remove labels.
        Set<Long> removeLabelIdsSet = removeLabels.stream().map(WorkspaceCustomLabel::getId).collect(Collectors.toSet()); //O(n)
        for (ReportContainerWorkspaceCustomLabelMapping rcwclm : rcwclmList) { //O(m)
            reportContainer.getWorkspaceCustomLabels().removeIf(mapping -> removeLabelIdsSet.contains(mapping.getWorkspaceCustomLabel().getId()));
        }
        //Filter add labels.
        addLabels.removeIf(label -> reportContainerWCLIds.contains(label.getId()));
        //Add labels.
        for (WorkspaceCustomLabel label : addLabels) { //O(n)
            reportContainer.getWorkspaceCustomLabels().add(new ReportContainerWorkspaceCustomLabelMapping(
                reportContainer,
                label
            ));
        }

        return reportContainerRepository.save(reportContainer);
    }

    @Transactional
    public ReportContainer updateReportContainerStatus(Long reportContainerId, ReportContainerStatus newStatus) {
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElseThrow();
        reportContainer.setStatus(newStatus);
        return reportContainer;
    }

    @Transactional
    public ReportContainer updateReportContainerProgress(Long reportContainerId, ReportContainerProgress newProgress) {
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElseThrow();
        reportContainer.setProgress(newProgress);
        return reportContainer;
    }

    /**
     * Method to check if provided string report container status is valid.
     * @return The actual report container status enum
     */
    public ReportContainerStatus validReportContainerStatus(String newStatus) {
        try {
            return ReportContainerStatus.valueOf(newStatus);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Method to check if provided string report container progress is valid.
     * @return The actual report container progress enum
     */
    public ReportContainerProgress validReportContainerProgress(String newProgress) {
        try {
            return ReportContainerProgress.valueOf(newProgress);
        } catch (Exception e) {
            return null;
        }
    }

}
