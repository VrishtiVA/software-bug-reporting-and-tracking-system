package com.example.backend.repositories.reports;

import com.example.backend.model.reports.FileType;
import com.example.backend.model.reports.ReportAttachment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ReportAttachmentRepository extends JpaRepository<ReportAttachment, Long> {

    Optional<ReportAttachment> findByReference(String reference);

    /** Method to update report attachment thumbnail for specific report attachment. */
    @Modifying
    @Query("""
        UPDATE ReportAttachment ra
        SET ra.thumbnailReference = :thumbnail_reference
        WHERE ra.id = :report_attachment_id
    """)
    void updateThumbnailReference(
        @Param("report_attachment_id") Long reportAttachmentId,
        @Param("thumbnail_reference") String thumbnailReference
    );

    /**
     * Query to find report attachments of a certain type, belonging to reports that belong to a certain report container.
     * @param reportContainerId The report container to find report attachments under.
     * @param fileType The attachment type to find.
     * @param pageable The pagination configuration.
     * @return A page of report attachments metadata.
     */
    @Query("""
        SELECT ra
        FROM ReportAttachment ra
        WHERE
            ra.type = :file_type AND
            EXISTS ( SELECT 1
                FROM Report r
                JOIN r.reportAttachments a
                WHERE a = ra AND
                    r.reportContainer.id = :report_container_id
            )
    """)
    Page<ReportAttachment> findByReportContainerAndFileTypeAsPage(
        @Param("report_container_id") Long reportContainerId,
        @Param("file_type") FileType fileType,
        Pageable pageable
    );

    /**
     * Method to delete all attachments under a workspace.
     * @param workspaceId The ID of the workspace.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
        DELETE ra FROM report_attachment ra
        JOIN report r ON r.id = ra.report_id
        JOIN workspace w ON w.id = r.workspace_id
        WHERE w.id = :workspace_id
    """, nativeQuery = true)
    void deleteAllByWorkspaceId(@Param("workspace_id") Long workspaceId);
}
