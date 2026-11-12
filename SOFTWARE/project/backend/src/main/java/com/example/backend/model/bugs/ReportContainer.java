package com.example.backend.model.bugs;

import com.example.backend.model.reports.Report;
import com.example.backend.model.workspaces.Workspace;
import jakarta.persistence.*;
import org.hibernate.search.engine.backend.types.Sortable;
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*;

import java.time.Instant;
import java.util.*;

/**
 * Note: Removing a report container should not naturally remove the report.
 */
@Entity
@Table(
    //Improve query performance
    indexes = {
        @Index(columnList = "workspace_id")
    }
)
@Indexed
public class ReportContainer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @GenericField
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportContainerStability stability;

    @FullTextField
    @Column
    private String summary;

    @GenericField
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportContainerProgress progress;

    @GenericField
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReportContainerStatus status;

    @GenericField(sortable = Sortable.YES)
    @Column
    private Instant firstReportedTimestamp;
    @Column
    private Instant lastUpdatedTimestamp;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @OneToOne(
        fetch = FetchType.LAZY,
        cascade = {CascadeType.MERGE, CascadeType.PERSIST}
    )
    @JoinColumn(name = "primary_report_id")
    private Report primaryReport;

    @GenericField(sortable = Sortable.YES)
    @Column
    private Double priority;
    @GenericField(sortable = Sortable.YES)
    @Column
    private Double hotness;

    /**
     * - Removing a report from this set, would set report container id to null in the report.
     * - Indexed embedding indexes reports embedded in a report container for searching purposes.
     */
    @IndexedEmbedded(
        includeEmbeddedObjectId = true,
        includePaths = {
            "summaryTitle",
            "stepsToReproduce",
            "environment",
            "expectedResult",
            "actualResult",
            "fixSuggestions",
            "additionalComments"
        }
    )
    @OneToMany(
        mappedBy = "reportContainer",
        fetch = FetchType.LAZY,
        cascade = {CascadeType.MERGE, CascadeType.PERSIST}
    )
    private Set<Report> reports = new HashSet<>();

    /**
     * Note:
     * Removing a workspace custom label from a report container should remove its mapping too.
     */
    @OneToMany(
        mappedBy = "reportContainer",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private Set<ReportContainerWorkspaceCustomLabelMapping> workspaceCustomLabels = new HashSet<>();

    @OneToMany(
        mappedBy = "reportContainer",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private List<ReportContainerActivity> reportContainerActivity = new ArrayList<>();

    @GenericField(sortable = Sortable.YES)
    @Column(nullable = false)
    private Integer frequency;
    @GenericField(sortable = Sortable.YES)
    @Column(nullable = false)
    private Integer incomingDuplicateSuggestionsCount;

    /* Fields to help keep background worker queries and updates smaller and efficient (e.g. for priority updates) */
    @Column
    private Integer totalSeverityRating;
    @Column
    private Integer totalUserImpactRating;

    protected ReportContainer() {}
    public ReportContainer(Workspace workspace, String summary, Report primaryReport, ReportContainerStability stability) {
        this.summary = summary;
        this.workspace = workspace;
        this.status = ReportContainerStatus.UNCONFIRMED;
        this.progress = ReportContainerProgress.OPEN;
        this.primaryReport = primaryReport;
        this.priority = 0.0;
        this.hotness = 1.0;
        this.stability = stability;
        this.lastUpdatedTimestamp = Instant.now();
        this.frequency = 1;
        this.incomingDuplicateSuggestionsCount = 0;
        this.totalSeverityRating = primaryReport.getSeverityRating();
        this.totalUserImpactRating = primaryReport.getUserImpactRating();
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public Workspace getWorkspace() {return workspace;}
    public void setWorkspace(Workspace workspace) {this.workspace = workspace;}
    public String getSummary() {return summary;}
    public void setSummary(String summary) {this.summary = summary;}
    public Double getPriority() {return priority;}
    public void setPriority(Double priority) {this.priority = priority;}
    public ReportContainerProgress getProgress() {return progress;}
    public void setProgress(ReportContainerProgress progress) {this.progress = progress;}
    public ReportContainerStatus getStatus() {return status;}
    public void setStatus(ReportContainerStatus status) {this.status = status;}
    public Report getPrimaryReport() {return primaryReport;}
    public void setPrimaryReport(Report primaryReport) {this.primaryReport = primaryReport;}
    public Set<Report> getReports() {return reports;}
    public void setReports(Set<Report> reports) {this.reports = reports;}
    public Set<ReportContainerWorkspaceCustomLabelMapping> getWorkspaceCustomLabels() {return workspaceCustomLabels;}
    public void setWorkspaceCustomLabels(Set<ReportContainerWorkspaceCustomLabelMapping> workspaceCustomLabels) {this.workspaceCustomLabels = workspaceCustomLabels;}
    public ReportContainerStability getStability() {return stability;}
    public void setStability(ReportContainerStability stability) {this.stability = stability;}
    public Instant getFirstReportedTimestamp() {return firstReportedTimestamp;}
    public void setFirstReportedTimestamp(Instant firstReportedTimestamp) {this.firstReportedTimestamp = firstReportedTimestamp;}
    public List<ReportContainerActivity> getReportContainerActivity() {return reportContainerActivity;}
    public void setReportContainerActivity(List<ReportContainerActivity> reportContainerActivity) {this.reportContainerActivity = reportContainerActivity;}
    public Instant getLastUpdatedTimestamp() {return lastUpdatedTimestamp;}
    public void setLastUpdatedTimestamp(Instant lastUpdatedTimestamp) {this.lastUpdatedTimestamp = lastUpdatedTimestamp;}
    public Double getHotness() {return hotness;}
    public void setHotness(Double hotness) {this.hotness = hotness;}

    public Integer getFrequency() {return frequency;}
    public void setFrequency(Integer frequency) {this.frequency = frequency;}
    public Integer getIncomingDuplicateSuggestionsCount() {return incomingDuplicateSuggestionsCount;}
    public void setIncomingDuplicateSuggestionsCount(Integer incomingDuplicateSuggestionsCount) {this.incomingDuplicateSuggestionsCount = incomingDuplicateSuggestionsCount;}
    public Integer getTotalSeverityRating() {return totalSeverityRating;}
    public void setTotalSeverityRating(Integer totalSeverityRating) {this.totalSeverityRating = totalSeverityRating;}
    public Integer getTotalUserImpactRating() {return totalUserImpactRating;}
    public void setTotalUserImpactRating(Integer totalUserImpactRating) {this.totalUserImpactRating = totalUserImpactRating;}

    @GenericField
    @IndexingDependency(derivedFrom = @ObjectPath(@PropertyValue(propertyName = "workspace")))
    public Long getWorkspaceId() {
        return workspace == null ? null : workspace.getId();
    }

    @GenericField
    @IndexingDependency(
        reindexOnUpdate = ReindexOnUpdate.SHALLOW, //Only update when new label mappings added/deleted.
        derivedFrom = @ObjectPath({@PropertyValue(propertyName = "workspaceCustomLabels")}) //In report container
    )
    public List<String> getLabelNames() {
        if (workspaceCustomLabels == null) return Collections.emptyList();
        return workspaceCustomLabels.stream().map(mapping -> mapping.getWorkspaceCustomLabel().getName()).toList();
    }
}
