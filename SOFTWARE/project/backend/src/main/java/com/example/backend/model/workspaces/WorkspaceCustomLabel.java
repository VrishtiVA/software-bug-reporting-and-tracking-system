package com.example.backend.model.workspaces;

import jakarta.persistence.*;

@Entity
@Table(
    uniqueConstraints = {
        //The name of a custom label should be unique in a workspace.
        @UniqueConstraint(columnNames = {"name", "workspace_id"})
    }
)
public class WorkspaceCustomLabel {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;
    @Column(nullable = false)
    private String name;
    @Column(nullable = false)
    private String colour;

    protected WorkspaceCustomLabel() {}
    public WorkspaceCustomLabel(String name, String colour) {
        this.name = name;
        this.colour = colour;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public String getName() {return name;}
    public void setName(String name) {this.name = name;}
    public String getColour() {return colour;}
    public void setColour(String colour) {this.colour = colour;}
}
