package com.example.backend.model.users;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
public class Notification {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<NotificationToken> message;
    @Column
    private Instant timestamp;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = true)
    private Long workspaceId;
    @Enumerated(EnumType.STRING)
    @Column(nullable = true)
    private ActionResource resource;
    @Column(nullable = true)
    private Long resourceId;

    protected Notification() {}
    public Notification(List<NotificationToken> message, NotificationType type, Long workspaceId, ActionResource resource, Long resourceId) {
        this.message = message;
        this.timestamp = Instant.now();
        this.type = type;
        this.workspaceId = workspaceId;
        this.resource = resource;
        this.resourceId = resourceId;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public List<NotificationToken> getMessage() {return message;}
    public void setMessage(List<NotificationToken> message) {this.message = message;}
    public Instant getTimestamp() {return timestamp;}
    public void setTimestamp(Instant timestamp) {this.timestamp = timestamp;}
    public Long getWorkspaceId() {return workspaceId;}
    public void setWorkspaceId(Long workspaceId) {this.workspaceId = workspaceId;}
    public ActionResource getResource() {return resource;}
    public void setResource(ActionResource resource) {this.resource = resource;}
    public Long getResourceId() {return resourceId;}
    public void setResourceId(Long resourceId) {this.resourceId = resourceId;}
    public NotificationType getType() {return type;}
    public void setType(NotificationType type) {this.type = type;}
}
