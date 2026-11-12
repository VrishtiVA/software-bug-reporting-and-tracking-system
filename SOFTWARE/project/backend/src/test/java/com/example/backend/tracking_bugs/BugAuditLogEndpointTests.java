package com.example.backend.tracking_bugs;

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

import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "mysql"}) //Use both application-test and application-test-mysql.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BugAuditLogEndpointTests {

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

    /* ---------- Integration Testing GET /api/workspaces/{id}/bugs/{bug_id}/audit-log Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_OwnerOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testGetAuditLogs_AdminOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk());
    }
    @Test
    @WithUserDetails("va104")
    void testGetAuditLogsDeveloperOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk());
    }
    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_ReporterOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_WorkspaceNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/25/bugs/1/audit-log"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_BugNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/25/audit-log"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_FirstPageOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.page_number").value(0))
            .andExpect(jsonPath("$.page_size").value(15))
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").value(true))
            .andExpect(jsonPath("$.is_last").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_BugCreationLogPresent() throws Exception {
        //Requesting with page size long enough to have desired item in first page.
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log").param("size", "50"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[-1].log_type").value("CREATION"));
    }

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_BugAssignmentLogFound() throws Exception {
        String request = """
            { "assign": [1, 2, 3], "unassign": [] }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/bugs/1/assignees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        );
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].log_type").value("ASSIGNMENT"));
    }

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_BugSelfAssignmentLogFound() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs/1/assignees/join"));
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].log_type").value("ASSIGNMENT"));
    }

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_EditBugSummaryLogFound() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/summary").param("summary", "This is the new bug summary"));
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].field").value("SUMMARY"))
            .andExpect(jsonPath("$.content[0].log_type").value("MODIFICATION"));
    }

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_EditBugStatusLogFound() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/status").param("status", "CONFIRMED"));
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].field").value("STATUS"))
            .andExpect(jsonPath("$.content[0].log_type").value("PROGRESSION"));
    }

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_EditBugProgressLogFound() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/progress").param("progress", "IN_PROGRESS"));
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].field").value("PROGRESS"))
            .andExpect(jsonPath("$.content[0].log_type").value("PROGRESSION"));
    }

    @Test
    @WithUserDetails("va104")
    void testGetAuditLogs_EditBugLabelsLogFound() throws Exception {
        String request = """
            { "add": ["happy", "organic"], "remove": [] }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        );
        mockMvc.perform(get("/api/workspaces/1/bugs/1/audit-log"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.content").isArray())
            .andExpect(jsonPath("$.content[0].field").value("LABELS"))
            .andExpect(jsonPath("$.content[0].log_type").value("MODIFICATION"));
    }

}
