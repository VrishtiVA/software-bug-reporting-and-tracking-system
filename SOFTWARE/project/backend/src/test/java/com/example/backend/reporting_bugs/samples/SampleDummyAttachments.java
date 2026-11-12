package com.example.backend.reporting_bugs.samples;

import org.springframework.mock.web.MockMultipartFile;

public class SampleDummyAttachments {

    public static final MockMultipartFile SAMPLE_ATT_VALID_IMAGE_JPG = new MockMultipartFile(
            "attachments",
            "test.jpg",
            "image/jpeg", //mime type
            new byte[1024 * 5]
    );

    public static final MockMultipartFile SAMPLE_ATT_VALID_IMAGE_PNG = new MockMultipartFile(
            "attachments",
            "test.png",
            "image/png",
            new byte[1024 * 5]
    );

    public static final MockMultipartFile SAMPLE_ATT_VALID_VIDEO_MP4 = new MockMultipartFile(
            "attachments",
            "test.mp4",
            "video/mp4",
            new byte[1024 * 1024 * 5]
    );

    public static final MockMultipartFile SAMPLE_ATT_VALID_VIDEO_WEBM = new MockMultipartFile(
            "attachments",
            "test.webm",
            "video/webm",
            new byte[1024 * 1024 * 5]
    );

    public static final MockMultipartFile SAMPLE_ATT_INVALID_IMAGE_SIZE = new MockMultipartFile(
            "attachments",
            "test.png",
            "image/png",
            new byte[1024 * 1024 * 10 + 1]
    );

    public static final MockMultipartFile SAMPLE_ATT_INVALID_VIDEO_SIZE = new MockMultipartFile(
            "attachments",
            "test.mp4",
            "video/mp4",
            new byte[1024 * 1024 * 50 + 1]
    );

    public static final MockMultipartFile SAMPLE_ATT_INVALID_TYPE = new MockMultipartFile(
        "attachments",
        "test.pdf",
        "application/pdf",
        new byte[1024 * 5]
    );

}
