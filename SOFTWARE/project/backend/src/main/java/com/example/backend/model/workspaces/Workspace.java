package com.example.backend.model.workspaces;

import com.example.backend.model.users.User;
import jakarta.persistence.*;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(
    name = "workspace",
    indexes = { //Improve query performance
        @Index(columnList = "owner_id")
    }
)
public class Workspace {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String name;
    @Column(columnDefinition = "TEXT")
    private String description;
    @Column(nullable = false)
    private boolean isPublic;
    @Column
    private String colour;
    @Column(nullable = false)
    private Instant creationTimestamp;

    //Consider, what if the owner deletes their account, also remove workspace?
    @ManyToOne
    @JoinColumn(name = "owner_id")
    private User owner;

    //If delete workspace, also remove workspace member objects.
    @OneToMany(mappedBy = "workspace", fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    private Set<WorkspaceMember> workspaceMembers = new HashSet<>();

    //If delete a workspace, also remove all workspace custom label objects.
    @OneToMany(fetch = FetchType.LAZY, orphanRemoval = true, cascade = CascadeType.ALL)
    @JoinColumn(name = "workspace_id")
    private Set<WorkspaceCustomLabel> workspaceCustomLabels = new HashSet<>();

    protected Workspace() {}
    public Workspace(String name, String description, boolean isPublic, String colour, User owner) {
        this.name = name;
        this.description = description;
        this.isPublic = isPublic;
        this.colour = colour;
        this.creationTimestamp = Instant.now();
        this.owner = owner;
        //Add workspace owner as a workspace member.
        this.workspaceMembers.add(new WorkspaceMember(this, owner, WorkspaceRole.OWNER));
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getDescription() {return description;}
    public void setDescription(String description) {this.description = description;}
    public boolean isPublic() {return isPublic;}
    public void setPublic(boolean aPublic) {isPublic = aPublic;}
    public String getColour() {return colour;}
    public void setColour(String colour) {this.colour = colour;}
    public Instant getCreationTimestamp() {return creationTimestamp;}
    public void setCreationTimestamp(Instant creationTimestamp) {this.creationTimestamp = creationTimestamp;}
    public User getOwner() {return owner;}
    public void setOwner(User owner) {this.owner = owner;}
    public Set<WorkspaceMember> getWorkspaceMembers() {return workspaceMembers;}
    public void setWorkspaceMembers(HashSet<WorkspaceMember> workspaceMembers) {this.workspaceMembers = workspaceMembers;}
    public Set<WorkspaceCustomLabel> getWorkspaceCustomLabels() {return workspaceCustomLabels;}
    public void setWorkspaceCustomLabels(Set<WorkspaceCustomLabel> workspaceCustomLabels) {this.workspaceCustomLabels = workspaceCustomLabels;}
}
