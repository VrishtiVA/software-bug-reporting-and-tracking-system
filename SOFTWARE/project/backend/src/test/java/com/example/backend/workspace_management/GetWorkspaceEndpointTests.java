package com.example.backend.workspace_management;

import com.example.backend.TestSampleInitialiserService;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "h2"}) //Use both application-test and application-test-h2.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class GetWorkspaceEndpointTests {

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

    /* ---------- Integration Testing GET /api/workspaces/owned Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetOwnedWorkspaces_Ok() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/owned")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        //Expecting 1 workspace result
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("[0].id").value(1))
        .andExpect(jsonPath("[0].name").exists())
        .andExpect(jsonPath("[0].colour").exists())
        .andExpect(jsonPath("[0].is_public").exists())
        .andExpect(jsonPath("[0].member_count").exists())
        .andExpect(jsonPath("[0].bug_count").exists());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testGetOwnedWorkspaces_NoContent() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/owned")
        )
        .andExpect(status().isNoContent());
    }

    /* ---------- Integration Testing GET /api/workspaces/member Endpoint ---------- */

    @Test
    @WithUserDetails("demonstrator1")
    void testGetMemberWorkspacesMember_Ok() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/member")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        //Expecting 1 workspace result
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("[0].id").value(1))
        .andExpect(jsonPath("[0].name").exists())
        .andExpect(jsonPath("[0].colour").exists())
        .andExpect(jsonPath("[0].is_public").exists())
        .andExpect(jsonPath("[0].role").exists())
        .andExpect(jsonPath("[0].member_count").exists())
        .andExpect(jsonPath("[0].bug_count").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetMemberWorkspaces_NoContent() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/member")
        )
        .andExpect(status().isNoContent());
    }

    /* ---------- Integration Testing GET /api/workspaces/{id} Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetWorkspace_Ok() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/1")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.description").exists())
        .andExpect(jsonPath("$.colour").exists())
        .andExpect(jsonPath("$.is_public").exists())
        .andExpect(jsonPath("$.owner").value("va104"))
        .andExpect(jsonPath("$.user_role").value("OWNER"));
    }

    @Test
    @WithUserDetails("va104")
    void testGetWorkspace_NotFound() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/25")
        )
        .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetWorkspace_PrivateNonMemberForbidden() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/2")
        )
        .andExpect(status().isForbidden());
    }

    /* ---------- Integration Testing GET /api/workspaces/{id}/members Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetWorkspaceMembers_Ok() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/1/members")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("[0].user_id").exists())
        .andExpect(jsonPath("[0].username").exists())
        .andExpect(jsonPath("[0].role").exists())
        .andExpect(jsonPath("[0].user_colour").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetWorkspaceMembers_WorkspaceNotFound() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/25/members")
        )
        .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetWorkspaceMembers_PrivateNonMemberForbidden() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/2/members")
        )
        .andExpect(status().isForbidden());
    }

}
