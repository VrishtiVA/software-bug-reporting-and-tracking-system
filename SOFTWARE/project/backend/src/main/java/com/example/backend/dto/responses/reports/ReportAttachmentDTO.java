package com.example.backend.dto.responses.reports;

import com.example.backend.model.reports.FileType;
import com.example.backend.model.reports.ReportAttachment;

public record ReportAttachmentDTO(
        Long id,
        String reference,
        FileType type,
        Long size,
        String caption
) {
    public static ReportAttachmentDTO fromEntity(ReportAttachment reportAttachment) {
        return new ReportAttachmentDTO(
            reportAttachment.getId(),
            reportAttachment.getReference(),
            reportAttachment.getType(),
            reportAttachment.getSize(),
            reportAttachment.getCaption()
        );
    }
}
