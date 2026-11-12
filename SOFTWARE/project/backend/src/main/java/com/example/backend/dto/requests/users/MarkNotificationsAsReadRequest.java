package com.example.backend.dto.requests.users;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record MarkNotificationsAsReadRequest(
    @JsonProperty("notification_ids") List<Long> notificationIds
) {}
