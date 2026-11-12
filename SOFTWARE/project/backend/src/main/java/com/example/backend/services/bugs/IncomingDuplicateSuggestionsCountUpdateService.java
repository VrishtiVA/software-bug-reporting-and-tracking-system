package com.example.backend.services.bugs;

import com.example.backend.repositories.bugs.ReportContainerRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class IncomingDuplicateSuggestionsCountUpdateService {

    @Autowired
    private ReportContainerRepository reportContainerRepository;
    @Autowired
    private ReportContainerSearchService reportContainerSearchService;

    private static final Logger log = LoggerFactory.getLogger(IncomingDuplicateSuggestionsCountUpdateService.class);

    /** Async-safe set to hold report containers that need updating for incoming duplicate suggestions count only. */
    private final AtomicReference<Set<Long>> dirtyReportContainers = new AtomicReference<>(ConcurrentHashMap.newKeySet());

    /** Method for marking a single report container as dirty for incoming duplicate suggestion count update. */
    public void markReportContainerDirty(Long reportContainerId) {
        dirtyReportContainers.get().add(reportContainerId);
    }
    /** Method for marking multiple report containers as dirty for incoming duplicate suggestion count update. */
    public void markReportContainersDirty(List<Long> reportContainerIds) {
        dirtyReportContainers.get().addAll(reportContainerIds);
    }

    /**
     * Update any dirty report containers on schedule.
     * Eventually consistent incoming duplicate suggestions count.
     */
    @Scheduled(fixedDelay = 7500)
    @Transactional
    public void updateReportContainerIncomingDuplicateSuggestionsCount() {
        if (dirtyReportContainers.get().isEmpty()) {
            log.trace("No dirty report containers here.");
            return;
        }
        try {
            //Get dirty report containers to update, and atomically reset list. This helps avoid race conditions (e.g. rather than cleaning list later).
            Set<Long> forReportContainerIds = dirtyReportContainers.getAndSet(ConcurrentHashMap.newKeySet());
            log.debug("Report containers {} marked as dirty to update incoming duplicate suggestions count for.", forReportContainerIds.toString());
            List<Long> forReportContainerIdsList = new ArrayList<>(forReportContainerIds);
            //Update counts
            reportContainerRepository.bulkUpdateReportContainerIncomingDuplicateSuggestionsCount(forReportContainerIdsList);
            //Update search index
            reportContainerSearchService.updateSearchIndexForReportContainers(forReportContainerIdsList);
            log.debug("Updated lucene search index after bulk dirty incoming duplicate suggestions count update");
        } catch (Exception e) {
            log.error("Failed in scheduled updateReportContainerIncomingDuplicateSuggestionsCount, saved for next time.", e);
        }
    }
}
