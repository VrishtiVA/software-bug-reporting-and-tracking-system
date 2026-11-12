package com.example.backend.repositories.workspaces;

import com.example.backend.model.bugs.ReportContainerWorkspaceCustomLabelMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportContainerWorkspaceCustomLabelMappingRepository extends JpaRepository<ReportContainerWorkspaceCustomLabelMapping, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteAllByWorkspaceCustomLabelId(Long workspaceCustomLabelId);

    List<ReportContainerWorkspaceCustomLabelMapping> findByReportContainerId(Long reportContainerId);

    @Modifying
    @Query("""
        DELETE FROM ReportContainerWorkspaceCustomLabelMapping rcwclm
        WHERE rcwclm.reportContainer.workspace.id = :workspace_id
    """)
    void deleteAllByWorkspaceId(@Param("workspace_id") Long workspaceId);
}
