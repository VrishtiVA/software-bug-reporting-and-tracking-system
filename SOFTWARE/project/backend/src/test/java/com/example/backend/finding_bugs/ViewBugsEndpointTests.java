package com.example.backend.finding_bugs;

import com.example.backend.TestSampleInitialiserService;
import com.example.backend.duplicate_detection.utilities.DuplicateDetectionTestService;
import com.example.backend.services.reports.ReportService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspaceService;
import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MvcResult;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "mysql"}) //Use both application-test and application-test-mysql.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class ViewBugsEndpointTests {

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
    void setUpFirst() throws Exception {
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

        //Reject all duplicate suggestions to ensure bugs available for viewing for testing with.
        DuplicateDetectionTestService duplicateDetectionTestService = new DuplicateDetectionTestService(mockMvc);
        duplicateDetectionTestService.rejectAllDuplicates();
    }

    /* ---------- Integration Testing POST /workspaces/{id}/bugs Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_NoParamsOwnerOk() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.page_number").value(0));
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_PaginationOk() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs?size=2&page=1"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.page_size").value(2))
            .andExpect(jsonPath("$.page_number").value(1));
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_SortFrequencyDefaultDescendingOk() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workspaces/1/bugs?sort=frequency"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        List<Integer> frequencies = JsonPath.parse(result.getResponse().getContentAsString()).read("$.content[*].frequency");
        assertThat(frequencies).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_SortFrequencyAscendingOk() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workspaces/1/bugs?sort=frequency&descending=false"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        List<Integer> frequencies = JsonPath.parse(result.getResponse().getContentAsString()).read("$.content[*].frequency");
        assertThat(frequencies).isSortedAccordingTo(Comparator.naturalOrder());
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_SortFirstReportedTimestamp() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workspaces/1/bugs?sort=firstReportedTimestamp"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        List<Integer> timestamps = JsonPath.parse(result.getResponse().getContentAsString()).read("$.content[*].first_reported_timestamp");
        assertThat(timestamps).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_SortFirstReportedPriority() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workspaces/1/bugs?sort=priority"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        List<Integer> priorities = JsonPath.parse(result.getResponse().getContentAsString()).read("$.content[*].priority");
        assertThat(priorities).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_SortFirstReportedHotness() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workspaces/1/bugs?sort=hotness"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        List<Integer> hotness = JsonPath.parse(result.getResponse().getContentAsString()).read("$.content[*].hotness");
        assertThat(hotness).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_SortFirstReportedSuggestions() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workspaces/1/bugs?sort=suggestions"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        List<Integer> suggestions = JsonPath.parse(result.getResponse().getContentAsString()).read("$.content[*].relevant_duplicate_suggestions");
        assertThat(suggestions).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_SortRelevanceIsPriority() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/workspaces/1/bugs?sort=relevance"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andReturn();
        List<Integer> priorities = JsonPath.parse(result.getResponse().getContentAsString()).read("$.content[*].priority");
        assertThat(priorities).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_QueryOk() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs?sort=relevance&query=laptop"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_FiltersNoneClosedOk() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "progress": ["OPEN", "IN_PROGRESS", "IN_TESTING", "IN_REVIEW"],
                    "status": [],
                    "labels": []
                }
            """)
        )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_FiltersNoneOpenOk() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                    "progress": ["IN_PROGRESS", "IN_TESTING", "IN_REVIEW"],
                    "status": [],
                    "labels": []
                }
            """)
        )
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_WorkspaceNotFound() throws Exception {
        mockMvc.perform(post("/api/workspaces/25/bugs"))
            .andExpect(status().isNotFound());
    }

    @Test
    @WithUserDetails("demonstrator1")
    void testSearchBugs_AdminOk() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("demonstrator2")
    void testSearchBugs_DeveloperOk() throws Exception {
        mockMvc.perform(post("/api/workspaces/1/bugs"))
            .andExpect(status().isOk());
    }

    @Test
    @WithUserDetails("va104")
    void testSearchBugs_NonMemberForbidden() throws Exception {
        mockMvc.perform(post("/api/workspaces/2/bugs"))
            .andExpect(status().isForbidden());
    }

    /* ---------- Integration Testing GET /workspaces/{id}/metrics Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testViewWorkspaceMetrics() throws Exception {
        mockMvc.perform(get("/api/workspaces/1/metrics"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.total_bugs").exists())
            .andExpect(jsonPath("$.total_outstanding_duplicate_report_suggestions").exists())
            .andExpect(jsonPath("$.total_staged_duplicate_reports").exists())
            .andExpect(jsonPath("$.workspace_custom_label_usage_distribution").exists())
            .andExpect(jsonPath("$.progress_distribution").exists())
            .andExpect(jsonPath("$.total_bugs_resolved").exists())
            .andExpect(jsonPath("$.priority_category_distribution").exists())
            .andExpect(jsonPath("$.priority_frequency_bug_count_buckets").exists());
    }

    /* ---------- Integration Testing POST /bugs/assignments Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testViewAssignments_NoWorkspaceOk() throws Exception {
        mockMvc.perform(post("/api/bugs/assignments"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.assigned_bugs.page_number").value(0));
    }

    @Test
    @WithUserDetails("va104")
    void testViewAssignments_PaginationOk() throws Exception {
        mockMvc.perform(post("/api/bugs/assignments?page=1&size=2"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.assigned_bugs.page_number").value(1))
            .andExpect(jsonPath("$.assigned_bugs.page_size").value(2));
    }

    @Test
    @WithUserDetails("va104")
    void testViewAssignments_ForWorkspaceOk() throws Exception {
        mockMvc.perform(post("/api/bugs/assignments?workspace_id=1"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.assigned_bugs.content[*].workspace_id", everyItem(hasValue(1))));
    }

    @Test
    @WithUserDetails("va104")
    void testViewAssignments_ForWorkspaceMemberOk() throws Exception {
        //Assign other user first to see
        mockMvc.perform(patch("/api/workspaces/1/bugs/1/assignees")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "assign": [2],
                        "unassign": []
                    }
                """)
        );
        mockMvc.perform(post("/api/bugs/assignments?workspace_id=1&user_id=2"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.assigned_bugs.content[*].workspace_id", everyItem(equalTo(1))))
            .andExpect(jsonPath("$.assigned_bugs.content[*]", hasSize(1)));
    }

    @Test
    @WithUserDetails("va104")
    void testViewAssignments_WorkspaceNotFound() throws Exception {
        mockMvc.perform(post("/api/bugs/assignments?workspace_id=25"))
            .andExpect(status().isNotFound());
    }

    /* ---------- Integration Testing GET /reports Endpoint ---------- */

    @Test
    @WithUserDetails("va104")
    void testViewUsersReports_DescendingDefaultOk() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.page_number").value(0))
            .andReturn();
        List<Integer> timestamps = JsonPath.parse(result.getResponse().getContentAsString()).read("$.content[*].creation_timestamp");
        assertThat(timestamps).isSortedAccordingTo(Comparator.reverseOrder());
    }

    @Test
    @WithUserDetails("va104")
    void testViewUsersReports_PaginationOk() throws Exception {
        mockMvc.perform(get("/api/reports?page=1&size=2"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.page_number").value(1))
            .andExpect(jsonPath("$.page_size").value(2));
    }

    @Test
    @WithUserDetails("va104")
    void testViewUsersReports_SortAscendingOk() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/reports?creation_timestamp_descending=false"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andReturn();
        List<Integer> timestamps = JsonPath.parse(result.getResponse().getContentAsString()).read("$.content[*].creation_timestamp");
        assertThat(timestamps).isSorted(); //Naturally ascending
    }

}
