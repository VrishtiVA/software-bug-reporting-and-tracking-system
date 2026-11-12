package com.example.backend.repositories.users.projections;

public interface UnreadNotificationCountProjection {
    String getUsername();
    Long getUnreadNotificationCount();
}
