package com.example.backend.reporting_bugs.samples;

import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;

public class SampleReportData {

    public static final MockMultipartFile SAMPLE_REPORT_DATA_VALID_NO_CAPTIONS = new MockMultipartFile(
        "reportData", //part name,
        "reportData.json", //file name
        MediaType.APPLICATION_JSON_VALUE, //content type
        ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": []
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_VALID_1_CAPTION = new MockMultipartFile(
        "reportData", //part name,
        "reportData.json", //file name
        MediaType.APPLICATION_JSON_VALUE, //content type
        ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": ["This is an image showing unable to get the resource."]
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_VALID_2_CAPTIONS = new MockMultipartFile(
            "reportData", //part name,
            "reportData.json", //file name
            MediaType.APPLICATION_JSON_VALUE, //content type
            ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": ["This is an image showing unable to get the resource.", ""]
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_VALID_3_CAPTIONS = new MockMultipartFile(
        "reportData", //part name,
        "reportData.json", //file name
        MediaType.APPLICATION_JSON_VALUE, //content type
        ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": ["This is an image showing unable to get the resource.", "", ""]
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_VALID_4_CAPTIONS = new MockMultipartFile(
        "reportData", //part name,
        "reportData.json", //file name
        MediaType.APPLICATION_JSON_VALUE, //content type
        ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": ["This is an image showing unable to get the resource.", "", "", ""]
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_VALID_WITH_LABELS = new MockMultipartFile(
        "reportData", //part name,
        "reportData.json", //file name
        MediaType.APPLICATION_JSON_VALUE, //content type
        ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": ["happy", "organic"],
            "captions": []
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_WITH_UNKNOWN_LABEL = new MockMultipartFile(
        "reportData", //part name,
        "reportData.json", //file name
        MediaType.APPLICATION_JSON_VALUE, //content type
        ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": ["unknown label", "happy"],
            "captions": []
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_INVALID_SUMMARY = new MockMultipartFile(
        "reportData", //part name,
        "reportData.json", //file name
        MediaType.APPLICATION_JSON_VALUE, //content type
        ("""
        {
            "summary": "Live lock",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": []
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_INVALID_STEPS = new MockMultipartFile(
            "reportData", //part name,
            "reportData.json", //file name
            MediaType.APPLICATION_JSON_VALUE, //content type
            ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "steps",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": []
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_INVALID_ENVIRONMENT = new MockMultipartFile(
            "reportData", //part name,
            "reportData.json", //file name
            MediaType.APPLICATION_JSON_VALUE, //content type
            ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "resource",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": []
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_INVALID_EXPECTED = new MockMultipartFile(
            "reportData", //part name,
            "reportData.json", //file name
            MediaType.APPLICATION_JSON_VALUE, //content type
            ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "give",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": []
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_INVALID_ACTUAL = new MockMultipartFile(
            "reportData", //part name,
            "reportData.json", //file name
            MediaType.APPLICATION_JSON_VALUE, //content type
            ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "Live lock",
            "user_impact": 4,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": []
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_INVALID_USER_IMPACT = new MockMultipartFile(
            "reportData", //part name,
            "reportData.json", //file name
            MediaType.APPLICATION_JSON_VALUE, //content type
            ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 5,
            "severity": 0,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": []
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );

    public static final MockMultipartFile SAMPLE_REPORT_DATA_INVALID_SEVERITY = new MockMultipartFile(
            "reportData", //part name,
            "reportData.json", //file name
            MediaType.APPLICATION_JSON_VALUE, //content type
            ("""
        {
            "summary": "Live locks when releasing deadlocks",
            "steps": "I took the key, but couldn't gain the other key, so I released the key, but the other also did this, and then everything happened all over again.",
            "environment": "This happened in a file where resources had to be requested, but the resources was already in use.",
            "expected": "I expected to be able to get the key after releasing all the locks and retrying.",
            "actual": "I couldn't get the key, it was released and taken up by the other client too.",
            "user_impact": 4,
            "severity": 5,
            "fix_suggestions": "Get some sleep",
            "additional_comments": "-Sigh-",
            "labels": [],
            "captions": []
        }
        """).getBytes(StandardCharsets.UTF_8) //file content in bytes
    );


}
