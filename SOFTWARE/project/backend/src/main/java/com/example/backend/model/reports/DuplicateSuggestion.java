package com.example.backend.model.reports;

import jakarta.persistence.*;

@Entity
@Table(
    indexes = {
        //Improve query performance
        @Index(columnList = "to_report_id"),
        @Index(columnList = "from_report_id")
    },
    uniqueConstraints = {
        //A specific report should only be suggested for a specific container once.
        @UniqueConstraint(columnNames = {"from_report_id", "to_report_id"})
    }
)
public class DuplicateSuggestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "from_report_id")
    private Report fromReport;

    @ManyToOne(optional = false)
    @JoinColumn(name = "to_report_id")
    private Report toReport;

    /**
     * The order from 1 = most to least similar duplicate suggestion for a report.
     * Probably I will look for the smallest similarity order where not rejected for a report that, and present reports from the relevant container.
     */
    @Column
    private int similarityOrder;

    @Column
    private Double similarityScore;

    protected DuplicateSuggestion() {}
    public DuplicateSuggestion(Report fromReport, Report toReport, int similarityOrder, Double similarityScore) {
        this.fromReport = fromReport;
        this.toReport = toReport;
        this.similarityOrder = similarityOrder;
        this.similarityScore = similarityScore;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public int getSimilarityOrder() {return similarityOrder;}
    public void setSimilarityOrder(int similarityOrder) {this.similarityOrder = similarityOrder;}
    public Double getSimilarityScore() {return similarityScore;}
    public void setSimilarityScore(Double similarityScore) {this.similarityScore = similarityScore;}
    public Report getFromReport() {return fromReport;}
    public void setFromReport(Report fromReport) {this.fromReport = fromReport;}
    public Report getToReport() {return toReport;}
    public void setToReport(Report toReport) {this.toReport = toReport;}
}
