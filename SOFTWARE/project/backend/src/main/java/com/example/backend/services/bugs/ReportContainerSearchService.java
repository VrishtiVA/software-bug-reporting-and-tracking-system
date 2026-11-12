package com.example.backend.services.bugs;

import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.bugs.ReportContainerStability;
import com.example.backend.model.bugs.ReportContainerStatus;
import com.example.backend.repositories.bugs.ReportContainerRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.mapper.orm.Search;
import org.hibernate.search.mapper.orm.massindexing.MassIndexer;
import org.hibernate.search.mapper.orm.session.SearchSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * This service was influenced by documentation from <a href="https://hibernate.org/search/">Hibernate Search w/Apache Lucene</a>
 * for working with Hibernate Search w/Apache Lucene.
 */
@Service
public class ReportContainerSearchService {

    @PersistenceContext
    private EntityManager entityManager;

    private static final Logger log = LoggerFactory.getLogger(ReportContainerSearchService.class);

    @Autowired
    private ReportContainerRepository reportContainerRepository;

    /**
     * Method to initialise the lucene search index.
     * The indexer will read all existing database rows and index the embedded bug reports.
     * This method should be called when the application starts running,
     * after which hibernate search will maintain the index.
     */
    @Transactional
    public void buildSearchIndex() throws InterruptedException {
        SearchSession searchSession = Search.session(entityManager);
        MassIndexer indexer = searchSession.massIndexer(ReportContainer.class);
        indexer.startAndWait();
    }

    /**
     * This method is used to update the search index for a specific report container
     * (in this way it is optimised as it doesn't have to update the whole index unnecessarily).
     * This sometimes has to be called manually to update the search index, since in cases
     * where native query is used to bulk move reports into containers or update statuses, hibernate can't
     * see these to update the index itself.
     * @param reportContainerId The ID of the report container to reindex for.
     */
    @Transactional
    public void updateSearchIndexForReportContainer(Long reportContainerId) {
        try {
            //Find report container
            ReportContainer reportContainer = reportContainerRepository.findByIdWithReports(reportContainerId).orElse(null);
            if (reportContainer == null) {
                log.warn("No report container with ID:{} was found in updateSearchIndexForReportContainer.", reportContainerId);
            }
            //Update search index for report container
            SearchSession searchSession = Search.session(entityManager);
            searchSession.indexingPlan().addOrUpdate(reportContainer);
        } catch (Exception e) {
            log.error("Failed in updateSearchIndexForReportContainer", e);
        }
    }

    /**
     * Update the search index for multiple reports containers at once.
     * More efficient when needing to update multiple as uses a single bulk query for fetching.
     * @param reportContainerIds A list of report container IDs to update the search index for.
     */
    @Transactional
    public void updateSearchIndexForReportContainers(List<Long> reportContainerIds) {
        try {
            //Find all report containers to update for.
            List<ReportContainer> reportContainers = reportContainerRepository.findReportContainersByIdWithReports(reportContainerIds);
            log.debug("Updating search index for {} report containers.", reportContainers.size());
            if (!reportContainers.isEmpty()) {
                //Update search index for report containers.
                SearchSession searchSession = Search.session(entityManager);
                for (ReportContainer reportContainer : reportContainers) {
                    if (reportContainer != null) searchSession.indexingPlan().addOrUpdate(reportContainer);
                }
            }
        } catch (Exception e) {
            log.error("Failed in updateSearchIndexForReportContainers", e);
        }
    }

    /**
     * Implementation of full-text hibernate search for bugs in a workspace using local Apache Lucene index.
     * Useful documentation resource: <a href="https://hibernate.org/search/">https://hibernate.org/search/</a>.
     * @param query The search query
     * @param workspaceId The workspace ID to search for bugs in
     * @param pageable The pagination configuration
     * @return The page of report containers/bug results
     */
    public Page<ReportContainer> findPaginatedStableBugResultsByWorkspaceId(
            String query,
            Long workspaceId,
            Pageable pageable,
            List<ReportContainerProgress> includeProgress,
            List<ReportContainerStatus> includeStatus,
            List<String> includeLabels
    ) {
        SearchSession searchSession = Search.session(entityManager);
        SearchResult<ReportContainer> result = searchSession.search(ReportContainer.class)

            //f here represents SearchPredicateFactory, and boolean query is used to combine multiple search conditions.
            .where(f -> {
                var bool = f.bool();

                //Filter workspaces
                //This is done first, to optimise and narrow down the search space early,
                //supporting a multi-tenant/workspace system.
                //Also for security, don't want to see bugs from different workspaces.
                bool.filter(
                    f.match()
                        .field("workspaceId")
                        .matching(workspaceId)
                );
                //Filter stability
                bool.filter(
                        f.match()
                                .field("stability")
                                .matching(ReportContainerStability.STABLE)
                );

                //Fuzzy text search -> allows misspells of up to 1 character for better UX.
                if (query != null && !query.isBlank()) bool.must(
                    f.match()
                        .fields(
                            "summary",
                            "reports.summaryTitle",
                            "reports.stepsToReproduce",
                            "reports.environment",
                            "reports.expectedResult",
                            "reports.actualResult",
                            "reports.fixSuggestions",
                            "reports.additionalComments"
                        )
                        .matching(query)
                        .fuzzy(1)
                );

                //Apply provided filters for progress.
                if (includeProgress != null && !includeProgress.isEmpty()) {
                    bool.filter(
                        f.terms().field("progress").matchingAny(includeProgress)
                    );
                }
                //Apply provided filters for status.
                if (includeStatus != null && !includeStatus.isEmpty()) {
                    bool.filter(
                        f.terms().field("status").matchingAny(includeStatus)
                    );
                }
                //Apply provided filters for labels.
                if (includeLabels != null && !includeLabels.isEmpty()) {
                    bool.filter(
                        f.terms().field("labelNames").matchingAny(includeLabels)
                    );
                }

                return bool;
            })

            //Sorting results depending on sort configuration provided.
            .sort(f -> {
                try {
                    if (pageable.getSort().isSorted()) {
                        Sort.Order sortOrder = pageable.getSort().iterator().next();
                        if (sortOrder.isAscending()) {
                            return f.field(sortOrder.getProperty()).asc();
                        } else {
                            return f.field(sortOrder.getProperty()).desc();
                        }
                    }
                    return f.score();
                } catch (Exception e) {
                    log.error("Failed to sort bug results by {}", pageable.getSort(), e);
                    return f.score();
                }
            })

            //Fetching results with .fetch(offset, limit)
            .fetch((int) pageable.getOffset(), pageable.getPageSize());

        //Hits = entities returned on this page.
        List<ReportContainer> content = result.hits();
        //Total hit count = total number of results.
        long totalElements = result.total().hitCount();

        //Page needs content, pageable, and total results.
        return new PageImpl<>(content, pageable, totalElements);
    }
}