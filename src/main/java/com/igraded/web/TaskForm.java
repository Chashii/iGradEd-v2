package com.igraded.web;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** The "Give a task" form. */
public class TaskForm {

    @NotBlank(message = "Type what needs to be done")
    @Size(max = 120, message = "Use 120 characters or fewer")
    private String title;

    /** "all", "dept:ID" or "user:ID". */
    @NotBlank(message = "Choose who the task is for")
    private String to;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate due;

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTo() { return to; }
    public void setTo(String to) { this.to = to; }

    public LocalDate getDue() { return due; }
    public void setDue(LocalDate due) { this.due = due; }
}
