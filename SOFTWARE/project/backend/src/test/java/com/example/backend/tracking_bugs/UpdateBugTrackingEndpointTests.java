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
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "mysql"}) //Use both application-test and application-test-mysql.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class UpdateBugTrackingEndpointTests {

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
            sampleInitialiserService.initialiseExtraDemoUsers();
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

    /* ---------- Integration Testing PATCH /api/workspaces/{id}/bugs/{bug_id}/summary Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testUpdateBugSummary_OwnerOk() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/summary").param("summary", "This is the new bug summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("This is the new bug summary"));
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testUpdateBugSummary_AdminOk() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/summary").param("summary", "This is the new bug summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("This is the new bug summary"));
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testUpdateBugSummary_DeveloperOk() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/summary").param("summary", "This is the new bug summary"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("This is the new bug summary"));
    }

    @Test
    @WithUserDetails("demonstrator4")
    void testUpdateBugSummary_ReporterForbidden() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/summary").param("summary", "This is the new bug summary"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testUpdateBugSummary_NonMemberForbidden() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/summary").param("summary", "This is the new bug summary"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugSummary_WorkspaceNotFound() throws Exception {
        mockMvc.perform(patch("/api/workspaces/25/bugs/1/summary").param("summary", "This is the new bug summary"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugSummary_BugNotFound() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/25/summary").param("summary", "This is the new bug summary"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugSummary_NoInputBadRequest() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/summary").param("summary", ""))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugSummary_InvalidSummaryBadRequest() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/summary").param("summary", "This summary is super long".repeat(10)))
            .andExpect(status().isBadRequest());
    }

    /* ---------- Integration Testing PATCH /api/workspaces/{id}/bugs/{bug_id}/status Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testUpdateBugStatus_OwnerOk() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/status").param("status", "CONFIRMED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("CONFIRMED"));
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testUpdateBugStatus_AdminOk() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/status").param("status", "CONFIRMED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("CONFIRMED"));
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testUpdateBugStatus_DeveloperOk() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/status").param("status", "CONFIRMED"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("CONFIRMED"));
    }

    @Test
    @WithUserDetails("demonstrator4")
    void testUpdateBugStatus_ReporterForbidden() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/status").param("status", "CONFIRMED"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testUpdateBugStatus_NonMemberForbidden() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/status").param("status", "CONFIRMED"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugStatus_WorkspaceNotFound() throws Exception {
        mockMvc.perform(patch("/api/workspaces/25/bugs/1/status").param("status", "CONFIRMED"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugStatus_BugNotFound() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/25/status").param("status", "CONFIRMED"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugStatus_NoInputBadRequest() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/status").param("status", ""))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugStatus_InvalidStatusBadRequest() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/status").param("status", "CONFIRM"))
            .andExpect(status().isBadRequest());
    }

    /* ---------- Integration Testing PATCH /api/workspaces/{id}/bugs/{bug_id}/progress Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testUpdateBugProgress_OwnerOk() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/progress").param("progress", "IN_PROGRESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("IN_PROGRESS"));
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testUpdateBugProgress_AdminOk() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/progress").param("progress", "IN_PROGRESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("IN_PROGRESS"));
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testUpdateBugProgress_DeveloperOk() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/progress").param("progress", "IN_PROGRESS"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").value("IN_PROGRESS"));
    }

    @Test
    @WithUserDetails("demonstrator4")
    void testUpdateBugProgress_ReporterForbidden() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/progress").param("progress", "IN_PROGRESS"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testUpdateBugProgress_NonMemberForbidden() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/progress").param("progress", "IN_PROGRESS"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugProgress_WorkspaceNotFound() throws Exception {
        mockMvc.perform(patch("/api/workspaces/25/bugs/1/progress").param("progress", "IN_PROGRESS"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugProgress_BugNotFound() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/25/progress").param("progress", "IN_PROGRESS"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugProgress_NoInputBadRequest() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/progress").param("progress", ""))
            .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugProgress_InvalidProgressBadRequest() throws Exception {
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/progress").param("progress", "PROGRESS"))
            .andExpect(status().isBadRequest());
    }

    /* ---------- Integration Testing PATCH /api/workspaces/{id}/bugs/{bug_id}/labels Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testUpdateBugLabels_OwnerOk() throws Exception {
        String request = """
            {
                "add": ["happy", "organic"],
                "remove": []
            }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].name").value(hasItems("happy", "organic")));
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testUpdateBugLabels_AdminOk() throws Exception {
        String request = """
            { "add": ["happy", "organic"], "remove": [] }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].name").value(hasItems("happy", "organic")));
    }
    @Test
    @WithUserDetails("demonstrator2")
    void testUpdateBugLabels_DeveloperOk() throws Exception {
        String request = """
            { "add": ["happy", "organic"], "remove": [] }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[*].name").value(hasItems("happy", "organic")));
    }
    @Test
    @WithUserDetails("demonstrator4")
    void testUpdateBugLabels_ReporterForbidden() throws Exception {
        String request = """
            { "add": ["happy", "organic"], "remove": [] }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isForbidden());
    }
    @Test
    @WithUserDetails("demonstrator3")
    void testUpdateBugLabels_NonMemberForbidden() throws Exception {
        String request = """
            { "add": ["happy", "organic"], "remove": [] }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugLabels_WorkspaceNotFound() throws Exception {
        String request = """
            { "add": ["happy", "organic"], "remove": [] }
        """;
        mockMvc.perform(patch("/api/workspaces/25/bugs/1/labels")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugLabels_BugNotFound() throws Exception {
        String request = """
            { "add": ["happy", "organic"], "remove": [] }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/25/labels")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugLabels_RemoveLabelOk() throws Exception {
        String request1 = """
            {
                "add": ["happy", "organic"],
                "remove": []
            }
        """;
        String request2 = """
            {
                "add": [],
                "remove": ["happy"]
            }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request1)
        );
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request2)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[?(@.name == 'happy')]").doesNotExist());
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugLabels_AddLabelOk() throws Exception {
        String request1 = """
            {
                "add": [],
                "remove": ["happy", "organic"]
            }
        """;
        String request2 = """
            {
                "add": ["happy"],
                "remove": []
            }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request1)
        );
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request2)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[*].name").value(hasItem("happy")));
    }

    @Test
    @WithUserDetails("va104")
    void testUpdateBugLabels_NullInputsBadRequest() throws Exception {
        String request = """
            { "add": null, "remove": null }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/labels")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isBadRequest());
    }

}
