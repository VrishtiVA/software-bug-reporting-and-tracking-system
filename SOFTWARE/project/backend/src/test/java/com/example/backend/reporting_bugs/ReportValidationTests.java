package com.example.backend.reporting_bugs;

import com.example.backend.services.reports.ReportService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import static org.junit.jupiter.api.Assertions.*;

@Execution(ExecutionMode.CONCURRENT)
public class ReportValidationTests {

    private final ReportService reportService = new ReportService();

    /* ---------- Testing reportService.isReportSummaryValid(String summary) ---------- */

    @Test
    void testIsReportSummaryValidEmpty() {
        assertFalse(reportService.isReportSummaryValid(""));
        assertFalse(reportService.isReportSummaryValid(" "));
        assertFalse(reportService.isReportSummaryValid("          "));
        assertFalse(reportService.isReportSummaryValid(null));
    }

    @Test
    void testIsReportSummaryValidLength() {
        assertFalse(reportService.isReportSummaryValid("Abcdefghi"));
        assertTrue(reportService.isReportSummaryValid("Abcdefghij"));
        assertTrue(reportService.isReportSummaryValid("Abcdefghij".repeat(5)));
        assertFalse(reportService.isReportSummaryValid("Abcdefghij".repeat(5) + "k"));
    }

    /* ---------- Testing reportService.isLongInputTextFieldValid(String text, boolean optional) ---------- */

    @Test
    void testIsReportLongInputTextValidOptionalEmpty() {
        assertTrue(reportService.isLongInputTextFieldValid("", true));
        assertTrue(reportService.isLongInputTextFieldValid("          ", true));
        assertTrue(reportService.isLongInputTextFieldValid(null, true));
    }

    @Test
    void testIsReportLongInputTextValidRequiredEmpty() {
        assertFalse(reportService.isLongInputTextFieldValid("", false));
        assertFalse(reportService.isLongInputTextFieldValid("          ", false));
        assertFalse(reportService.isLongInputTextFieldValid(null, false));
    }

    @Test
    void testIsReportLongInputTextValidRequiredLength() {
        assertFalse(reportService.isLongInputTextFieldValid("Abcdefghi", false));
        assertTrue(reportService.isLongInputTextFieldValid("Abcdefghij", false));
        assertTrue(reportService.isLongInputTextFieldValid("Abcdefghij".repeat(50), false));
        assertFalse(reportService.isLongInputTextFieldValid("Abcdefghij".repeat(50) + "k", false));
    }

    @Test
    void testIsReportLongInputTextValidOptionalLength() {
        assertTrue(reportService.isLongInputTextFieldValid("Abcdefghi", true));
        assertTrue(reportService.isLongInputTextFieldValid("Abcdefghij", true));
        assertTrue(reportService.isLongInputTextFieldValid("Abcdefghij".repeat(50), true));
        assertFalse(reportService.isLongInputTextFieldValid("Abcdefghij".repeat(50) + "k", true));
    }

    /* ---------- Testing reportService.isRatingInputValid(int rating, boolean zeroIndexed) ---------- */

    @Test
    void testIsRatingInputValidZeroIndexed() {
        assertTrue(reportService.isRatingInputValid(0, true));
        assertTrue(reportService.isRatingInputValid(1, true));
        assertTrue(reportService.isRatingInputValid(4, true));
        assertFalse(reportService.isRatingInputValid(5, true));
    }

    @Test
    void testIsRatingInputValidOneIndexed() {
        assertFalse(reportService.isRatingInputValid(0, false));
        assertTrue(reportService.isRatingInputValid(1, false));
        assertTrue(reportService.isRatingInputValid(4, false));
        assertTrue(reportService.isRatingInputValid(5, false));
    }

    /* ---------- Testing reportService.isAttachmentCaptionValid(String caption) ---------- */

    @Test
    void testIsAttachmentCaptionValidEmpty() {
        assertTrue(reportService.isAttachmentCaptionValid(""));
        assertTrue(reportService.isAttachmentCaptionValid(" "));
        assertTrue(reportService.isAttachmentCaptionValid(null));
    }

    @Test
    void testIsAttachmentCaptionValidLength() {
        assertTrue(reportService.isAttachmentCaptionValid("abcdefghij".repeat(10)));
        assertFalse(reportService.isAttachmentCaptionValid("abcdefghij".repeat(10) + "k"));
    }

}
