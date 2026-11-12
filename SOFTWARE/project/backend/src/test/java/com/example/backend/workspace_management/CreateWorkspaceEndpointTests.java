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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "h2"}) //Use both application-test and application-test-h2.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class CreateWorkspaceEndpointTests {

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
            //Initialise maximum workspaces for one user.
            sampleInitialiserService.initialiseMaximumSampleWorkspacesForUser();
        }
    }

    /* ---------- Integration Testing GET /api/workspaces/create/access Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetWorkspaceCreateAccess_True() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/create/access")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string("true"));
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testGetWorkspaceCreateAccess_False() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/create/access")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string("false"));
    }

    /* ---------- Integration Testing GET /api/workspaces/create/validate/member/{username} Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetWorkspaceCreateValidMember_Ok() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/create/validate/member/demonstrator1")
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.user_id").value(2))
        .andExpect(jsonPath("$.username").value("demonstrator1"))
        .andExpect(jsonPath("$.user_colour").exists());
    }

    @Test
    @WithUserDetails("va104")
    void testGetWorkspaceCreateValidMember_NotFound() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/create/validate/member/unknownUser")
        )
        .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testGetWorkspaceCreateValidMember_AddSelfAttemptBadRequest() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/create/validate/member/va104")
        )
        .andExpect(status().isBadRequest());
    }

    /* ---------- Integration Testing POST /api/workspaces Endpoint ---------- */

    @Test
    @WithUserDetails("demonstrator1")
    void testPostCreateWorkspace_Created() throws Exception {
        String request = """
        {
            "name": "Brand New Workspace",
            "visibility": true,
            "description": null,
            "colour": "blue",
            "labels": [
                {
                    "name": "yes",
                    "colour": "green"
                },
                {
                    "name": "no",
                    "colour": "red"
                }
            ],
            "members": [
                {
                    "username": "va104",
                    "role": "admin"
                },
                {
                    "username": "demonstrator2",
                    "role": "developer"
                },
                {
                    "username": "demonstrator3",
                    "role": "reporter"
                }
            ]
        }
        """;
        mockMvc.perform(
            post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"));
    }

    @Test
    @WithUserDetails("va104")
    void testPostCreateWorkspace_NoMembersAndNoLabelsCreated() throws Exception {
        String request = """
        {
            "name": "Brand New Workspace",
            "visibility": true,
            "description": "This is a brand new workspace for the purpose of testing.",
            "colour": "blue",
            "labels": [],
            "members": []
        }
        """;
        mockMvc.perform(
            post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"));
    }

    @Test
    @WithUserDetails("va104")
    void testPostCreateWorkspace_NoDescriptionAndNoMembersAndNoLabelsCreated() throws Exception {
        String request = """
        {
            "name": "Brand New Workspace",
            "visibility": true,
            "description": null,
            "colour": "blue",
            "labels": [],
            "members": []
        }
        """;
        mockMvc.perform(
            post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(header().exists("Location"));
    }

    @Test
    @WithUserDetails("va104")
    void testPostCreateWorkspace_InvalidColourBadRequest() throws Exception {
        String request = """
        {
            "name": "Brand New Workspace",
            "visibility": true,
            "description": null,
            "colour": "blu",
            "labels": [],
            "members": []
        }
        """;
        mockMvc.perform(
            post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testPostCreateWorkspace_ReachedMaximumWorkspacesBadRequest() throws Exception {
        String request = """
        {
            "name": "Brand New Workspace",
            "visibility": true,
            "description": null,
            "colour": "blue",
            "labels": [],
            "members": []
        }
        """;
        mockMvc.perform(
            post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPostCreateWorkspace_AddSelfAsMemberBadRequest() throws Exception {
        String request = """
        {
            "name": "Brand New Workspace",
            "visibility": true,
            "description": null,
            "colour": "blue",
            "labels": [],
            "members": [
                {
                    "username": "demonstrator1",
                    "role": "admin"
                }
            ]
        }
        """;
        mockMvc.perform(
            post("/api/workspaces")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

}
