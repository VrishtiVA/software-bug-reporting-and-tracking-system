package com.example.backend.reporting_bugs;

import com.example.backend.TestSampleInitialiserService;
import com.example.backend.reporting_bugs.samples.SampleReportData;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspaceService;
import com.example.backend.reporting_bugs.samples.SampleDummyAttachments;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "h2"}) //Use both application-test and application-test-h2.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CreateReportEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private TestSampleInitialiserService sampleInitialiserService;

    @BeforeAll
    void setUpFirst() {
        //Initialise sample users
        if (userService.isUserRepositoryEmpty()) {
            sampleInitialiserService.initialiseSampleUser();
            sampleInitialiserService.initialiseDemoUsers();
        }
        //Initialise sample workspace.
        if (workspaceService.isWorkspaceRepositoryEmpty()) {
            sampleInitialiserService.initialiseSampleWorkspace();
            sampleInitialiserService.initialisePrivateSampleWorkspace();
        }
    }

    /* ---------- Integration Testing GET /api/workspaces/{id}/bugs/report Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetBugReportFormDetails_PublicWorkspaceAndMemberOkay() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/report"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.user_role").exists())
            .andExpect(jsonPath("$.custom_labels").exists());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testGetBugReportFormDetails_PublicWorkspaceAndNonMemberOkay() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/report"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.user_role").isEmpty())
            .andExpect(jsonPath("$.custom_labels").exists());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testGetBugReportFormDetails_PrivateWorkspaceAndMemberOkay() throws Exception {
        mockMvc.perform(get("/api/workspaces/2/bugs/report"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.user_role").exists())
            .andExpect(jsonPath("$.custom_labels").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugReportFormDetails_PrivateWorkspaceAndNonMemberForbidden() throws Exception {
        mockMvc.perform(get("/api/workspaces/2/bugs/report"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugReportFormDetails_WorkspaceNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/25/bugs/report"))
            .andExpect(status().isNotFound());
    }

    /* ---------- Integration Testing POST /api/workspaces/{id}/bugs/report Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_PublicWorkspaceAndMemberOkay_NoLabelsAndNoAttachmentsCreated() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report") //this is post by default
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_NO_CAPTIONS)
        )
        .andExpect(status().isCreated());

    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_WithLabelsCreated() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_WITH_LABELS)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_WithAttachmentAndCaptionCreated() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_1_CAPTION)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_PNG)
        )
        .andExpect(status().isCreated());
    }


    @Test
    @WithUserDetails("demonstrator2")
    void testPostBugReport_PrivateWorkspaceAndMemberCreated() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/2/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_NO_CAPTIONS)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testPostBugReport_PublicWorkspaceAndNonMemberOkay() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_NO_CAPTIONS)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_PrivateWorkspaceAndNonMemberForbidden() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/2/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_NO_CAPTIONS)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_WorkspaceNotFound() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/25/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_NO_CAPTIONS)
        )
        .andExpect(status().isNotFound());
    }

    /* ----- Attachment Validity ----- */

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_UnbalancedAttachmentsAndCaptionsBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_1_CAPTION)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_PNG)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_JPG)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_WithAttachmentImagesCreated() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_2_CAPTIONS)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_PNG)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_JPG)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_WithAttachmentVideosCreated() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_2_CAPTIONS)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_VIDEO_MP4)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_VIDEO_WEBM)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_WithMixedAttachmentsCreated() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_2_CAPTIONS)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_VIDEO_MP4)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_JPG)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_DuplicateAttachmentsBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_3_CAPTIONS)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_VIDEO_MP4)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_PNG)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_PNG)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_MemberAttachmentOverLimitBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_4_CAPTIONS)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_VIDEO_MP4)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_PNG)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_JPG)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_VIDEO_WEBM)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testPostBugReport_NonMemberAttachmentUnderLimitOk() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_1_CAPTION)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_JPG)
        )
        .andExpect(status().isCreated());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testPostBugReport_NonMemberAttachmentsOverLimitBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_VALID_2_CAPTIONS)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_VIDEO_MP4)
                .file(SampleDummyAttachments.SAMPLE_ATT_VALID_IMAGE_JPG)
        )
        .andExpect(status().isBadRequest());
    }

    /* ----- Validity Tests ----- */

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_InvalidSummaryBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_INVALID_SUMMARY)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_InvalidStepsBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_INVALID_STEPS)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_InvalidEnvironmentBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_INVALID_ENVIRONMENT)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_InvalidExpectedBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_INVALID_EXPECTED)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_InvalidActualBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_INVALID_ACTUAL)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_InvalidUserImpactBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_INVALID_USER_IMPACT)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_InvalidSeverityBadRequest() throws Exception {
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_INVALID_USER_IMPACT)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostBugReport_WithUnknownLabelSkippedCreated() throws Exception {
        //It would just skip the unknown label, since the user shouldn't be able to this naturally anyway.
        mockMvc.perform(
            multipart("/api/workspaces/1/bugs/report")
                .file(SampleReportData.SAMPLE_REPORT_DATA_WITH_UNKNOWN_LABEL)
        )
        .andExpect(status().isCreated());
    }

}
