package com.example.backend.model.bugs;

import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.WorkspaceRole;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.List;

@Entity
@Table(
    indexes = {
        //Improve query performance.
        @Index(columnList = "report_container_id")
    }
)
public class ReportContainerActivity {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column(nullable = false)
    private Instant timestamp;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "report_container_id", nullable = false)
    private ReportContainer reportContainer;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "json")
    private List<AuditToken> content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportContainerActivityType type;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportContainerActivityLogType logType;

    /* ---------- Optional fields depending on type ---------- */

    @ManyToOne(optional = true, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User performer;
    @Column(nullable = true)
    private WorkspaceRole performerRole;
    @Column(nullable = true)
    private String field;

    protected ReportContainerActivity() {}
    public ReportContainerActivity(
            ReportContainer reportContainer,
            List<AuditToken> content,
            ReportContainerActivityType type,
            ReportContainerActivityLogType logType,
            User performer,
            WorkspaceRole performerRole,
            String field
    ) {
        this.timestamp = Instant.now();
        this.reportContainer = reportContainer;
        this.content = content;
        this.type = type;
        this.logType = logType;
        this.performer = performer;
        this.performerRole = performerRole;
        this.field = field;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public Instant getTimestamp() {return timestamp;}
    public void setTimestamp(Instant timestamp) {this.timestamp = timestamp;}
    public ReportContainer getReportContainer() {return reportContainer;}
    public void setReportContainer(ReportContainer reportContainer) {this.reportContainer = reportContainer;}
    public ReportContainerActivityType getType() {return type;}
    public void setType(ReportContainerActivityType type) {this.type = type;}
    public User getPerformer() {return performer;}
    public void setPerformer(User performer) {this.performer = performer;}
    public WorkspaceRole getPerformerRole() {return performerRole;}
    public void setPerformerRole(WorkspaceRole performerRole) {this.performerRole = performerRole;}
    public ReportContainerActivityLogType getLogType() {return logType;}
    public void setLogType(ReportContainerActivityLogType logType) {this.logType = logType;}
    public List<AuditToken> getContent() {return content;}
    public void setContent(List<AuditToken> content) {this.content = content;}
    public String getField() {return field;}
    public void setField(String field) {this.field = field;}
}
