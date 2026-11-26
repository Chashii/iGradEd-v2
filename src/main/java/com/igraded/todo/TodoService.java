package com.igraded.todo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.igraded.user.AppUser;
import com.igraded.user.UserRepository;

/** A person's own To-do list: tasks they added themselves plus tasks an admin gave them. */
@Service
public class TodoService {

    private static final DateTimeFormatter DUE = DateTimeFormatter.ofPattern("EEE, MMM d", Locale.ENGLISH);
    private static final int MAX_SHOWN = 30;
    private static final int MAX_TITLE = 120;

    private final TodoRepository todos;
    private final UserRepository users;
    private final ZoneId zone;

    public TodoService(TodoRepository todos, UserRepository users,
                       @Value("${igraded.timezone:Asia/Manila}") String timezone) {
        this.todos = todos;
        this.users = users;
        this.zone = ZoneId.of(timezone);
    }

    /** Open tasks first (soonest due first), finished ones at the bottom. */
    public List<TodoView> forUser(Long userId) {
        LocalDate today = LocalDate.now(zone);
        List<Todo> mine = new ArrayList<>(todos.findByOwnerId(userId));
        mine.sort(Comparator
                .comparing(Todo::isDone)
                .thenComparing(Todo::getDueOn, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(Todo::getCreatedAt));

        Set<Long> assigners = new HashSet<>();
        for (Todo t : mine) {
            if (t.getAssignedBy() != null) {
                assigners.add(t.getAssignedBy());
            }
        }
        Map<Long, String> names = new HashMap<>();
        if (!assigners.isEmpty()) {
            for (AppUser u : users.findAllById(assigners)) {
                names.put(u.getId(), u.getFullName());
            }
        }

        return mine.stream().limit(MAX_SHOWN).map(t -> toView(t, names, today)).toList();
    }

    /** @throws IllegalArgumentException with a message to show if the title is not acceptable */
    public void addPersonal(Long ownerId, String title, LocalDate due) {
        String clean = cleanTitle(title);
        todos.save(new Todo(ownerId, clean, due, LocalDateTime.now(zone), null));
    }

    /** @throws NoSuchElementException if the task is not this person's */
    public void setDone(Long ownerId, Long id, boolean done) {
        Todo todo = todos.findByIdAndOwnerId(id, ownerId).orElseThrow(NoSuchElementException::new);
        todo.setDone(done);
        todos.save(todo);
    }

    /**
     * Deletes a task the person made themselves.
     *
     * @return false if it was given by an admin, which the teacher may not delete
     * @throws NoSuchElementException if the task is not this person's
     */
    public boolean deletePersonal(Long ownerId, Long id) {
        Todo todo = todos.findByIdAndOwnerId(id, ownerId).orElseThrow(NoSuchElementException::new);
        if (todo.getAssignedBy() != null) {
            return false;
        }
        todos.delete(todo);
        return true;
    }

    /** Shared with the assignment service so both apply the same rule. */
    static String cleanTitle(String title) {
        String clean = title == null ? "" : title.trim().replaceAll("\\s+", " ");
        if (clean.isEmpty()) {
            throw new IllegalArgumentException("Type what needs to be done.");
        }
        if (clean.length() > MAX_TITLE) {
            throw new IllegalArgumentException("Use " + MAX_TITLE + " characters or fewer for the task.");
        }
        return clean;
    }

    private TodoView toView(Todo t, Map<Long, String> names, LocalDate today) {
        boolean overdue = !t.isDone() && t.getDueOn() != null && t.getDueOn().isBefore(today);

        List<String> parts = new ArrayList<>();
        if (t.getDueOn() != null) {
            parts.add((overdue ? "Overdue, was due " : "Due ") + t.getDueOn().format(DUE));
        }
        if (t.getAssignedBy() != null) {
            parts.add("Assigned by " + names.getOrDefault(t.getAssignedBy(), "an admin"));
        }
        return new TodoView(t.getId(), t.getTitle(), String.join(" · ", parts),
                t.isDone(), Objects.nonNull(t.getAssignedBy()), overdue);
    }
}
