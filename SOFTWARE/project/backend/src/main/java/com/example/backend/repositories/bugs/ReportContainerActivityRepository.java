package com.example.backend.repositories.bugs;

import com.example.backend.model.bugs.ReportContainerActivity;
import com.example.backend.model.bugs.ReportContainerActivityType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportContainerActivityRepository extends JpaRepository<ReportContainerActivity, Long> {

    /**
     * Find audit logs in a report container's activity.
     * @param reportContainerId The report container to find audit logs for.
     * @param pageable The pagination configuration.
     * @return The relevant page of audit logs.
     */
    Page<ReportContainerActivity> findByReportContainerIdAndType(
            Long reportContainerId,
            ReportContainerActivityType type,
            Pageable pageable
    );

    /**
     * Method to delete all report containers/bugs in a workspace.
     * @param workspaceId The workspace ID.
     */
    @Modifying
    @Query("""
        DELETE FROM ReportContainerActivity rca
        WHERE rca.reportContainer.workspace.id = :workspace_id
    """)
    void deleteAllByWorkspaceId(@Param("workspace_id") Long workspaceId);

}
