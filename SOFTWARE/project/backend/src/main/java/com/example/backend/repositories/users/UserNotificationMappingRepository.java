package com.example.backend.repositories.users;

import com.example.backend.model.users.UserNotificationMapping;
import com.example.backend.repositories.users.projections.UnreadNotificationCountProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserNotificationMappingRepository extends JpaRepository<UserNotificationMapping, Long> {

    @Query("""
        SELECT unm
        FROM UserNotificationMapping unm
        JOIN FETCH unm.notification
        WHERE unm.user.id = :user_id
    """)
    Page<UserNotificationMapping> findByUserId(@Param("user_id") Long userId, Pageable pageable);

    /**
     * Method to mark user notifications as read
     * @param notificationIds A list of notification IDs to mark as read for the user.
     * @param userId The ID of the user to mark notifications as read for.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        UPDATE UserNotificationMapping unm
        SET unm.read = true
        WHERE
            unm.user.id = :user_id AND
            unm.notification.id IN (:notification_ids)
    """)
    void markNotificationsAsRead(@Param("notification_ids") List<Long> notificationIds, @Param("user_id") Long userId);

    /**
     * Find unread notifications count in bulk.
     * @param userIds The IDs of the users to find this for.
     * @return A list of UnreadNotificationCountProjections containing the username and count,
     *          that can be mapped in the service layer.
     */
    @Query("""
        SELECT unm.user.username as username, COUNT(unm.notification.id) as unreadNotificationCount
        FROM UserNotificationMapping unm
        WHERE unm.read = false AND unm.user.id IN (:user_ids)
        GROUP BY username
    """)
    List<UnreadNotificationCountProjection> findUnreadNotificationCountByUserIds(@Param("user_ids") List<Long> userIds);

    @Query("""
        SELECT COUNT(unm.id)
        FROM UserNotificationMapping unm
        WHERE unm.user.id = :user_id AND unm.read = false
    """)
    Long countUnreadNotificationsByUserId(@Param("user_id") Long userId);

    @Query("""
        SELECT COUNT(unm.id)
        FROM UserNotificationMapping unm
        WHERE unm.user.username = :username AND unm.read = false
    """)
    Long countUnreadNotificationsByUsername(@Param("username") String username);

    @Query("""
        SELECT unm.id
        FROM UserNotificationMapping unm
        WHERE unm.user.username = :username AND unm.read = false
    """)
    List<Long> findUnreadNotificationIdsByUserId(@Param("username") String username);

}
