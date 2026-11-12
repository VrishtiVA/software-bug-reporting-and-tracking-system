package com.example.backend.services.bugs;

import com.example.backend.dto.responses.bugs.BugAuditLogDTO;
import com.example.backend.model.bugs.*;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.repositories.bugs.ReportContainerActivityRepository;
import com.example.backend.repositories.bugs.ReportContainerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
public class ReportContainerActivityService {

    private static final Logger log = LoggerFactory.getLogger(ReportContainerActivityService.class);

    @Autowired
    private ReportContainerRepository reportContainerRepository;
    @Autowired
    private ReportContainerActivityRepository reportContainerActivityRepository;

    @Transactional
    public void logReportContainerCreation(
        Long reportContainerId,
        Long firstReportId
    ) {
        //Find report container
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElse(null);
        if (reportContainer == null) {
            log.warn("No report container with ID:{} found in logReportContainerCreation(...).", reportContainerId);
            return;
        }
        //Create audit log
        ReportContainerActivity activity = new ReportContainerActivity(
                reportContainer,
                new AuditMessageBuilder().text("This bug was first discovered through report").id(firstReportId).build(),
                ReportContainerActivityType.LOG,
                ReportContainerActivityLogType.CREATION,
                null, null, null
        );
        reportContainer.getReportContainerActivity().add(activity);
        reportContainerRepository.save(reportContainer);
    }

    /**
     * Method to log report container modification.
     * This is for field "summary" or "labels".
     * All fields should be provided for "summary" variant of this method, while for "labels", old and new aren't required.
     */
    @Transactional
    public void logReportContainerModification(
        String field,
        Long reportContainerId,
        User performer,
        WorkspaceRole performerRole,
        String oldValue,
        String newValue
    ) {
        //Find report container
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElse(null);
        if (reportContainer == null) {
            log.warn("No report container with ID:{} found in logReportContainerModification(...).", reportContainerId);
            return;
        }
        //Create audit log
        ReportContainerActivity activity;
        if (field.equals("summary")) {
            activity = new ReportContainerActivity(
                reportContainer,
                new AuditMessageBuilder()
                    .username(performer.getUsername())
                    .text("updated bug summary from")
                    .oldValue(oldValue)
                    .text("to")
                    .newValue(newValue)
                    .build(),
                ReportContainerActivityType.LOG,
                ReportContainerActivityLogType.MODIFICATION,
                performer, performerRole, "SUMMARY"
            );
            reportContainer.setLastUpdatedTimestamp(Instant.now());
        } else if (field.equals("labels")) {
            activity = new ReportContainerActivity(
                reportContainer,
                new AuditMessageBuilder()
                    .username(performer.getUsername())
                    .text("updated bug labels")
                    .build(),
                ReportContainerActivityType.LOG,
                ReportContainerActivityLogType.MODIFICATION,
                performer, performerRole, "LABELS"
            );
            reportContainer.setLastUpdatedTimestamp(Instant.now());
        } else {
            log.warn("No field to log provided in logReportContainerModification(...).");
            return;
        }
        reportContainer.getReportContainerActivity().add(activity);
        reportContainerRepository.save(reportContainer);
    }

    /**
     * Method to log report container progression.
     * This could be for field "status" or "progress".
     */
    @Transactional
    public void logReportContainerProgression(
        String field,
        Long reportContainerId,
        User performer,
        WorkspaceRole performerRole,
        String oldValue,
        String newValue
    ) {
        //Find report container
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElse(null);
        if (reportContainer == null) {
            log.warn("No report container with ID:{} found in logReportContainerProgression(...).", reportContainerId);
            return;
        }
        //Create audit log
        ReportContainerActivity activity;
        if (field.equals("status")) {
            activity = new ReportContainerActivity(
                reportContainer,
                new AuditMessageBuilder()
                    .username(performer.getUsername())
                    .text("updated bug status from")
                    .oldValue(oldValue)
                    .text("to")
                    .newValue(newValue)
                    .build(),
                ReportContainerActivityType.LOG,
                ReportContainerActivityLogType.PROGRESSION,
                performer, performerRole, "STATUS"
            );
            reportContainer.setLastUpdatedTimestamp(Instant.now());
        } else if (field.equals("progress")) {
            activity = new ReportContainerActivity(
                reportContainer,
                new AuditMessageBuilder()
                    .username(performer.getUsername())
                    .text("updated bug progress from")
                    .oldValue(oldValue)
                    .text("to")
                    .newValue(newValue)
                    .build(),
                ReportContainerActivityType.LOG,
                ReportContainerActivityLogType.PROGRESSION,
                performer, performerRole, "PROGRESS"
            );
            reportContainer.setLastUpdatedTimestamp(Instant.now());
        } else {
            log.warn("No field to log provided in logReportContainerProgression(...).");
            return;
        }
        reportContainer.getReportContainerActivity().add(activity);
        reportContainerRepository.save(reportContainer);
    }

    @Transactional
    public void logReportContainerAssignmentChanges(
        boolean quickSelfAssign,
        Long reportContainerId,
        User performer,
        WorkspaceRole performerRole
    ) {
        //Find report container
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElse(null);
        if (reportContainer == null) {
            log.warn("No report container with ID:{} found in logReportContainerAssignmentChanges(...).", reportContainerId);
            return;
        }
        //Create audit log
        ReportContainerActivity activity = new ReportContainerActivity(
            reportContainer,
            new AuditMessageBuilder()
                .username(performer.getUsername())
                .text(quickSelfAssign ? "assigned themselves to this bug" : "updated bug assignees")
                .build(),
            ReportContainerActivityType.LOG,
            ReportContainerActivityLogType.ASSIGNMENT,
            performer, performerRole, "ASSIGNEES"
        );
        //Update report container.
        reportContainer.setLastUpdatedTimestamp(Instant.now());
        reportContainer.getReportContainerActivity().add(activity);
        reportContainerRepository.save(reportContainer);
    }

    @Transactional
    public void logReportContainerDuplicateResolved(
        Long reportContainerId,
        User performer,
        WorkspaceRole performerRole,
        Long reportId,
        Integer reportsAddedCount
    ) {
        //Find report container
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElse(null);
        if (reportContainer == null) {
            log.warn("No report container with ID:{} found in logReportContainerDuplicateResolved(...).", reportContainerId);
            return;
        }
        //Add audit log
        ReportContainerActivity activity = new ReportContainerActivity(
            reportContainer,
            new AuditMessageBuilder()
                .username(performer.getUsername())
                .text("resolved report")
                .id(reportId)
                .text("as a duplicate report for this bug" + (reportsAddedCount > 1 ? ", merging in " + reportsAddedCount + " new reports" : ""))
                .build(),
            ReportContainerActivityType.LOG,
            ReportContainerActivityLogType.DUPLICATE_RESOLVED,
            performer, performerRole, null
        );
        //Update report container.
        reportContainer.setLastUpdatedTimestamp(Instant.now());
        reportContainer.getReportContainerActivity().add(activity);
        reportContainerRepository.save(reportContainer);
    }

    @Transactional
    public void logReportContainerDuplicateAutoResolved(
        Long reportContainerId,
        Long reportId,
        Integer reportsAddedCount
    ) {
        //Find report container
        ReportContainer reportContainer = reportContainerRepository.findById(reportContainerId).orElse(null);
        if (reportContainer == null) {
            log.warn("No report container with ID:{} found in logReportContainerDuplicateAutoResolved(...).", reportContainerId);
            return;
        }
        //Add audit log
        ReportContainerActivity activity = new ReportContainerActivity(
            reportContainer,
            new AuditMessageBuilder()
                .text("Report")
                .id(reportId)
                .text("was automatically resolved as a duplicate report for this bug" + (reportsAddedCount > 1 ? ", merging in "+reportsAddedCount+" new reports" : ""))
                .build(),
            ReportContainerActivityType.LOG,
            ReportContainerActivityLogType.DUPLICATE_AUTO_RESOLVED,
            null, null, null
        );
        //Update report container.
        reportContainer.setLastUpdatedTimestamp(Instant.now());
        reportContainer.getReportContainerActivity().add(activity);
        reportContainerRepository.save(reportContainer);
    }

    /**
     * Method to find paginated audit logs.
     * @param reportContainer The report container to find audit logs for.
     * @param pageable The pagination configuration.
     * @return The page of audit logs formatted as BugAuditLogDTO.
     * @see BugAuditLogDTO
     */
    public Page<BugAuditLogDTO> findPaginatedAuditLogsData(ReportContainer reportContainer, Pageable pageable) {
        return reportContainerActivityRepository.findByReportContainerIdAndType(
                reportContainer.getId(),
                ReportContainerActivityType.LOG,
                pageable
        ).map(BugAuditLogDTO::fromEntity);
    }

}
