package com.example.backend.tracking_bugs;

import com.example.backend.model.bugs.ReportContainerProgress;
import com.example.backend.model.bugs.ReportContainerStatus;
import com.example.backend.services.bugs.ReportContainerService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
public class BugTrackingDetailsValidationTests {

    private final ReportContainerService reportContainerService = new ReportContainerService();

    /* ---------- Testing reportContainerService.validReportContainerStatus(String newStatus) ---------- */

    @Test
    void testIsValidReportContainerStatusEmpty() {
        assertNull(reportContainerService.validReportContainerStatus(""));
        assertNull(reportContainerService.validReportContainerStatus(" "));
        assertNull(reportContainerService.validReportContainerStatus(null));
    }

    @Test
    void testIsReportContainerStatusValid() {
        assertEquals(ReportContainerStatus.UNCONFIRMED, reportContainerService.validReportContainerStatus("UNCONFIRMED"));
        assertEquals(ReportContainerStatus.CONFIRMED, reportContainerService.validReportContainerStatus("CONFIRMED"));
        assertEquals(ReportContainerStatus.UNREPRODUCIBLE, reportContainerService.validReportContainerStatus("UNREPRODUCIBLE"));
        assertEquals(ReportContainerStatus.UNFIXABLE, reportContainerService.validReportContainerStatus("UNFIXABLE"));
        assertEquals(ReportContainerStatus.RESOLVED, reportContainerService.validReportContainerStatus("RESOLVED"));
    }

    @Test
    void testIsReportContainerStatusInvalid() {
        assertNull(reportContainerService.validReportContainerStatus("unconfirmed"));
        assertNull(reportContainerService.validReportContainerStatus("confirmed"));
        assertNull(reportContainerService.validReportContainerStatus("unreproducible"));
        assertNull(reportContainerService.validReportContainerStatus("resolved"));
        assertNull(reportContainerService.validReportContainerStatus("resolve"));
    }

    /* ---------- Testing reportContainerService.validReportContainerProgress(String newProgress) ---------- */

    @Test
    void testIsValidReportContainerProgressEmpty() {
        assertNull(reportContainerService.validReportContainerProgress(""));
        assertNull(reportContainerService.validReportContainerProgress(" "));
        assertNull(reportContainerService.validReportContainerProgress(null));
    }

    @Test
    void testIsReportContainerProgressValid() {
        assertEquals(ReportContainerProgress.OPEN, reportContainerService.validReportContainerProgress("OPEN"));
        assertEquals(ReportContainerProgress.IN_PROGRESS, reportContainerService.validReportContainerProgress("IN_PROGRESS"));
        assertEquals(ReportContainerProgress.IN_TESTING, reportContainerService.validReportContainerProgress("IN_TESTING"));
        assertEquals(ReportContainerProgress.IN_REVIEW, reportContainerService.validReportContainerProgress("IN_REVIEW"));
        assertEquals(ReportContainerProgress.CLOSED, reportContainerService.validReportContainerProgress("CLOSED"));
    }

    @Test
    void testIsReportContainerProgressInvalid() {
        assertNull(reportContainerService.validReportContainerProgress("open"));
        assertNull(reportContainerService.validReportContainerProgress("in_progress"));
        assertNull(reportContainerService.validReportContainerProgress("in_testing"));
        assertNull(reportContainerService.validReportContainerProgress("in_review"));
        assertNull(reportContainerService.validReportContainerProgress("closed"));
        assertNull(reportContainerService.validReportContainerProgress("close"));
        assertNull(reportContainerService.validReportContainerProgress("in progress"));
        assertNull(reportContainerService.validReportContainerProgress("IN PROGRESS"));
    }

}
