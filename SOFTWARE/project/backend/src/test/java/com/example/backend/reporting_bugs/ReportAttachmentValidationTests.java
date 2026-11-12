package com.example.backend.reporting_bugs;

import com.example.backend.reporting_bugs.samples.SampleDummyAttachments;
import com.example.backend.services.reports.ReportAttachmentService;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Execution(ExecutionMode.CONCURRENT)
public class ReportAttachmentValidationTests {

    @TempDir
    static Path temp_uploads; //Use for all tests in the test class.

    private static ReportAttachmentService reportAttachmentService;

    @BeforeAll
    public static void setUp() {
        reportAttachmentService = new ReportAttachmentService(String.valueOf(temp_uploads));
    }

    /* ---------- Testing reportAttachmentService.isAttachmentValid(MultipartFile attachment) ---------- */

    @Test
    void testIsAttachmentValidImageType() {
        assertTrue(reportAttachmentService.isAttachmentValid(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_JPG));
        assertTrue(reportAttachmentService.isAttachmentValid(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_PNG));
    }

    @Test
    void testIsAttachmentValidVideoType() {
        assertTrue(reportAttachmentService.isAttachmentValid(SampleDummyAttachments.SAMPLE_ATT_VALID_VIDEO_MP4));
        assertTrue(reportAttachmentService.isAttachmentValid(SampleDummyAttachments.SAMPLE_ATT_VALID_VIDEO_WEBM));
    }

    @Test
    void testIsAttachmentInvalidType() {
        assertFalse(reportAttachmentService.isAttachmentValid(SampleDummyAttachments.SAMPLE_ATT_INVALID_TYPE));
    }

    @Test
    void testIsAttachmentInvalidImageSize() {
        assertFalse(reportAttachmentService.isAttachmentValid(SampleDummyAttachments.SAMPLE_ATT_INVALID_IMAGE_SIZE));
        assertTrue(reportAttachmentService.isAttachmentValid(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_PNG));
    }

    @Test
    void testIsAttachmentInvalidVideoSize() {
        assertFalse(reportAttachmentService.isAttachmentValid(SampleDummyAttachments.SAMPLE_ATT_INVALID_VIDEO_SIZE));
        assertTrue(reportAttachmentService.isAttachmentValid(SampleDummyAttachments.SAMPLE_ATT_VALID_VIDEO_MP4));
    }

}
