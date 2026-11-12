package com.example.backend.assigning_bugs;

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

import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "mysql"}) //Use both application-test and application-test-mysql.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BugAssignmentEndpointTests {

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

    /* ---------- Integration Testing GET /api/workspaces/{id}/bugs/{bug_id}/assignees Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetBugAssignmentMaterials_OwnerOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/assignees"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.assignees").exists())
            .andExpect(jsonPath("$.assignable").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugAssignmentMaterials_WorkspaceNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/25/bugs/1/assignees"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetBugAssignmentMaterials_BugNotFound() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/25/assignees"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testGetBugAssignmentMaterials_AdminOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/assignees"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testGetBugAssignmentMaterials_DeveloperOk() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/assignees"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator4")
    void testGetBugAssignmentMaterials_ReporterForbidden() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/assignees"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testGetBugAssignmentMaterials_NonMemberForbidden() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/bugs/1/assignees"))
            .andExpect(status().isForbidden());
    }

    /* ---------- Integration Testing POST /api/workspaces/{id}/bugs/{bug_id}/assignees/join Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testAssignSelfToBug_OwnerOk() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs/1/assignees/join"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails("va104")
    void testAssignSelfToBug_AlreadyAssignedConflict() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs/1/assignees/join"));
        mockMvc.perform(post("/api/workspaces/1/bugs/1/assignees/join"))
            .andExpect(status().isConflict());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testAssignSelfToBug_AdminOk() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs/1/assignees/join"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testAssignSelfToBug_DeveloperOk() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs/1/assignees/join"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator4")
    void testAssignSelfToBug_ReporterForbidden() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs/1/assignees/join"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testAssignSelfToBug_NonMemberForbidden() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs/1/assignees/join"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testAssignSelfToBug_WorkspaceNotFound() throws Exception {
        mockMvc.perform(post("/api/workspaces/25/bugs/1/assignees/join"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testAssignSelfToBug_BugNotFound() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs/25/assignees/join"))
            .andExpect(status().isNotFound());
    }

    /* ---------- Integration Testing PATCH /api/workspaces/{id}/bugs/{bug_id}/assignees Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testEditBugAssignment_OwnerAssignAllRolesOk() throws Exception {
        String request = """
            {
                "assign": [1, 2, 3],
                "unassign": []
            }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/bugs/1/assignees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[*].user.user_id").value(containsInAnyOrder(1, 2, 3)));
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testEditBugAssignment_AdminCannotAssignOwnerOk() throws Exception {
        String request = """
            {
                "assign": [1, 2, 3],
                "unassign": []
            }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/bugs/1/assignees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[*].user.user_id").value(containsInAnyOrder(2, 3)));
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testEditBugAssignment_DeveloperCannotAssignOwnerOrAdminOk() throws Exception {
        String request = """
            {
                "assign": [1, 2, 3],
                "unassign": []
            }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/bugs/1/assignees")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[*].user.user_id").value(containsInAnyOrder(3)));
    }

    @Test
    @WithUserDetails("va104")
    void testEditBugAssignment_OwnerUnassignAllRolesOk() throws Exception {
        String request1 = """
            {
                "assign": [1, 2, 3],
                "unassign": []
            }
        """;
        String request2 = """
            {
                "assign": [],
                "unassign": [1, 2, 3]
            }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/assignees")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request1)
        );
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/assignees")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request2)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[*].user.user_id").isEmpty());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testEditBugAssignment_AdminCantUnassignOwnerOk() throws Exception {
        //Set up sample assignments for test
        sampleInitialiserService.assignSampleUsers(1L, 1L, List.of(1L,2L,3L));
        //Actual test
        String request = """
            {
                "assign": [],
                "unassign": [1, 2, 3]
            }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/assignees")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[*].user.user_id").value(containsInAnyOrder(1)));
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testEditBugAssignment_DeveloperCantUnassignAdminOrOwnerOk() throws Exception {
        //Set up sample assignments for test
        sampleInitialiserService.assignSampleUsers(1L, 1L, List.of(1L,2L,3L));
        //Actual test
        String request = """
            {
                "assign": [],
                "unassign": [1, 2, 3]
            }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/assignees")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray())
        .andExpect(jsonPath("$[*].user.user_id").value(containsInAnyOrder(1, 2)));
    }

    @Test
    @WithUserDetails("va104")
    void testEditBugAssignment_WorkspaceNotFound() throws Exception {
        String request = """
            {
                "assign": [1, 2, 3],
                "unassign": []
            }
        """;
        mockMvc.perform(patch("/api/workspaces/25/bugs/1/assignees")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testEditBugAssignment_BugNotFound() throws Exception {
        String request = """
            {
                "assign": [1, 2, 3],
                "unassign": []
            }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/25/assignees")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("demonstrator4")
    void testEditBugAssignment_ReporterForbidden() throws Exception {
        String request = """
            {
                "assign": [1, 2, 3],
                "unassign": []
            }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/assignees")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testEditBugAssignment_NonMemberForbidden() throws Exception {
        String request = """
            {
                "assign": [1, 2, 3],
                "unassign": []
            }
        """;
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/assignees")
            .contentType(MediaType.APPLICATION_JSON)
            .content(request)
        )
        .andExpect(status().isForbidden());
    }

}