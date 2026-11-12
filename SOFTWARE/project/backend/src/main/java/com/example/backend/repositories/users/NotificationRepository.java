package com.example.backend.repositories.users;

import com.example.backend.model.users.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    @Query("""
        SELECT unm.notification
        FROM UserNotificationMapping unm
        WHERE unm.user.id = :user_id AND unm.read = false
        ORDER BY unm.notification.timestamp DESC
        LIMIT 1
    """)
    Optional<Notification> findLastUnreadNotificationByUserId(@Param("user_id") Long userId);

}
