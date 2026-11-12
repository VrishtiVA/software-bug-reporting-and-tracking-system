package com.example.backend.dto.responses.users;

import com.example.backend.model.users.ActionResource;
import com.example.backend.model.users.Notification;
import com.example.backend.model.users.NotificationToken;
import com.example.backend.model.users.NotificationType;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public record NotificationDTO(
        @JsonProperty("id") Long id,
        @JsonProperty("message") List<NotificationToken> message,
        @JsonProperty("timestamp") Instant timestamp,
        @JsonProperty("type") NotificationType type,
        @JsonProperty("type_string") String typeString,
        @JsonProperty("resource") ActionResource resource,
        @JsonProperty("resource_id") Long resourceId
) {
    public static String convertNotificationTypeToString(String notificationString) {
        return Arrays.stream(notificationString.split("_")).map(word -> word.charAt(0) + word.substring(1).toLowerCase()).collect(Collectors.joining(" "));
    }
    public static NotificationDTO fromEntity(Notification notification) {
        return new NotificationDTO(
            notification.getId(),
            notification.getMessage(),
            notification.getTimestamp(),
            notification.getType(),
            convertNotificationTypeToString(notification.getType().toString()),
            notification.getResource(),
            notification.getResourceId()
        );
    }
}
