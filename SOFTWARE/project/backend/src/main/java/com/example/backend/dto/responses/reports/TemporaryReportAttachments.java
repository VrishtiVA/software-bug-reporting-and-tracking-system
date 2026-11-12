package com.example.backend.dto.responses.reports;

import com.example.backend.model.reports.ReportAttachment;

import java.nio.file.Path;
import java.util.Set;

public record TemporaryReportAttachments(
    Path temporaryLocation,
    Set<ReportAttachment> reportAttachments
) {}
