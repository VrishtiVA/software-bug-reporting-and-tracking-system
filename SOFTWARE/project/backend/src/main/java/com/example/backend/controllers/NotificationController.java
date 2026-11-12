package com.example.backend.controllers;

import com.example.backend.dto.requests.users.MarkNotificationsAsReadRequest;
import com.example.backend.dto.responses.ErrorResponse;
import com.example.backend.dto.responses.PaginatedResponse;
import com.example.backend.dto.responses.users.NotificationWithReceiptDTO;
import com.example.backend.model.users.User;
import com.example.backend.services.security.JwtService;
import com.example.backend.services.users.NotificationService;
import com.example.backend.services.users.UserService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
public class NotificationController {

    @Autowired
    private NotificationService notificationService;
    @Autowired
    private JwtService jwtService;
    @Autowired
    private UserService userService;

    private static final Logger log = LoggerFactory.getLogger(NotificationController.class);

    /**
     * Request method to establish a notification stream for
     * Server-sent events to be delivered while the connection is open.
     * @param request The request, which contains the JWT, that needs to be handled slightly differently here
     *                since Spring Security is configured for stateless authentication. Here, the JWT is validated
     *                to take the username of the authenticated user and create an emitter for them,
     *                establishing a secure connection.
     * @return The notification emitter event source.
     */
    @GetMapping(value = "/notifications/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream(HttpServletRequest request) {
        try {
            //Validate user authentication and find their username.
            String username = jwtService.tokenFilterLogic(request);
            if (username == null) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
            //Create new notification emitter to establish the connection.
            SseEmitter notificationEmitter = notificationService.createEmitter(username);
            //Fire last unread notification to the user
            notificationService.fireLastUnreadNotificationToUser(username);
            return notificationEmitter;
        } catch (Exception e) {
            log.warn("Failed in /notifications/stream. Issue: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Request method to get paginated notifications.
     * @param authentication The authenticated user.
     * @param page The page number
     * @param size The page size
     * @return The requested page of users notifications.
     */
    @GetMapping("/api/notifications")
    public ResponseEntity<?> getNotifications(
        Authentication authentication,
        @RequestParam(value = "page", defaultValue = "0") int page,
        @RequestParam(value = "size", defaultValue = "15") int size
    ) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            //Find page of notifications
            Pageable pageable = PageRequest.of(page, size, Sort.by("notification.timestamp").descending());
            Page<NotificationWithReceiptDTO> notificationsPage = notificationService.findPaginatedUserNotifications(currentUser, pageable);
            //Return paginated response
            return new ResponseEntity<>(PaginatedResponse.fromPage(notificationsPage), HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed in /notifications", e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

    /**
     * Request method for marking notifications as read, and fire back updated unread notifications data.
     * @param authentication The user authentication object.
     * @param requestBody The MarkNotificationsAsReadRequest to know which user notification to mark as read.
     * @see MarkNotificationsAsReadRequest
     */
    @PostMapping("/api/notifications/read")
    public ResponseEntity<?> markNotificationsAsRead(Authentication authentication, @RequestBody MarkNotificationsAsReadRequest requestBody) {
        try {
            //Ensure user exists
            User currentUser = userService.findUserByAuthentication(authentication);
            notificationService.markNotificationsAsRead(requestBody.notificationIds(), currentUser);
            //Send update on stream
            notificationService.sendToUsers(List.of(currentUser), null);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            log.error("Failed in /notifications/read", e);
            return ErrorResponse.SOMETHING_WENT_WRONG_SERVER_ERROR;
        }
    }

}
