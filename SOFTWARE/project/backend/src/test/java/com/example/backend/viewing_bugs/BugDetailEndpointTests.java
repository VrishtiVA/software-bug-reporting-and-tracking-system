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
public class BugDetailEndpointTests {

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

    /* ---------- Integration Testing GET /workspaces/{id}/bugs/{bug_id} Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetBugDetailsOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugDetailsWorkspaceNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/25/bugs/1"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugDetailsWorkspaceAccessForbidden() throws Exception {
        mockMvc.perform(get("/api/workspaces/2/bugs/1"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugDetailsBugNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/25"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugDetailsUnstableBugAccessForbidden() throws Exception {
        int status = mockMvc.perform(get("/api/workspaces/2/bugs/1")).andReturn().getResponse().getStatus();
        assertThat(status).isIn(403, 404);
    }

}
