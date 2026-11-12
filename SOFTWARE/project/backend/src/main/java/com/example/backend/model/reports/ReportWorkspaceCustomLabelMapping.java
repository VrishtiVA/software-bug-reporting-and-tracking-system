package com.example.backend.model.reports;

import com.example.backend.model.workspaces.WorkspaceCustomLabel;
import jakarta.persistence.*;

/**
 * A similar mapping entity just for individual reports.
 * May be useful for automatically applying labels when grouping reports.
 */
@Entity
public class ReportWorkspaceCustomLabelMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_id", nullable = false)
    private Report report;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_custom_label_id", nullable = false)
    private WorkspaceCustomLabel workspaceCustomLabel;

    protected ReportWorkspaceCustomLabelMapping() {}
    public ReportWorkspaceCustomLabelMapping(Report report, WorkspaceCustomLabel workspaceCustomLabel) {
        this.report = report;
        this.workspaceCustomLabel = workspaceCustomLabel;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public Report getReport() {return report;}
    public void setReport(Report report) {this.report = report;}
    public WorkspaceCustomLabel getWorkspaceCustomLabel() {return workspaceCustomLabel;}
    public void setWorkspaceCustomLabel(WorkspaceCustomLabel workspaceCustomLabel) {this.workspaceCustomLabel = workspaceCustomLabel;}

}