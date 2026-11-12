package com.example.backend;

import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.reports.FileType;
import com.example.backend.model.reports.Report;
import com.example.backend.model.reports.ReportAttachment;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceMember;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.repositories.users.UserRepository;
import com.example.backend.services.bugs.ReportContainerAsyncService;
import com.example.backend.services.bugs.ReportContainerService;
import com.example.backend.services.reports.ReportService;
import com.example.backend.services.users.NotificationService;
import com.example.backend.services.users.UserService;
import com.example.backend.services.workspaces.WorkspaceService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TestSampleInitialiserService {

    private static final Logger log = LoggerFactory.getLogger(TestSampleInitialiserService.class);

    private final PasswordEncoder passwordEncoder;
    private final WorkspaceService workspaceService;
    private final ReportContainerService reportContainerService;
    private final UserService userService;
    private final NotificationService notificationService;
    private final ReportService reportService;
    private final ReportContainerAsyncService reportContainerAsyncService;
    private final UserRepository userRepository;

    public TestSampleInitialiserService(
        PasswordEncoder passwordEncoder,
        WorkspaceService workspaceService,
        ReportContainerService reportContainerService,
        UserService userService,
        NotificationService notificationService,
        ReportService reportService,
        ReportContainerAsyncService reportContainerAsyncService,
        UserRepository userRepository
    ) {
        this.passwordEncoder = passwordEncoder;
        this.workspaceService = workspaceService;
        this.reportContainerService = reportContainerService;
        this.userService = userService;
        this.notificationService = notificationService;
        this.reportService = reportService;
        this.reportContainerAsyncService = reportContainerAsyncService;
        this.userRepository = userRepository;
    }

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
                "MyBugspace",
                "A very lovely description",
                true,
                "sky",
                members,
                Map.of("happy", "sky", "organic", "lime")
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

    @Transactional
    public void initialiseMaximumSampleWorkspacesForUser() {
        User sampleUser = userRepository.findByUsername("demonstrator3").orElse(null);
        if (sampleUser != null) {
            workspaceService.createWorkspace(sampleUser, "WS1", null, false, "red", null, null);
            workspaceService.createWorkspace(sampleUser, "WS2", null, false, "orange", null, null);
            workspaceService.createWorkspace(sampleUser, "WS3", null, false, "yellow", null, null);
            workspaceService.createWorkspace(sampleUser, "WS4", null, false, "lime", null, null);
            workspaceService.createWorkspace(sampleUser, "WS5", null, false, "green", null, null);
        } else {
            log.warn("Could not initialise maximum sample workspaces for user as sample user not found.");
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
                "This is the summary of the bug",
                "To find this bug, first do this, then that.",
                "On my laptop, using Google Chrome browser.",
                "I thought a bug would not occur.",
                "A bug occurred",
                3,
                2,
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
                    "This is the summary of a bug",
                    "1. do this, 2. then that.",
                    "PC, Google",
                    "Didn't think anything would happen.",
                    "Something went wrong",
                    2,
                    3,
                    null,
                    null,
                    Set.of(
                        workspaceService.getValidWorkspaceCustomLabel(sampleWorkspace, "happy"),
                        workspaceService.getValidWorkspaceCustomLabel(sampleWorkspace, "organic")
                    ),
                    Set.of(
                        new ReportAttachment(
                            "This is an image",
                            "1771619548511-d1428018-0724-48b8-821b-90c482544c26.png",
                            "1771619548611-83c1c5ac-fab4-416f-b758-9fcc2533c982.jpg",
                            FileType.IMAGE,
                            1000L
                        ),
                        new ReportAttachment(
                            "This is a video",
                            "1771698925403-9c19f6af-45a4-472a-98dc-a993991c5829.mp4",
                            "1771698925439-1f3e1eb6-7d15-4f1f-85bc-dfbfc2b6b109.jpg",
                            FileType.VIDEO,
                            10000L
                        )
                    )
                );
                //Send for detection
                if (shallSendToComputeEmbedding) reportContainerAsyncService.handleNewReport(newReport, sampleWorkspace, sampleUser);

                //Create report #2
                newReport = reportService.createReport(
                    sampleWorkspace,
                    sampleUser,
                    sampleRole,
                    "This is the summary of the bug again",
                    "To find this bug, first do this, then that.",
                    "Google.",
                    "Nothing.",
                    "This bug appeared.",
                    3,
                    2,
                    null,
                    null,
                    new HashSet<>(),
                    Set.of(
                        new ReportAttachment(
                            "This is an image",
                            "1771619548520-e827f83e-5f51-4d32-a670-41520a744faf.png",
                            "1771619552849-51bbb49f-8b5f-4adc-91e9-95e3264e2de5.jpg",
                            FileType.IMAGE,
                            1000L
                        ),
                        new ReportAttachment(
                            "This is an image",
                            "1771619798261-d2a8c216-4090-4971-9c73-6c76653a4d2d.png",
                            "1771619798326-726d7e07-b1bc-470f-9694-7c2934a7efd5.jpg",
                            FileType.IMAGE,
                            1000L
                        ),
                        new ReportAttachment(
                            "This is an image",
                            "1771619798263-ec820835-fbdf-4947-8579-26be5e16c7f8.png",
                            "1771619798498-2e9fe8df-3a27-48b5-b94b-35e6d89daedb.jpg",
                            FileType.IMAGE,
                            1000L
                        )
                    )
                );
                //Send for detection
                if (shallSendToComputeEmbedding) reportContainerAsyncService.handleNewReport(newReport, sampleWorkspace, sampleUser);

                //Create report #3
                newReport = reportService.createReport(
                    sampleWorkspace,
                    sampleUser,
                    sampleRole,
                    "This is the summary of the bug",
                    "this, then that.",
                    "Laptop, Google.",
                    "No bug.",
                    "A bug",
                    3,
                    2,
                    null,
                    null,
                    new HashSet<>(),
                    Set.of(
                        new ReportAttachment(
                            "This is an image",
                            "1771619982196-7b24c7ad-ee74-4642-9fae-8d83be8f2276.png",
                            "1771619982238-51736378-5e53-404b-be99-88d1bab5fef0.jpg",
                            FileType.IMAGE,
                            1000L
                        ),
                        new ReportAttachment(
                            "This is an image",
                            "1771619982197-7b4594c4-c655-4be3-b803-d6688c0b7fa8.png",
                            "1771619982472-49f62647-046d-40c0-a195-13bda049511c.jpg",
                            FileType.IMAGE,
                            1000L
                        ),
                        new ReportAttachment(
                            "This is an image",
                            "1771619982202-9a9db4fb-fab7-4cd4-a5d1-6c07e2ae46e9.png",
                            "1771619983561-b48cb16e-350c-42e1-8504-ae94a117ff53.jpg",
                            FileType.IMAGE,
                            1000L
                        )
                    )
                );
                //Send for detection
                if (shallSendToComputeEmbedding) reportContainerAsyncService.handleNewReport(newReport, sampleWorkspace, sampleUser);

                //Create report #4
                newReport = reportService.createReport(
                    sampleWorkspace,
                    sampleUser,
                    sampleRole,
                    "This is the summary of the bug",
                    "this, then that, and then another thing.",
                    "Laptop, Google.",
                    "There should not have been any bug.",
                    "A bug happened.",
                    2,
                    3,
                    null,
                    null,
                    new HashSet<>(),
                    new HashSet<>()
                );
                //Send for detection
                if (shallSendToComputeEmbedding) reportContainerAsyncService.handleNewReport(newReport, sampleWorkspace, sampleUser);

                //Create report #5
                newReport = reportService.createReport(
                    sampleWorkspace,
                    sampleUser,
                    sampleRole,
                    "This is another bug",
                    "1. do this, 2. then that. that's all.",
                    "PC, Google",
                    "Didn't think anything would happen.",
                    "Something went wrong here",
                    2,
                    3,
                    null,
                    null,
                    new HashSet<>(),
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

    /* ---------- Assignments ---------- */

    /**
     * Method to initialise sample assignments to a bug.
     * This method should only be used as a utility for test cases.
     * @param bugId The bug ID to assign users to.
     * @param userIds The users to assign
     */
    @Transactional
    public void assignSampleUsers(Long workspaceId, Long bugId, List<Long> userIds) {
        for (Long userId : userIds) {
            WorkspaceMember member = workspaceService.findWorkspaceMemberInWorkspaceByUserId(
                workspaceService.findWorkspaceById(workspaceId).orElse(null),
                userId
            );
            ReportContainer bug = reportContainerService.findReportContainerById(bugId);
            reportContainerService.assignWorkspaceMemberToBug(bug, member);
        }
    }

}
