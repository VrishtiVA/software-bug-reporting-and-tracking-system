package com.example.backend.model.users;

import jakarta.persistence.*;

@Entity
@Table(
    uniqueConstraints = {
        //A specific notification object should only be sent to the user once.
        @UniqueConstraint(columnNames = {"notification_id", "user_id"})
    },
    indexes = {
        //Improve query performance when looking for users notifications.
        @Index(columnList = "user_id")
    }
)
public class UserNotificationMapping {

    //Surrogate primary key easier to handle with Spring JPA
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private Notification notification;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /** Had to alter database level name for this field, to prevent keyword usage error. */
    @Column(name = "is_read")
    private boolean read;

    protected UserNotificationMapping() {}
    public UserNotificationMapping(Notification notification, User user) {
        this.notification = notification;
        this.user = user;
        this.read = false;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public Notification getNotification() {return notification;}
    public void setNotification(Notification notification) {this.notification = notification;}
    public User getUser() {return user;}
    public void setUser(User user) {this.user = user;}
    public boolean isRead() {return read;}
    public void setRead(boolean read) {this.read = read;}
}
