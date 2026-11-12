package com.example.backend.model.reports;

import com.example.backend.model.bugs.ReportContainer;
import com.example.backend.model.users.User;
import com.example.backend.model.workspaces.Workspace;
import com.example.backend.model.workspaces.WorkspaceRole;
import jakarta.persistence.*;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

@Entity
public class Report {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "workspace_id")
    private Workspace workspace;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    /**
     * A report may belong to a report container, or it may not.
     * A report may be assigned a report container once it has passed through duplicate detection.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "report_container_id")
    private ReportContainer reportContainer;

    /**
     * To hold the role of the user at the time,
     * this is since the user role could change, while
     * it may be useful to know which bugs were reported by a higher ranked user.
     */
    @Enumerated(EnumType.STRING)
    @Column
    private WorkspaceRole userRole;

    @Column(nullable = false)
    private Instant creationTimestamp;

    /* ---------- Report Contents ---------- */
    /*
     * Note: columnDefinition = "TEXT" defines column collation as 'utf8mb4_0900_ai_ci' in MySQL,
     * which uses up to 4 bytes per character, while supporting uni-code & emojis.
     */

    @Column(nullable = false)
    @FullTextField
    private String summaryTitle;

    @Column(columnDefinition = "TEXT")
    @FullTextField
    private String stepsToReproduce;

    @Column(columnDefinition = "TEXT")
    @FullTextField
    private String environment;

    @Column(columnDefinition = "TEXT")
    @FullTextField
    private String expectedResult;

    @Column(columnDefinition = "TEXT")
    @FullTextField
    private String actualResult;

    @Column(columnDefinition = "TEXT")
    @FullTextField
    private String fixSuggestions;

    @Column(columnDefinition = "TEXT")
    @FullTextField
    private String additionalComments;

    @Column
    private int severityRating;

    @Column
    private int userImpactRating;

    @OneToMany(
        cascade = CascadeType.ALL,
        orphanRemoval = true,
        fetch = FetchType.LAZY
    )
    @JoinColumn(name = "report_id")
    private Set<ReportAttachment> reportAttachments = new HashSet<>();

    /**
     * Note:
     * Removing a workspace custom label from a report container should remove its mapping too.
     */
    @OneToMany(
        mappedBy = "report",
        fetch = FetchType.LAZY,
        cascade = CascadeType.ALL,
        orphanRemoval = true
    )
    private Set<ReportWorkspaceCustomLabelMapping> workspaceCustomLabels = new HashSet<>();

    /* ---------- Duplicate Detection ---------- */

    /**
     * Caching vector embedding into database, to avoid having to recompute.
     * This is good for performance.
     */
    @Lob
    private byte[] vectorEmbedding;

    @Column
    private Instant embeddingTimestamp;

    /**
     * Internal duplicate detection lifecycle state for grouping a report into a container.
     */
    @Enumerated(EnumType.STRING)
    @Column
    private DuplicateDetectionLifecycleState duplicateDetectionLifecycleState;

    @Column
    private boolean duplicateSuggestionsBeingManuallyResolved;

    @Column
    private Instant duplicateSuggestionsBeingManuallyResolvedTimestamp;

    @Column
    private Long duplicateSuggestionsBeingManuallyResolvedByUserId;

    protected Report() {}
    public Report(
            Workspace workspace,
            User user,
            WorkspaceRole userRole,
            String summaryTitle,
            String stepsToReproduce,
            String environment,
            String expectedResult,
            String actualResult,
            int userImpactRating,
            int severityRating,
            String fixSuggestions,
            String additionalComments,
            Set<ReportWorkspaceCustomLabelMapping> workspaceCustomLabels,
            Set<ReportAttachment> reportAttachments
    ) {
        this.workspace = workspace;
        this.user = user;
        this.userRole = userRole;
        this.expectedResult = expectedResult;
        this.summaryTitle = summaryTitle;
        this.actualResult = actualResult;
        this.stepsToReproduce = stepsToReproduce;
        this.environment = environment;
        this.userImpactRating = userImpactRating;
        this.severityRating = severityRating;
        this.fixSuggestions = fixSuggestions;
        this.additionalComments = additionalComments;
        this.reportAttachments = reportAttachments;
        this.workspaceCustomLabels = workspaceCustomLabels;
        this.creationTimestamp = Instant.now();
        this.duplicateDetectionLifecycleState = DuplicateDetectionLifecycleState.NEW;
        this.duplicateSuggestionsBeingManuallyResolved = false;
    }

    /* ---------- Getter and Setter Methods ---------- */

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public Workspace getWorkspace() {return workspace;}
    public void setWorkspace(Workspace workspace) {this.workspace = workspace;}
    public ReportContainer getReportContainer() {return reportContainer;}
    public void setReportContainer(ReportContainer reportContainer) {this.reportContainer = reportContainer;}

    public User getUser() {return user;}
    public void setUser(User user) {this.user = user;}
    public WorkspaceRole getUserRole() {return userRole;}
    public void setUserRole(WorkspaceRole userRole) {this.userRole = userRole;}
    public Instant getCreationTimestamp() {return creationTimestamp;}
    public void setCreationTimestamp(Instant creationTimestamp) {this.creationTimestamp = creationTimestamp;}

    public String getSummaryTitle() {return summaryTitle;}
    public void setSummaryTitle(String summaryTitle) {this.summaryTitle = summaryTitle;}
    public String getExpectedResult() {return expectedResult;}
    public void setExpectedResult(String expectedResult) {this.expectedResult = expectedResult;}
    public String getActualResult() {return actualResult;}
    public void setActualResult(String actualResult) {this.actualResult = actualResult;}
    public String getStepsToReproduce() {return stepsToReproduce;}
    public void setStepsToReproduce(String stepsToReproduce) {this.stepsToReproduce = stepsToReproduce;}
    public String getEnvironment() {return environment;}
    public void setEnvironment(String environment) {this.environment = environment;}
    public String getAdditionalComments() {return additionalComments;}
    public void setAdditionalComments(String additionalComments) {this.additionalComments = additionalComments;}
    public String getFixSuggestions() {return fixSuggestions;}
    public void setFixSuggestions(String fixSuggestions) {this.fixSuggestions = fixSuggestions;}
    public int getSeverityRating() {return severityRating;}
    public void setSeverityRating(int severityRating) {this.severityRating = severityRating;}
    public int getUserImpactRating() {return userImpactRating;}
    public void setUserImpactRating(int userImpactRating) {this.userImpactRating = userImpactRating;}

    public Set<ReportAttachment> getReportAttachments() {return reportAttachments;}
    public void setReportAttachments(Set<ReportAttachment> reportAttachments) {this.reportAttachments = reportAttachments;}

    public Set<ReportWorkspaceCustomLabelMapping> getWorkspaceCustomLabels() {return workspaceCustomLabels;}
    public void setWorkspaceCustomLabels(Set<ReportWorkspaceCustomLabelMapping> workspaceCustomLabels) {this.workspaceCustomLabels = workspaceCustomLabels;}

    public DuplicateDetectionLifecycleState getDuplicateDetectionLifecycleState() {return duplicateDetectionLifecycleState;}
    public void setDuplicateDetectionLifecycleState(DuplicateDetectionLifecycleState duplicateDetectionLifecycleState) {this.duplicateDetectionLifecycleState = duplicateDetectionLifecycleState;}
    public boolean isDuplicateSuggestionsBeingManuallyResolved() {return duplicateSuggestionsBeingManuallyResolved;}
    public void setDuplicateSuggestionsBeingManuallyResolved(boolean duplicateSuggestionsBeingManuallyResolved) {this.duplicateSuggestionsBeingManuallyResolved = duplicateSuggestionsBeingManuallyResolved;}
    public Instant getDuplicateSuggestionsBeingManuallyResolvedTimestamp() {return duplicateSuggestionsBeingManuallyResolvedTimestamp;}
    public void setDuplicateSuggestionsBeingManuallyResolvedTimestamp(Instant duplicateSuggestionsBeingManuallyResolvedTimestamp) {this.duplicateSuggestionsBeingManuallyResolvedTimestamp = duplicateSuggestionsBeingManuallyResolvedTimestamp;}
    public Long getDuplicateSuggestionsBeingManuallyResolvedByUserId() {return duplicateSuggestionsBeingManuallyResolvedByUserId;}
    public void setDuplicateSuggestionsBeingManuallyResolvedByUserId(Long duplicateSuggestionsBeingManuallyResolvedByUserId) {this.duplicateSuggestionsBeingManuallyResolvedByUserId = duplicateSuggestionsBeingManuallyResolvedByUserId;}

    public byte[] getVectorEmbedding() {return vectorEmbedding;}
    public void setVectorEmbedding(byte[] vectorEmbedding) {this.vectorEmbedding = vectorEmbedding;}
    public Instant getEmbeddingTimestamp() {return embeddingTimestamp;}
    public void setEmbeddingTimestamp(Instant embeddingTimestamp) {this.embeddingTimestamp = embeddingTimestamp;}
}
