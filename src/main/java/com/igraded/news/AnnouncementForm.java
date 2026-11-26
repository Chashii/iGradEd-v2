package com.igraded.news;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** The fields on the Announcements form, with their rules. */
public class AnnouncementForm {

    @NotNull(message = "Choose a type")
    private AnnouncementKind kind = AnnouncementKind.NEWS;

    @NotBlank(message = "Enter a title")
    @Size(max = 120, message = "Use 120 characters or fewer")
    private String title;

    @NotBlank(message = "Enter the message")
    @Size(max = 500, message = "Use 500 characters or fewer")
    private String body;

    /** The day an event or meeting happens (optional). */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate eventOn;

    /** First day it is shown. Empty means today. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate publishedOn;

    /** Last day it is shown. Empty means it stays until someone removes it. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiresOn;

    /** "school" for everyone, or a department id. */
    private String audience = "school";

    public AnnouncementKind getKind() { return kind; }
    public void setKind(AnnouncementKind kind) { this.kind = kind; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public LocalDate getEventOn() { return eventOn; }
    public void setEventOn(LocalDate eventOn) { this.eventOn = eventOn; }

    public LocalDate getPublishedOn() { return publishedOn; }
    public void setPublishedOn(LocalDate publishedOn) { this.publishedOn = publishedOn; }

    public LocalDate getExpiresOn() { return expiresOn; }
    public void setExpiresOn(LocalDate expiresOn) { this.expiresOn = expiresOn; }

    public String getAudience() { return audience; }
    public void setAudience(String audience) { this.audience = audience; }
}
