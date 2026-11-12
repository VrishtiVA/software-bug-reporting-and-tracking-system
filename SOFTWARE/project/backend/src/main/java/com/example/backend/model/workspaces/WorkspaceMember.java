package com.example.backend.model.workspaces;

import com.example.backend.model.users.User;
import jakarta.persistence.*;

@Entity
@Table(
    uniqueConstraints = {
        //A user should only appear as a workspace member once.
        @UniqueConstraint(columnNames = {"workspace_id", "user_id"})
    },
    indexes = { //Improve query performance
        @Index(columnList = "workspace_id")
    }
)
public class WorkspaceMember {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private long assignmentCount;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WorkspaceRole role;

    protected WorkspaceMember() {}
    public WorkspaceMember(Workspace workspace, User user, WorkspaceRole role) {
        this.workspace = workspace;
        this.user = user;
        this.role = role;
        this.assignmentCount = 0;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public Workspace getWorkspace() {return workspace;}
    public void setWorkspace(Workspace workspace) {this.workspace = workspace;}
    public User getUser() {return user;}
    public void setUser(User user) {this.user = user;}
    public WorkspaceRole getRole() {return role;}
    public void setRole(WorkspaceRole role) {this.role = role;}
    public long getAssignmentsCount() {return assignmentCount;}
    public void setAssignmentsCount(long assignmentsCount) {this.assignmentCount = assignmentsCount;}
}

