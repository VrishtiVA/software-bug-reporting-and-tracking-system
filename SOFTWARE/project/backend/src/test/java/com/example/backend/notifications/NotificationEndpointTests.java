package com.example.backend.notifications;

import com.example.backend.TestSampleInitialiserService;
import com.example.backend.services.reports.ReportService;
import com.example.backend.services.users.NotificationService;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles({"test", "mysql"}) //Use both application-test and application-test-mysql.properties
@Transactional //Rollback once done
@TestInstance(TestInstance.Lifecycle.PER_CLASS) //Allow @BeforeAll not have to be static
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class NotificationEndpointTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserService userService;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private NotificationService notificationService;
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
    }

    /* ---------- Integration Testing GET /api/notifications ---------- */

    @Test
    @WithUserDetails("va104")
    void testGetNotifications() throws Exception {
        mockMvc.perform(get("/api/notifications"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "application/json"))
            .andExpect(jsonPath("$.content").exists())
            .andExpect(jsonPath("$.page_number").exists())
            .andExpect(jsonPath("$.page_size").exists())
            .andExpect(jsonPath("$.total_elements").exists())
            .andExpect(jsonPath("$.total_pages").exists())
            .andExpect(jsonPath("$.is_first").exists())
            .andExpect(jsonPath("$.is_last").exists());
    }

    /* ---------- Integration Testing GET /api/notifications/read ---------- */

    @Test
    @WithUserDetails("va104")
    void testMarkOneNotificationAsRead() throws Exception {
        Long unreadNotificationsCount = notificationService.getUnreadNotificationCountByUsername("va104");
        if (unreadNotificationsCount == 0) return;
        List<Long> unreadNotificationIds = notificationService.findUnreadNotificationIdsByUsername("va104");

        //Build request body of notifications to mark as read
        String requestBody = "{\"notification_ids\": [" + unreadNotificationIds.getLast() + "]}";

        //Mark one as read.
        mockMvc.perform(get("/api/notifications/read")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
        ).andExpect(status().isOk());

        //Ensure unread remaining is only one less.
        Long newUnreadNotificationsCount = notificationService.getUnreadNotificationCountByUsername("va104");
        assertEquals(unreadNotificationsCount - 1, newUnreadNotificationsCount);
    }

    @Test
    @WithUserDetails("va104")
    void testMarkAllNotificationsAsRead() throws Exception {
        Long unreadNotificationsCount = notificationService.getUnreadNotificationCountByUsername("va104");
        if (unreadNotificationsCount == 0) return;
        List<Long> unreadNotificationIds = notificationService.findUnreadNotificationIdsByUsername("va104");

        //Build request body of notifications to mark as read
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("{\"notification_ids\": [");
        for (Long notificationId : unreadNotificationIds) {
            stringBuilder
                .append(notificationId)
                .append(",");
        }
        stringBuilder.delete(stringBuilder.length()-1, stringBuilder.length());
        stringBuilder.append("]}");
        String requestBody = stringBuilder.toString();

        //Mark them all as read.
        mockMvc.perform(get("/api/notifications/read")
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestBody)
        ).andExpect(status().isOk());

        //Ensure no unread remain.
        unreadNotificationsCount = notificationService.getUnreadNotificationCountByUsername("va104");
        assertEquals(0L, unreadNotificationsCount);
    }

}
