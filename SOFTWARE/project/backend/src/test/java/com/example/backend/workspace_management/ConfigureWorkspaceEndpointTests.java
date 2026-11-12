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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "h2"}) //Use both application-test and application-test-h2.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ConfigureWorkspaceEndpointTests {

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

    /* ---------- Integration Testing GET /api/workspaces/{id}/configure/access Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetWorkspaceConfigureAccess_OwnerOk() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/1/configure/access")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.description").exists())
        .andExpect(jsonPath("$.colour").exists())
        .andExpect(jsonPath("$.is_public").exists())
        .andExpect(jsonPath("$.owner").value("va104"))
        .andExpect(jsonPath("$.user_role").value("OWNER"))
        .andExpect(jsonPath("$.members").exists())
        .andExpect(jsonPath("$.custom_labels").exists());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testGetWorkspaceConfigureAccess_AdminOk() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/1/configure/access")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").exists())
        .andExpect(jsonPath("$.description").exists())
        .andExpect(jsonPath("$.colour").exists())
        .andExpect(jsonPath("$.is_public").exists())
        .andExpect(jsonPath("$.owner").value("va104"))
        .andExpect(jsonPath("$.user_role").value("ADMIN"))
        .andExpect(jsonPath("$.members").exists())
        .andExpect(jsonPath("$.custom_labels").exists());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testGetWorkspaceConfigureAccess_NonAdminForbidden() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/1/configure/access")
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testGetWorkspaceConfigureAccess_WorkspaceNotFound() throws Exception {
        mockMvc.perform(
            get("/api/workspaces/25/configure/access")
        )
        .andExpect(status().isNotFound());
    }

    /* ---------- Integration Testing PATCH /api/workspaces/{id}/configure/details Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceDetails_OwnerOk() throws Exception {
        String request = """
        {
            "name": "A new name",
            "visibility": false,
            "description": "A new description",
            "colour": "green"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/details")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("A new name"))
        .andExpect(jsonPath("$.description").value("A new description"))
        .andExpect(jsonPath("$.colour").value("green"))
        .andExpect(jsonPath("$.is_public").value(false))
        .andExpect(jsonPath("$.owner").value("va104"))
        .andExpect(jsonPath("$.user_role").value("OWNER"));
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPatchWorkspaceDetails_AdminOk() throws Exception {
        String request = """
        {
            "name": "A new name",
            "visibility": false,
            "description": "A new description",
            "colour": "green"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/details")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.id").value(1))
        .andExpect(jsonPath("$.name").value("MyBugspace"))
        .andExpect(jsonPath("$.description").value("A new description"))
        .andExpect(jsonPath("$.colour").value("green"))
        .andExpect(jsonPath("$.is_public").value(true))
        .andExpect(jsonPath("$.owner").value("va104"))
        .andExpect(jsonPath("$.user_role").value("ADMIN"));
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testPatchWorkspaceDetails_NotAdminForbidden() throws Exception {
        String request = """
        {
            "name": "A new name",
            "visibility": false,
            "description": "A new description",
            "colour": "green"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/details")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceDetails_WorkspaceNotFound() throws Exception {
        String request = """
        {
            "name": "A new name",
            "visibility": false,
            "description": "A new description",
            "colour": "green"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/25/configure/details")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceDetails_InvalidWorkspaceNameBadRequest() throws Exception {
        String request = """
        {
            "name": "A",
            "visibility": false,
            "description": "A new description",
            "colour": "green"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/details")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceDetails_InvalidWorkspaceColourBadRequest() throws Exception {
        String request = """
        {
            "name": "ABC-123",
            "visibility": false,
            "description": null,
            "colour": "gr"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/details")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    /* ---------- Integration Testing DELETE /api/workspaces/{id} Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspace_OwnerOk() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1")
        )
        .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testDeleteWorkspace_AdminForbidden() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1")
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testDeleteWorkspace_NonAdminForbidden() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1")
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspace_WorkspaceNotFound() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/25")
        )
        .andExpect(status().isNotFound());
    }

    /* ---------- Integration Testing POST /api/workspaces/{id}/configure/labels Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceCustomLabel_OwnerValidCreated() throws Exception {
        String request = """
        {
            "name": "okay",
            "colour": "violet"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPostAddWorkspaceCustomLabel_AdminValidCreated() throws Exception {
        String request = """
        {
            "name": "another",
            "colour": "blue"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceCustomLabel_NotUniqueBadRequest() throws Exception {
        String request = """
        {
            "name": "okay",
            "colour": "violet"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        );
        mockMvc.perform(
            post("/api/workspaces/1/configure/labels")
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceCustomLabel_InvalidLabelNameBadRequest() throws Exception {
        String request = """
        {
            "name": "",
            "colour": "violet"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceCustomLabel_InvalidLabelColourBadRequest() throws Exception {
        String request = """
        {
            "name": "labelName",
            "colour": "v"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testPostAddWorkspaceCustomLabel_NonAdminForbidden() throws Exception {
        String request = """
        {
            "name": "labelName",
            "colour": "violet"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceCustomLabel_WorkspaceNotFound() throws Exception {
        String request = """
        {
            "name": "labelName",
            "colour": "violet"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/25/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isNotFound());
    }

    /* ---------- Integration Testing DELETE /api/workspaces/{id}/configure/labels/{labelName} Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspaceCustomLabel_OwnerOk() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/labels/happy")
        )
        .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testDeleteWorkspaceCustomLabel_AdminOk() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/labels/happy")
        )
        .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testDeleteWorkspaceCustomLabel_NonAdminForbidden() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/labels/happy")
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspaceCustomLabel_WorkspaceNotFound() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/25/configure/labels/happy")
        )
        .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspaceCustomLabel_InvalidLabelNameBadRequest() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/labels/happ")
        )
        .andExpect(status().isOk()); //Since idempotency here
    }

    /* ---------- Integration Testing PATCH /api/workspaces/{id}/configure/labels Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceCustomLabelColour_OwnerOk() throws Exception {
        String request = """
        {
            "name": "happy",
            "colour": "blue"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("[?(@.name == 'happy')].colour").value("blue"));
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPatchWorkspaceCustomLabelColour_AdminOk() throws Exception {
        String request = """
        {
            "name": "happy",
            "colour": "blue"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("[?(@.name == 'happy')].colour").value("blue"));
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testPatchWorkspaceCustomLabelColour_NonAdminForbidden() throws Exception {
        String request = """
        {
            "name": "happy",
            "colour": "blue"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testPatchWorkspaceCustomLabelColour_NonMemberForbidden() throws Exception {
        String request = """
        {
            "name": "happy",
            "colour": "blue"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceCustomLabelColour_WorkspaceCustomLabelNotFound() throws Exception {
        String request = """
        {
            "name": "happ",
            "colour": "blue"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceCustomLabelColour_InvalidLabelColourBadRequest() throws Exception {
        String request = """
        {
            "name": "happy",
            "colour": "blu"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceCustomLabelColour_WorkspaceNotFound() throws Exception {
        String request = """
        {
            "name": "labelName",
            "colour": "sky"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/25/configure/labels")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isNotFound());
    }

    /* ---------- Integration Testing POST /api/workspaces/{id}/configure/members Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceMember_OwnerAddsAdminOk() throws Exception {
        String request = """
        {
            "username": "demonstrator3",
            "role": "admin"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceMember_OwnerAddsNonAdminOk() throws Exception {
        String request = """
        {
            "username": "demonstrator3",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPostAddWorkspaceMember_AdminAddsNonAdminOk() throws Exception {
        String request = """
        {
            "username": "demonstrator3",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$").isArray());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPostAddWorkspaceMember_AdminAddsAdminForbidden() throws Exception {
        String request = """
        {
            "username": "demonstrator3",
            "role": "admin"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPostAddWorkspaceMember_AdminAddsOwnerForbidden() throws Exception {
        String request = """
        {
            "username": "demonstrator3",
            "role": "owner"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testPostAddWorkspaceMember_NonAdminAddsUserForbidden() throws Exception {
        String request = """
        {
            "username": "demonstrator3",
            "role": "reporter"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceMember_UserNotExistBadRequest() throws Exception {
        String request = """
        {
            "username": "nonExistentUser",
            "role": "reporter"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceMember_InvalidUserBadRequest() throws Exception {
        String request = """
        {
            "username": "Invalid User",
            "role": "reporter"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceMember_InvalidRoleBadRequest() throws Exception {
        String request = """
        {
            "username": "demonstrator3",
            "role": "repor"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceMember_AlreadyMemberBadRequest() throws Exception {
        String request = """
        {
            "username": "demonstrator1",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceMember_AddingSelfBadRequest() throws Exception {
        String request = """
        {
            "username": "va104",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPostAddWorkspaceMember_WorkspaceNotFound() throws Exception {
        String request = """
        {
            "username": "demonstrator3",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            post("/api/workspaces/25/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isNotFound());
    }

    /* ---------- Integration Testing DELETE /api/workspaces/{id}/configure/members/{memberUsername} Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspaceMember_OwnerRemovesAdminOk() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/members/demonstrator1")
        )
        .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspaceMember_OwnerRemovesNonAdminOk() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/members/demonstrator2")
        )
        .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testDeleteWorkspaceMember_AdminRemovesNonAdminOk() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/members/demonstrator2")
        )
        .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testDeleteWorkspaceMember_AdminRemovesAdminForbidden() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/members/demonstrator1")
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testDeleteWorkspaceMember_AdminRemovesOwnerForbidden() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/members/va104")
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testDeleteWorkspaceMember_NonAdminRemovesUserForbidden() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/members/demonstrator1")
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testDeleteWorkspaceMember_NonMemberRemovesUserForbidden() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/members/demonstrator2")
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspaceMember_UnknownMemberBadRequest() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/members/unknownMember")
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspaceMember_RemoveNonMemberBadRequest() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/members/demonstrator3")
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspaceMember_InvalidMemberUsernameBadRequest() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/1/configure/members/Invalid User")
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testDeleteWorkspaceMember_WorkspaceNotFound() throws Exception {
        mockMvc.perform(
            delete("/api/workspaces/25/configure/members/demonstrator1")
        )
        .andExpect(status().isNotFound());
    }

    /* ---------- Integration Testing PATCH /api/workspaces/{id}/configure/members Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceMemberRole_OwnerDemotesAdminToNonAdminOk() throws Exception {
        String request = """
        {
            "username": "demonstrator1",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("[?(@.username == 'demonstrator1')].role").value("DEVELOPER"));
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceMemberRole_OwnerPromotesNonAdminToAdminOk() throws Exception {
        String request = """
        {
            "username": "demonstrator2",
            "role": "admin"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("[?(@.username == 'demonstrator2')].role").value("ADMIN"));
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceMemberRole_AdminUpdatesNonAdminOk() throws Exception {
        String request = """
        {
            "username": "demonstrator2",
            "role": "reporter"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("[?(@.username == 'demonstrator2')].role").value("REPORTER"));
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPatchWorkspaceMemberRole_AdminPromotesNonAdminToAdminForbidden() throws Exception {
        String request = """
        {
            "username": "demonstrator2",
            "role": "admin"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPatchWorkspaceMemberRole_AdminPromotesNonAdminToOwnerForbidden() throws Exception {
        String request = """
        {
            "username": "demonstrator2",
            "role": "owner"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPatchWorkspaceMemberRole_AdminDemotesAdminToNonAdminForbidden() throws Exception {
        String request = """
        {
            "username": "demonstrator1",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testPatchWorkspaceMemberRole_AdminDemotesOwnerForbidden() throws Exception {
        String request = """
        {
            "username": "va104",
            "role": "admin"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testPatchWorkspaceMemberRole_NonAdminDemotesOwnerForbidden() throws Exception {
        String request = """
        {
            "username": "va104",
            "role": "admin"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testPatchWorkspaceMemberRole_NonAdminDemotesUserForbidden() throws Exception {
        String request = """
        {
            "username": "demonstrator1",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testPatchWorkspaceMemberRole_NonMemberDemotesUserForbidden() throws Exception {
        String request = """
        {
            "username": "demonstrator1",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("demonstrator3")
    void testPatchWorkspaceMemberRole_NonMemberDemotesOwnerForbidden() throws Exception {
        String request = """
        {
            "username": "va104",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isForbidden());
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceMemberRole_UserNotMemberBadRequest() throws Exception {
        String request = """
        {
            "username": "demonstrator3",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceMemberRole_InvalidMemberUserBadRequest() throws Exception {
        String request = """
        {
            "username": "Invalid User",
            "role": "developer"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceMemberRole_InvalidMemberRoleBadRequest() throws Exception {
        String request = """
        {
            "username": "Invalid User",
            "role": "develop"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/1/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    @WithUserDetails("va104")
    void testPatchWorkspaceMemberRole_WorkspaceNotFound() throws Exception {
        String request = """
        {
            "username": "demonstrator2",
            "role": "reporter"
        }
        """;
        mockMvc.perform(
            patch("/api/workspaces/25/configure/members")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isNotFound());
    }

}
