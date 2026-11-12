package com.example.backend.user_management;

import com.example.backend.TestSampleInitialiserService;
import com.example.backend.services.users.UserService;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "h2"}) //Use both application-test and application-test-h2.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AuthenticationEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;
    @Autowired
    private TestSampleInitialiserService sampleInitialiserService;

    @BeforeAll
    void setUp() {
        //Initialise sample user.
        if (userService.isUserRepositoryEmpty()) {
            sampleInitialiserService.initialiseSampleUser();
        }
    }

    /* ---------- Integration Testing /api/auth/signin Endpoint ---------- */

    @Test
    void testSignInValid() throws Exception {
        String request = """
            {
                "username": "va104",
                "password": "password"
            }
        """;
        mockMvc.perform(
            post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.currentUser").value("va104"))
        .andExpect(jsonPath("$.jwtToken").exists())
        .andExpect(jsonPath("$.avatarColour").exists());
    }

    @Test
    void testSignInEmpty() throws Exception {
        String request = """
            {
                "username": " ",
                "password": " "
            }
        """;
        mockMvc.perform(
            post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void testSignInWrongPassword() throws Exception {
        String request = """
            {
                "username": "va104",
                "password": "wrong_password"
            }
        """;
        mockMvc.perform(
            post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isUnauthorized());
    }

    @Test
    void testSignInUserNotExist() throws Exception {
        String request = """
            {
                "username": "va123",
                "password": "password"
            }
        """;
        mockMvc.perform(
            post("/api/auth/signin")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isUnauthorized());
    }

    /* ---------- Integration Testing /api/auth/signup Endpoint ---------- */

    @Test
    void testSignUpValid() throws Exception {
        String request = """
            {
                "email": "va112@student.le.ac.uk",
                "username": "va112",
                "password": "password"
            }
        """;
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.currentUser").value("va112"))
        .andExpect(jsonPath("$.jwtToken").exists())
        .andExpect(jsonPath("$.avatarColour").exists());
    }

    @Test
    void testSignUpEmpty() throws Exception {
        String request = """
            {
                "email": " ",
                "username": " ",
                "password": null
            }
        """;
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    @Test
    void testSignUpConflict() throws Exception {
        String request = """
            {
                "email": "va104@student.le.ac.uk",
                "username": "va104",
                "password": "password"
            }
        """;
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isConflict());
    }

    @Test
    void testSignUpInvalid() throws Exception {
        String request = """
            {
                "email": "va104@student.",
                "username": "va104",
                "password": "password"
            }
        """;
        mockMvc.perform(
            post("/api/auth/signup")
                .contentType(MediaType.APPLICATION_JSON)
                .content(request)
        )
        .andExpect(status().isBadRequest());
    }

    /* ---------- Integration Testing /api/auth/validate/is-username-available Endpoint ---------- */

    @Test
    void testIsUsernameAvailableTrue() throws Exception {
        mockMvc.perform(
            get("/api/auth/validate/is-username-available")
                .param("username", "va123")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string("true"));
    }

    @Test
    void testIsUsernameAvailableFalse() throws Exception {
        mockMvc.perform(
            get("/api/auth/validate/is-username-available")
                .param("username", "va104")
        )
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(content().string("false"));
    }

}
