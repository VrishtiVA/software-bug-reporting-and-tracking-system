package com.example.backend.repositories.workspaces;

import com.example.backend.model.reports.ReportWorkspaceCustomLabelMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ReportWorkspaceCustomLabelMappingRepository extends JpaRepository<ReportWorkspaceCustomLabelMapping, Long> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    void deleteAllByWorkspaceCustomLabelId(Long workspaceCustomLabelId);

    @Modifying
    @Query("""
        DELETE FROM ReportWorkspaceCustomLabelMapping rwclm
        WHERE rwclm.report.workspace.id = :workspace_id
    """)
    void deleteAllByWorkspaceId(@Param("workspace_id") Long workspaceId);

}
