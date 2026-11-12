package com.example.backend.dto.responses.users;

import com.fasterxml.jackson.annotation.JsonProperty;

public record NotificationResponse(
        @JsonProperty("unread_count") Long unreadCount,
        @JsonProperty("notification") NotificationDTO notification
) {
    public static NotificationResponse fromData(Long unreadCount, NotificationDTO notification) {
        return new NotificationResponse(unreadCount, notification);
    }
}
