package com.igraded.todo;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** One task on someone's To-do list. assignedBy is null for a task the person made for themselves. */
@Entity
@Table(name = "todos")
public class Todo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long ownerId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column
    private LocalDate dueOn;

    @Column(nullable = false)
    private boolean done;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column
    private Long assignedBy;

    protected Todo() {
        // required by JPA
    }

    public Todo(Long ownerId, String title, LocalDate dueOn, LocalDateTime createdAt, Long assignedBy) {
        this.ownerId = ownerId;
        this.title = title;
        this.dueOn = dueOn;
        this.createdAt = createdAt;
        this.assignedBy = assignedBy;
    }

    public Long getId() { return id; }
    public Long getOwnerId() { return ownerId; }
    public String getTitle() { return title; }
    public LocalDate getDueOn() { return dueOn; }
    public boolean isDone() { return done; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Long getAssignedBy() { return assignedBy; }

    public void setDone(boolean done) { this.done = done; }
}
