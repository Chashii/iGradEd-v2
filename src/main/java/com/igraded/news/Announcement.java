package com.igraded.news;

import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One announcement. departmentId null means it is shown to the whole school. */
@Entity
@Table(name = "announcements")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private AnnouncementKind kind;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(nullable = false, length = 500)
    private String body;

    @Column(nullable = false)
    private LocalDate publishedOn;

    /** Last day it is shown. Null = never hidden. */
    @Column
    private LocalDate expiresOn;

    /** The day an event or meeting takes place. Null for plain news. */
    @Column
    private LocalDate eventOn;

    @Column
    private Long departmentId;

    @Column
    private Long createdBy;

    protected Announcement() {
        // required by JPA
    }

    public Announcement(AnnouncementKind kind, String title, String body, LocalDate publishedOn,
                        LocalDate expiresOn, LocalDate eventOn, Long departmentId, Long createdBy) {
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.publishedOn = publishedOn;
        this.expiresOn = expiresOn;
        this.eventOn = eventOn;
        this.departmentId = departmentId;
        this.createdBy = createdBy;
    }

    /** Changes everything an editor can change. Who made it stays the same. */
    public void update(AnnouncementKind kind, String title, String body, LocalDate publishedOn,
                       LocalDate expiresOn, LocalDate eventOn, Long departmentId) {
        this.kind = kind;
        this.title = title;
        this.body = body;
        this.publishedOn = publishedOn;
        this.expiresOn = expiresOn;
        this.eventOn = eventOn;
        this.departmentId = departmentId;
    }

    public Long getId() { return id; }
    public AnnouncementKind getKind() { return kind; }
    public String getTitle() { return title; }
    public String getBody() { return body; }
    public LocalDate getPublishedOn() { return publishedOn; }
    public LocalDate getExpiresOn() { return expiresOn; }
    public LocalDate getEventOn() { return eventOn; }
    public Long getDepartmentId() { return departmentId; }
    public Long getCreatedBy() { return createdBy; }
}
