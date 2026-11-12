package com.example.backend.model.reports;

import jakarta.persistence.*;

/**
 * Note:
 * - Could add category in the future for if it is an image, snippet, log etc.
 *   Perhaps this could be detected by AI.
 */
@Entity
public class ReportAttachment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private long id;

    @Column
    private String caption;

    @Column
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private FileType type;

    @Column
    private Long size;

    @Column
    private String thumbnailReference;

    protected ReportAttachment() {}

    public ReportAttachment(String caption, String reference, FileType type, Long size) {
        this.caption = caption;
        this.reference = reference;
        this.type = type;
        this.size = size;
    }

    /** Constructor with thumbnail for samples */
    public ReportAttachment(String caption, String reference, String thumbnailReference, FileType type, Long size) {
        this.caption = caption;
        this.reference = reference;
        this.thumbnailReference = thumbnailReference;
        this.type = type;
        this.size = size;
    }

    public long getId() {return id;}
    public void setId(long id) {this.id = id;}
    public String getCaption() {return caption;}
    public void setCaption(String caption) {this.caption = caption;}
    public String getReference() {return reference;}
    public void setReference(String reference) {this.reference = reference;}
    public FileType getType() {return type;}
    public void setType(FileType type) {this.type = type;}
    public Long getSize() {return size;}
    public void setSize(Long size) {this.size = size;}
    public String getThumbnailReference() {return thumbnailReference;}
    public void setThumbnailReference(String thumbnailReference) {this.thumbnailReference = thumbnailReference;}
}
