package com.example.backend.services;

import com.example.backend.model.reports.Report;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.repositories.users.UserRepository;
import com.example.backend.services.bugs.ReportContainerAsyncService;
import com.example.backend.services.reports.ReportService;
import com.example.backend.services.users.NotificationService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Initialise sample entities.
 * Useful for testing during development, usability testing, and demonstrations.
 * Some sample data e.g. bug report data for samples was quickly generated using ChatGPT
 */
@Service
public class SampleInitialiserService {

    private static final Logger log = LoggerFactory.getLogger(SampleInitialiserService.class);

    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private UserService userService;
    @Autowired
    private WorkspaceService workspaceService;
    @Autowired
    private NotificationService notificationService;
    @Autowired
    private ReportService reportService;
    @Autowired
    private ReportContainerAsyncService reportContainerAsyncService;

    /* ---------- Users ---------- */

    /** Method to initialise sample user. */
    public void initialiseSampleUser() {
        User user = new User(
            "va104",
            "va104@student.le.ac.uk",
            passwordEncoder.encode("password"),
            userService.selectRandomAvatarColour(),
            "USER"
        );
        userRepository.save(user);
    }

    /** Method to initialise sample user. */
    public void initialiseDemoUsers() {
        User user;

        //1
        user = new User(
            "demonstrator1",
            "demo1@example.le.ac.uk",
            passwordEncoder.encode("password"),
            userService.selectRandomAvatarColour(),
            "USER"
        );
        userRepository.save(user);

        //2
        user = new User(
            "demonstrator2",
            "demo2@example.le.ac.uk",
            passwordEncoder.encode("password"),
            userService.selectRandomAvatarColour(),
            "USER"
        );
        userRepository.save(user);

        //3
        user = new User(
            "demonstrator3",
            "demo3@example.le.ac.uk",
            passwordEncoder.encode("password"),
            userService.selectRandomAvatarColour(),
            "USER"
        );
        userRepository.save(user);
    }

    public void initialiseExtraDemoUsers() {
        User user;
        user = new User(
            "demonstrator4",
            "demo4@example.le.ac.uk",
            passwordEncoder.encode("password"),
            userService.selectRandomAvatarColour(),
            "USER"
        );
        userRepository.save(user);
    }

    public void initialiseTestUser() {
        User user;
        user = new User(
            "testuser1",
            "test1@example.le.ac.uk",
            passwordEncoder.encode("password"),
            userService.selectRandomAvatarColour(),
            "USER"
        );
        userRepository.save(user);
    }

    /* ---------- Workspaces ---------- */

    /** Method for initialising a sample workspace under my sample user. */
    @Transactional
    public void initialiseSampleWorkspace() {

        User sampleUser = userRepository.findByUsername("va104").orElse(null);
        User sampleMember = userRepository.findByUsername("demonstrator1").orElse(null);
        User sampleMember2 = userRepository.findByUsername("demonstrator2").orElse(null);
        User sampleMember4 = userRepository.findByUsername("demonstrator4").orElse(null);
        if (sampleUser != null && sampleMember != null && sampleMember2 != null) {

            Map<User, WorkspaceRole> members = new HashMap<>();
            members.put(sampleMember, WorkspaceRole.ADMIN);
            members.put(sampleMember2, WorkspaceRole.DEVELOPER);
            if (sampleMember4 != null) members.put(sampleMember4, WorkspaceRole.REPORTER);

            Workspace sampleWorkspace = workspaceService.createWorkspace(
                sampleUser,
                "FictionalApp",
                "Welcome to Fictional App's Bugspace. As you explore and interact with our application, you may encounter issues or unexpected behaviour. If you do, please use the bug report section to let us know what you found. Your reports help us identify problems and improve the overall experience for everyone.",
                true,
                "sky",
                members,
                Map.of(
                    "defect", "orange",
                    "ui", "pink",
                    "performance", "green",
                    "security", "blue",
                    "crash", "red",
                    "data-integrity", "sky"
                )
            );
            notificationService.sendMassAddedToWorkspaceNotification(members.keySet().stream().toList(), sampleWorkspace);

        } else {
            log.warn("Could not initialise sample workspace as sample user not found.");
        }
    }

    @Transactional
    public void initialisePrivateSampleWorkspace() {
        User sampleUser = userRepository.findByUsername("demonstrator2").orElse(null);
        if (sampleUser != null) {
            workspaceService.createWorkspace(
                sampleUser,
                "TheSecretSpace",
                "This workspace is a secret since it is private.",
                false,
                "indigo",
                null,
                null
            );
        } else {
            log.warn("Could not initialise private sample workspace as sample user not found.");
        }
    }

    /* ---------- Reports ---------- */

    @Transactional
    public void initialiseSampleReport(boolean shallSendToComputeEmbedding) {

        User sampleUser = userService.findUserByUsername("va104");
        Workspace sampleWorkspace = workspaceService.findWorkspaceById(1L).orElse(null);
        WorkspaceRole sampleRole = WorkspaceRole.OWNER;

        if (sampleWorkspace != null && sampleUser != null) {

            //Create report
            Report newReport = reportService.createReport(
                sampleWorkspace,
                sampleUser,
                sampleRole,
                "Refresh logs user out of dashboard.",
                "Log into FictionalApp. Navigate to the dashboard. Refresh the browser page using the refresh button or F5.",
                "Windows 11, Google Chrome 122.",
                "The dashboard reloads and the user remains logged in.",
                "The user is redirected to the login page and must authenticate again.",
                3,
                3,
                null,
                null,
                new HashSet<>(),
                new HashSet<>()
            );
            //Send for detection
            if (shallSendToComputeEmbedding) reportContainerAsyncService.handleNewReport(newReport, sampleWorkspace, sampleUser);

        } else {
            log.error("Unable to initialise sample bug report as sample user or sample workspace is missing.");
        }

    }

    @Transactional
    public void initialiseAdditionalSampleReports(boolean shallSendToComputeEmbedding) {
        try {
            User sampleUser = userService.findUserByUsername("va104");
            Workspace sampleWorkspace = workspaceService.findWorkspaceById(1L).orElse(null);
            WorkspaceRole sampleRole = WorkspaceRole.OWNER;

            if (sampleWorkspace != null && sampleUser != null) {

                //Create report #1
                Report newReport = reportService.createReport(
                    sampleWorkspace,
                    sampleUser,
                    sampleRole,
                    "User session lost after page reload",
                    "Sign in to FictionalApp. Wait on the dashboard for a few seconds. Reload the page from the browser.",
                    "macOS Sonoma, Safari 17",
                    "User session should persist and the dashboard should reload normally.",
                    "User is logged out and returned to the sign-in screen.",
                    4,
                    4,
                    null,
                    null,
                    Set.of(
                        workspaceService.getValidWorkspaceCustomLabel(sampleWorkspace, "security")
                    ),
                    new HashSet<>()
                );
                //Send for detection
                if (shallSendToComputeEmbedding) reportContainerAsyncService.handleNewReport(newReport, sampleWorkspace, sampleUser);

                //Create report #2
                newReport = reportService.createReport(
                    sampleWorkspace,
                    sampleUser,
                    sampleRole,
                    "Reloading page clears login state",
                    "Log in successfully. Immediately refresh the current page. Observe what happens.",
                    "Ubuntu 22.04, Firefox 123.",
                    "The current page refreshes without affecting the logged-in state.",
                    "The application forgets the session and requires login again.",
                    3,
                    4,
                    null,
                    null,
                    new HashSet<>(),
                    new HashSet<>()
                );
                //Send for detection
                if (shallSendToComputeEmbedding) reportContainerAsyncService.handleNewReport(newReport, sampleWorkspace, sampleUser);

                //Create report #3
                newReport = reportService.createReport(
                    sampleWorkspace,
                    sampleUser,
                    sampleRole,
                    "Browser refresh removes authentication",
                    "Access FictionalApp and log in. Press Ctrl+R to refresh. Watch where the app navigates.",
                    "Windows 10, Microsoft Edge 121.",
                    "User remains authenticated after refresh",
                    "Application redirects to login as if the session expired",
                    4,
                    3,
                    null,
                    null,
                    Set.of(
                        workspaceService.getValidWorkspaceCustomLabel(sampleWorkspace, "security")
                    ),
                    new HashSet<>()
                );
                //Send for detection
                if (shallSendToComputeEmbedding) reportContainerAsyncService.handleNewReport(newReport, sampleWorkspace, sampleUser);

                //Create report #4
                newReport = reportService.createReport(
                    sampleWorkspace,
                    sampleUser,
                    sampleRole,
                    "Notifications overlap content on small screens",
                    "Open FictionalApp on a laptop or resize the browser window to a narrow width. Open the notifications panel from the top bar.",
                    "macOS Sonoma, Google Chrome 122, 1280×800 resolution.",
                    "Notification panel should appear beside or above content without blocking it.",
                    "Notification panel overlaps the dashboard content, making buttons underneath unclickable.",
                    2,
                    1,
                    null,
                    null,
                    Set.of(
                        workspaceService.getValidWorkspaceCustomLabel(sampleWorkspace, "ui")
                    ),
                    new HashSet<>()
                );
                //Send for detection
                if (shallSendToComputeEmbedding) reportContainerAsyncService.handleNewReport(newReport, sampleWorkspace, sampleUser);

                //Create report #5
                newReport = reportService.createReport(
                    sampleWorkspace,
                    sampleUser,
                    sampleRole,
                    "Error when saving unchanged profile settings.",
                    "1. Log into FictionalApp. 2. Go to the profile settings page. 3. Click “Save Changes” without editing anything",
                    "Windows 11, Firefox 123",
                    "Either no action occurs or a message indicates no changes were made.",
                    "An error message appears stating that the profile could not be saved",
                    2,
                    4,
                    null,
                    null,
                    Set.of(
                        workspaceService.getValidWorkspaceCustomLabel(sampleWorkspace, "defect")
                    ),
                    new HashSet<>()
                );
                //Send for detection
                if (shallSendToComputeEmbedding) reportContainerAsyncService.handleNewReport(newReport, sampleWorkspace, sampleUser);

            } else {
                log.error("Unable to initialise additional sample bug reports as sample user or sample workspace is missing.");
            }
        } catch (Exception e) {
            log.error("Unable to initialise additional sample bug reports", e);
        }
    }

}
