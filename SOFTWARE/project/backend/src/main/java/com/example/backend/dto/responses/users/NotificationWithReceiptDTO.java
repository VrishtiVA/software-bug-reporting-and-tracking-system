package com.example.backend.dto.responses.users;

import com.example.backend.model.users.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.List;

public record NotificationWithReceiptDTO(
    @JsonProperty("id") Long id,
    @JsonProperty("message") List<NotificationToken> message,
    @JsonProperty("timestamp") Instant timestamp,
    @JsonProperty("type") NotificationType type,
    @JsonProperty("type_string") String typeString,
    @JsonProperty("workspace_id") Long workspaceId,
    @JsonProperty("resource") ActionResource resource,
    @JsonProperty("resource_id") Long resourceId,
    @JsonProperty("is_read") Boolean isRead
) {
    public static NotificationWithReceiptDTO fromEntity(UserNotificationMapping mapping) {
        Notification notification = mapping.getNotification();
        return new NotificationWithReceiptDTO(
            notification.getId(),
            notification.getMessage(),
            notification.getTimestamp(),
            notification.getType(),
            NotificationDTO.convertNotificationTypeToString(notification.getType().toString()),
            notification.getWorkspaceId(),
            notification.getResource(),
            notification.getResourceId(),
            mapping.isRead()
        );
    }
}
