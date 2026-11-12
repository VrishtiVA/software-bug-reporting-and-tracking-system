package com.example.backend.viewing_bugs;

import com.example.backend.TestSampleInitialiserService;
import com.example.backend.services.reports.ReportService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspaceService;
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

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "mysql"}) //Use both application-test and application-test-mysql.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BugExplorerEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private ReportService reportService;
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
            //Initialise sample reports.
            sampleInitialiserService.initialiseSampleReport(true);
            sampleInitialiserService.initialiseAdditionalSampleReports(true);
            //Wait for reports to return from duplicate detection process.
            //Using awaitility here, wait/poll until certain condition is met, up to a certain amount of time.
            await().atMost(30, TimeUnit.SECONDS)
                .pollInterval(500, TimeUnit.MILLISECONDS)
                .until(() -> reportService.completedSampleReportsInitialisation());
        }
    }

    /* ---------- Integration Testing GET /api/workspaces/{id}/bugs/{bug_id}/reports Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedReports_FirstPageOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/reports"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.content").isNotEmpty()) //Not empty since bug should contain more than 1 report.
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(15))
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").value(true))
            .andExpect(jsonPath("$.is_last").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedReports_WorkspaceNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/25/bugs/1/reports"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedReports_WorkspaceAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/workspaces/2/bugs/1/reports"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedReports_BugNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/25/reports"))
            .andExpect(status().isNotFound());
    }

    /**
     * It can be difficult to test unstable bug as it may not exist,
     * due to asynchronous nature of how duplicate detection process concludes depending on order of reports,
     * therefore, this test allows either from 403 or 404.
     */
    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedReports_UnstableBugForbidden() throws Exception {
        int status = mockMvc.perform(get("/api/workspaces/2/bugs/1/reports")).andReturn().getResponse().getStatus();
        assertThat(status).isIn(403, 404);
    }

    /* ---------- Integration Testing GET /workspaces/{id}/bugs/{bug_id}/attachments/{type} Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedAttachmentMetadata_ImagesFirstPageOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/attachments/images"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(15))
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").value(true))
            .andExpect(jsonPath("$.is_last").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedAttachmentMetadata_VideosFirstPageOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/attachments/videos"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(15))
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").value(true))
            .andExpect(jsonPath("$.is_last").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedAttachmentMetadata_WorkspaceNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/25/bugs/1/attachments/images"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedAttachmentMetadata_WorkspaceAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/workspaces/2/bugs/1/attachments/images"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedAttachmentMetadata_BugNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/25/attachments/images"))
            .andExpect(status().isNotFound());
    }

    /* ---------- Integration Testing GET /workspaces/{id}/bugs/{bug_id}/fields/{field} Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedFieldData_StepsToReproduceFirstPageOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/fields/steps"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(15))
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").value(true))
            .andExpect(jsonPath("$.is_last").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedFieldData_EnvironmentFirstPageOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/fields/environment"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(15))
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").value(true))
            .andExpect(jsonPath("$.is_last").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedFieldData_ExpectedResultsFirstPageOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/fields/expected"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(15))
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").value(true))
            .andExpect(jsonPath("$.is_last").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedFieldData_ActualResultsFirstPageOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/fields/actual"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(15))
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").value(true))
            .andExpect(jsonPath("$.is_last").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedFieldData_FixSuggestionsFirstPageOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/fields/fix-suggestions"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(15))
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").value(true))
            .andExpect(jsonPath("$.is_last").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedFieldData_AdditionalCommentsFirstPageOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/fields/additional-comments"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(15))
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").value(true))
            .andExpect(jsonPath("$.is_last").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedFieldData_InvalidFieldBadRequest() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/fields/images"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedFieldData_WorkspaceNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/25/bugs/1/fields/steps"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedFieldData_WorkspaceAccessFrrbidden() throws Exception {
        mockMvc.perform(get("/api/workspaces/2/bugs/1/fields/steps"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugPaginatedFieldData_BugNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/25/fields/steps"))
            .andExpect(status().isNotFound());
    }

}
