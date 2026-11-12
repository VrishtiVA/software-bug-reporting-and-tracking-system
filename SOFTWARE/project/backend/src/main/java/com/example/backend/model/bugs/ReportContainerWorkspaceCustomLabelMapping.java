package com.example.backend.model.bugs;

import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import jakarta.persistence.*;

/**
 * When attaching a custom label to a bug report,
 * this entity can be used to join the custom label to the report container.
 * If a custom label is removed from the workspace, it should also be removed from the reports containers collection of custom labels.
 * This join table is necessary, to allow easy updates e.g. when custom labels are removed.
 */
@Entity
public class ReportContainerWorkspaceCustomLabelMapping {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_container_id", nullable = false)
    private ReportContainer reportContainer;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_custom_label_id", nullable = false)
    private WorkspaceCustomLabel workspaceCustomLabel;

    protected ReportContainerWorkspaceCustomLabelMapping() {}
    public ReportContainerWorkspaceCustomLabelMapping(ReportContainer reportContainer, WorkspaceCustomLabel workspaceCustomLabel) {
        this.reportContainer = reportContainer;
        this.workspaceCustomLabel = workspaceCustomLabel;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public ReportContainer getReportContainer() {return reportContainer;}
    public void setReportContainer(ReportContainer reportContainer) {this.reportContainer = reportContainer;}
    public WorkspaceCustomLabel getWorkspaceCustomLabel() {return workspaceCustomLabel;}
    public void setWorkspaceCustomLabel(WorkspaceCustomLabel workspaceCustomLabel) {this.workspaceCustomLabel = workspaceCustomLabel;}

}
