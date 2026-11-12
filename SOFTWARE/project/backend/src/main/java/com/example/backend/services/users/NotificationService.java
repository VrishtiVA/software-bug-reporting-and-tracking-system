package com.example.backend.services.users;

import com.example.backend.dto.responses.users.NotificationDTO;
import com.example.backend.dto.responses.users.NotificationResponse;
import com.example.backend.dto.responses.users.NotificationWithReceiptDTO;
import com.example.backend.model.bugs.ReportContainerStability;
import com.example.backend.model.users.*;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceRole;
import com.example.backend.repositories.users.NotificationRepository;
import com.example.backend.repositories.users.projections.UnreadNotificationCountProjection;
import com.example.backend.repositories.users.UserNotificationMappingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    @Autowired
    private NotificationRepository notificationRepository;
    @Autowired
    private UserNotificationMappingRepository userNotificationMappingRepository;
    @Autowired
    private UserService userService;

    /**
     * Async-safe username to notification emitter hashmap.
     * Multiple emitters per user in case of multiple tabs.
     */
    public static final Map<String, List<SseEmitter>> notificationEmitters = new ConcurrentHashMap<>();

    /**
     * Method to find page of user notifications
     * @param user The user to find notifications for
     * @param pageable The pagination configuration.
     * @return The page of user notifications in NotificationWithReceiptDTO format
     * @see NotificationWithReceiptDTO
     */
    public Page<NotificationWithReceiptDTO> findPaginatedUserNotifications(User user, Pageable pageable) {
        Page<UserNotificationMapping> mappingsPage = userNotificationMappingRepository.findByUserId(user.getId(), pageable);
        return mappingsPage.map(NotificationWithReceiptDTO::fromEntity);
    }

    /* ---------- Notification Storage and Delivery Handlers ---------- */

    /**
     * Method to create a new notification emitter for a user.
     * @param username The user to create for.
     * @return The newly created notification emitter.
     */
    public SseEmitter createEmitter(String username) {
        SseEmitter notificationEmitter = new SseEmitter(Long.MAX_VALUE);
        //Thread-safe array list used, to allow for mutability with eventually consistent read operations.
        notificationEmitters.computeIfAbsent(username, f -> new CopyOnWriteArrayList<>()).add(notificationEmitter);

        //Listeners to ensure it is removed when appropriate.
        notificationEmitter.onCompletion(() -> removeNotificationEmitter(username, notificationEmitter));
        notificationEmitter.onTimeout(() -> removeNotificationEmitter(username, notificationEmitter));
        notificationEmitter.onError((e) -> removeNotificationEmitter(username, notificationEmitter));

        return notificationEmitter;
    }

    /** Utility method to remove emitter from list of user emitters. */
    private void removeNotificationEmitter(String username, SseEmitter notificationEmitter) {
        List<SseEmitter> userEmitters = notificationEmitters.get(username);
        if (userEmitters != null) userEmitters.remove(notificationEmitter);
    }

    /**
     * Send a heartbeat to signed-in users every 25 seconds
     * to prevent the browser from silently closing the notification sse connection stream.
     */
    @Scheduled(fixedRate = 25000) //
    public void scheduledNotificationStreamHeartbeats() {
        notificationEmitters.forEach((username, userEmitters) -> {
            for (SseEmitter notificationEmitter : userEmitters) {
                try {
                    notificationEmitter.send(SseEmitter.event()
                        .name("heartbeat")
                        .data("SSE Heartbeat")
                    );
                } catch (Exception e) {
                    log.warn("Cannot return heartbeat to user: {}, connection must have closed.", username);
                    userEmitters.remove(notificationEmitter);
                }
            }

        });
    }

    /**
     * Async method to send initial unread notification details to user on first load.
     * @param username The user to find unread notification details for.
     */
    @Async
    public void fireLastUnreadNotificationToUser(String username) {
        try {
            //Wait for higher chance that connection is set up before first fire, so that some initial state can be received.
            Thread.sleep(1500);
        } catch (InterruptedException e) {
            log.warn("Wait failed in fireLastUnreadNotificationToUser, proceeding", e);
        }
        User user = userService.findUserByUsername(username);
        if (user == null) return;

        //Find unread notification count
        Long unreadNotificationCount = userNotificationMappingRepository.countUnreadNotificationsByUserId(user.getId());
        if (unreadNotificationCount > 0) {
            //If user has unread notifications, send them the last one.
            notificationRepository.findLastUnreadNotificationByUserId(user.getId())
                .ifPresent(notification -> sendToUser(user, notification, unreadNotificationCount));
        }
    }

    /**
     * Method to fetch unread notification count for a user by their username directly.
     * @param username The username of the user to fetch unread notification count for.
     * @return The unread notification count.
     */
    public Long getUnreadNotificationCountByUsername(String username) {
        return userNotificationMappingRepository.countUnreadNotificationsByUsername(username);
    }

    public List<Long> findUnreadNotificationIdsByUsername(String username) {
        return userNotificationMappingRepository.findUnreadNotificationIdsByUserId(username);
    }

    /**
     * Method to post a notification to a user.
     * @param user The user.
     * @param notification The notification.
     * @param unreadNotificationCount The users new unread notification count.
     */
    public void sendToUser(User user, Notification notification, Long unreadNotificationCount) {
        String username = user.getUsername();
        List<SseEmitter> userEmitters = notificationEmitters.get(username);
        if (userEmitters == null) return;

        //If user is signed in, send them the notification.
        for (SseEmitter notificationEmitter : userEmitters) {
            try {
                notificationEmitter.send(SseEmitter.event()
                    .name("notification")
                    .data(NotificationResponse.fromData(
                        unreadNotificationCount,
                        notification == null ? null : NotificationDTO.fromEntity(notification)
                    )
                ));
            } catch (Exception e) {
                userEmitters.remove(notificationEmitter);
            }
        }
    }

    /**
     * Single async thread to handle sending a certain notification to multiple users.
     * @param users The list of users to send the notification to.
     * @param notification The notification to send.
     */
    @Async
    public void sendToUsers(List<User> users, Notification notification) {

        //Fetch unread notification count for each user, and send to along too.
        List<UnreadNotificationCountProjection> unreadNotificationCountProjections = userNotificationMappingRepository.findUnreadNotificationCountByUserIds(
            users.stream().map(User::getId).toList()
        );
        Map<String, Long> unreadNotificationCountMap = new HashMap<>();
        for (UnreadNotificationCountProjection uncp : unreadNotificationCountProjections) {
            unreadNotificationCountMap.put(uncp.getUsername(), uncp.getUnreadNotificationCount());
        }
        for (User user : users) {
            sendToUser(user, notification, unreadNotificationCountMap.get(user.getUsername()));
        }
    }

    /** Method to save and send a new notification to users. */
    @Transactional
    public void massSaveAndSendToUser(List<User> users, Notification notification) {
        //Save notification
        notification = notificationRepository.save(notification);
        //Save mappings
        List<UserNotificationMapping> mappings = new ArrayList<>();
        for (User user : users) {
            UserNotificationMapping mapping = new UserNotificationMapping(notification, user);
            mappings.add(mapping);
        }
        userNotificationMappingRepository.saveAll(mappings);
        //Send notification to users
        sendToUsers(users, notification);
    }

    /** Method to create a new notification */
    @Transactional
    public Notification createNotification(
        List<NotificationToken> message,
        NotificationType type,
        Long workspaceId,
        ActionResource resource,
        Long resourceId
    ) {
        Notification newNotification = new Notification(
            message,
            type,
            workspaceId,
            resource,
            resourceId
        );
        newNotification = notificationRepository.save(newNotification);
        return newNotification;
    }

    /** Method to mark a notification as read. */
    @Transactional
    public void markNotificationsAsRead(List<Long> notificationIds, User user) {
        userNotificationMappingRepository.markNotificationsAsRead(notificationIds, user.getId());
    }

    /* ---------- Send Different Notifications ---------- */

    @Transactional
    public void sendDummyNotification(User user) {
        Notification notification = createNotification(
            new NotificationMessageBuilder().text("This is a")
                .highlight("dummy")
                .text("notification")
                .build(),
            NotificationType.ADDED_TO_WORKSPACE,
            null, null, null
        );
        massSaveAndSendToUser(List.of(user), notification);
    }

    @Transactional
    public void sendAddedToWorkspaceNotification(User user, WorkspaceRole role, Workspace workspace) {
        String rolePresentable = role.toString().charAt(0) + role.toString().substring(1).toLowerCase();
        Notification notification = createNotification(
                new NotificationMessageBuilder()
                        .text("You were added to workspace")
                        .highlight(workspace.getName())
                        .text("as a")
                        .highlight(rolePresentable)
                        .build(),
                NotificationType.ADDED_TO_WORKSPACE,
                workspace.getId(),
                ActionResource.WORKSPACE,
                workspace.getId()
        );
        massSaveAndSendToUser(List.of(user), notification);
    }

    @Transactional
    public void sendMassAddedToWorkspaceNotification(List<User> users, Workspace workspace) {
        Notification notification = createNotification(
            new NotificationMessageBuilder()
                .text("You were added to a new workspace")
                .highlight(workspace.getName())
                .build(),
            NotificationType.ADDED_TO_WORKSPACE,
            workspace.getId(),
            ActionResource.WORKSPACE,
            workspace.getId()
        );
        massSaveAndSendToUser(users, notification);
    }

    @Transactional
    public void sendRemovedFromWorkspaceNotification(User user, Workspace workspace) {
        Notification notification = createNotification(
            new NotificationMessageBuilder()
                .text("You were removed from workspace")
                .highlight(workspace.getName())
                .build(),
            NotificationType.REMOVED_FROM_WORKSPACE,
            null, null, null
        );
        massSaveAndSendToUser(List.of(user), notification);
    }

    @Transactional
    public void sendMassWorkspaceRenamedNotification(List<User> users, String oldName, Workspace workspace) {
        Notification notification = createNotification(
            new NotificationMessageBuilder()
                .text("Workspace")
                .highlight(oldName)
                .text("was renamed to")
                .highlight(workspace.getName())
                .build(),
            NotificationType.WORKSPACE_UPDATE,
            workspace.getId(),
            ActionResource.WORKSPACE,
            workspace.getId()
        );
        massSaveAndSendToUser(users, notification);
    }

    @Transactional
    public void sendMassWorkspaceDeletedNotification(List<User> users, String workspaceName) {
        Notification notification = createNotification(
            new NotificationMessageBuilder()
                .text("Workspace")
                .highlight(workspaceName)
                .text("was removed from existance")
                .build(),
            NotificationType.WORKSPACE_DELETED,
            null, null, null
        );
        massSaveAndSendToUser(users, notification);
    }

    @Transactional
    public void sendMassAssignedToBugNotification(List<User> users, Long reportContainerId, Workspace workspace) {
        Notification notification = createNotification(
            new NotificationMessageBuilder()
                .text("You were assigned to")
                .highlight("BUG ID: " + reportContainerId)
                .text("in workspace")
                .highlight(workspace.getName())
                .build(),
            NotificationType.ASSIGNED_TO_BUG,
            workspace.getId(),
            ActionResource.BUG,
            reportContainerId
        );
        massSaveAndSendToUser(users, notification);
    }

    @Transactional
    public void sendMassUnassignedFromBugNotification(List<User> users, Long reportContainerId, Workspace workspace) {
        Notification notification = createNotification(
            new NotificationMessageBuilder()
                .text("You were unassigned from")
                .highlight("BUG ID: " + reportContainerId)
                .text("in workspace")
                .highlight(workspace.getName())
                .build(),
            NotificationType.UNASSIGNED_FROM_BUG,
            null, null, null
        );
        massSaveAndSendToUser(users, notification);
    }

    @Transactional
    public void sendMassBugSummaryUpdateNotification(List<User> users, Long reportContainerId, Workspace workspace, String oldSummaryTitle, String newSummaryTitle) {
        Notification notification = createNotification(
            new NotificationMessageBuilder()
                .text("The summary title of your assigned bug")
                .highlight("BUG ID: "+reportContainerId)
                .text("was updated from")
                .highlight(oldSummaryTitle)
                .text("to")
                .highlight(newSummaryTitle)
                .build(),
            NotificationType.BUG_UPDATE,
            workspace.getId(),
            ActionResource.BUG,
            reportContainerId
        );
        massSaveAndSendToUser(users, notification);
    }

    @Transactional
    public void sendMassBugProgressionUpdateNotification(List<User> users, Long reportContainerId, Workspace workspace, String field, String oldValue, String newValue) {
        oldValue = oldValue.replace("_", " ");
        newValue = newValue.replace("_", " ");
        Notification notification = createNotification(
            new NotificationMessageBuilder()
                .text("The "+ field +" of your assigned bug")
                .highlight("BUG ID: "+reportContainerId)
                .text("was updated from")
                .highlight(oldValue)
                .text("to")
                .highlight(newValue)
                .build(),
            NotificationType.BUG_UPDATE,
            workspace.getId(),
            ActionResource.BUG,
            reportContainerId
        );
        massSaveAndSendToUser(users, notification);
    }

    @Transactional
    public void sendMassNewDuplicatesAddedNotification(List<User> users, Long reportContainerId, Workspace workspace) {
        Notification notification = createNotification(
            new NotificationMessageBuilder()
                .text("There are new duplicate reports available for your assigned bug")
                .highlight("BUG ID: " + reportContainerId)
                .build(),
            NotificationType.NEW_DUPLICATE_REPORTS,
            workspace.getId(),
            ActionResource.BUG,
            reportContainerId
        );
        massSaveAndSendToUser(users, notification);
    }

    @Transactional
    public void sendMassNewRelevantDuplicateSuggestionsNotification(List<User> users, Long reportContainerId, Workspace workspace) {
        Notification notification = createNotification(
            new NotificationMessageBuilder()
                .text("There are new duplicate suggestions available in workspace")
                .highlight(workspace.getName())
                .text("that are relevant to your assigned bug")
                .highlight("BUG ID: " + reportContainerId)
                .build(),
            NotificationType.NEW_DUPLICATE_SUGGESTIONS,
            workspace.getId(),
            ActionResource.BUG,
            reportContainerId
        );
        massSaveAndSendToUser(users, notification);
    }

    @Transactional
    public void sendReportDiscoveredAsDuplicateNotification(User user, Long reportContainerId, Workspace workspace, Long reportId, ReportContainerStability stability) {
        Notification notification = createNotification(
            (stability == ReportContainerStability.STABLE ?
                new NotificationMessageBuilder()
                    .text("Your bug report")
                    .highlight("REPORT ID: " + reportId)
                    .text("was discovered to be a duplicate of")
                    .highlight("BUG ID: " + reportContainerId)
                    .build()
                :
                new NotificationMessageBuilder()
                    .text("Your bug report")
                    .highlight("REPORT ID: " + reportId)
                    .text("was discovered as a duplicate report")
                    .build()
            ),
            NotificationType.REPORT_DISCOVERED_AS_DUPLICATE,
            workspace.getId(),
            ActionResource.BUG,
            stability == ReportContainerStability.STABLE ? reportContainerId : null
        );
        massSaveAndSendToUser(List.of(user), notification);
    }

}
