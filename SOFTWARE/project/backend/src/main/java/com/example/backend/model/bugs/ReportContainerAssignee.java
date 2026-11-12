package com.example.backend.model.bugs;

import com.example.backend.model.workspaces.WorkspaceMember;
import jakarta.persistence.*;

@Entity
@Table(
    uniqueConstraints = {
        //A specific workspace member should only have one assignment instance with a specific report container.
        @UniqueConstraint(columnNames = {"workspace_member_id", "report_container_id"})
    },
    indexes = {
        //Improve query performance when looking for assignments.
        @Index(columnList = "workspace_member_id"),
        @Index(columnList = "report_container_id")
    }
)
public class ReportContainerAssignee {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_container_id", nullable = false)
    private ReportContainer reportContainer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_member_id", nullable = false)
    private WorkspaceMember workspaceMember;

    protected ReportContainerAssignee() {}
    public ReportContainerAssignee(ReportContainer reportContainer, WorkspaceMember workspaceMember) {
        this.reportContainer = reportContainer;
        this.workspaceMember = workspaceMember;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public ReportContainer getReportContainer() {return reportContainer;}
    public void setReportContainer(ReportContainer reportContainer) {this.reportContainer = reportContainer;}
    public WorkspaceMember getWorkspaceMember() {return workspaceMember;}
    public void setWorkspaceMember(WorkspaceMember workspaceMember) {this.workspaceMember = workspaceMember;}
}
