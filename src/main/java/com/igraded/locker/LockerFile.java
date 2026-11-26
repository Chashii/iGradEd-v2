package com.igraded.locker;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** The details of one uploaded file. The file itself is on disk under storedName (see LockerStorage). */
@Entity
@Table(name = "locker_files")
public class LockerFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 80)
    private String folder;

    /** The name the teacher sees, already cleaned. */
    @Column(nullable = false, length = 255)
    private String originalName;

    /** Random name on disk. */
    @Column(nullable = false, length = 64)
    private String storedName;

    @Column(length = 120)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false)
    private LocalDateTime uploadedAt;

    protected LockerFile() {
        // required by JPA
    }

    public LockerFile(Long ownerId, String folder, String originalName, String storedName,
                      String contentType, long sizeBytes, LocalDateTime uploadedAt) {
        this.ownerId = ownerId;
        this.folder = folder;
        this.originalName = originalName;
        this.storedName = storedName;
        this.contentType = contentType;
        this.sizeBytes = sizeBytes;
        this.uploadedAt = uploadedAt;
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getFolder() { return folder; }
    public String getOriginalName() { return originalName; }
    public String getStoredName() { return storedName; }
    public String getContentType() { return contentType; }
    public long getSizeBytes() { return sizeBytes; }
    public LocalDateTime getUploadedAt() { return uploadedAt; }
}
