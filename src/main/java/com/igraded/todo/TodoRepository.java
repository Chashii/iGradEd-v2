package com.igraded.todo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    List<Todo> findByOwnerId(Long ownerId);

    /** By id AND owner, so nobody can tick or delete someone else's task. */
    Optional<Todo> findByIdAndOwnerId(Long id, Long ownerId);

    /** Tasks one admin has given out. */
    List<Todo> findByAssignedByOrderByCreatedAtDesc(Long assignedBy);

    /** Every task any admin has given out (for the super admin). */
    List<Todo> findByAssignedByIsNotNullOrderByCreatedAtDesc();
}
