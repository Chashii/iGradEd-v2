package com.igraded.todo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;

import com.igraded.department.Department;
import com.igraded.department.DepartmentRepository;
import com.igraded.security.Permissions;
import com.igraded.user.AppUser;
import com.igraded.user.Role;
import com.igraded.user.UserRepository;
import com.igraded.user.UserService;

/**
 * Giving tasks to teachers. A department head (admin) can task teachers in their own department;
 * the super admin can task anyone. Powers are checked against the database on every call.
 */
@Service
public class AssignmentService {

    private static final DateTimeFormatter DUE = DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);
    private static final int MAX_LISTED = 200;

    /** One choice in the "For" list. value is "all", "dept:ID" or "user:ID". */
    public record Recipient(String value, String label) {
    }

    /** One line of the "Tasks you've given" table. */
    public record AssignedView(Long id, String title, String teacher, String due, boolean done, String assignedBy) {
    }

    private final TodoRepository todos;
    private final UserRepository users;
    private final UserService userService;
    private final DepartmentRepository departments;
    private final ZoneId zone;

    public AssignmentService(TodoRepository todos, UserRepository users, UserService userService,
                             DepartmentRepository departments,
                             @Value("${igraded.timezone:Asia/Manila}") String timezone) {
        this.todos = todos;
        this.users = users;
        this.userService = userService;
        this.departments = departments;
        this.zone = ZoneId.of(timezone);
    }

    /** Who this admin may give a task to. Empty for a head who is not in a department yet. */
    public List<Recipient> recipients(Long actorId) {
        AppUser actor = userService.requireStaff(actorId);
        List<Recipient> out = new ArrayList<>();

        if (actor.getRole() == Role.SUPER_ADMIN) {
            List<AppUser> everyone = users.findByRoleOrderByFullNameAsc(Role.TEACHER);
            Map<Long, String> deptNames = new HashMap<>();
            out.add(new Recipient("all", "All teachers (" + everyone.size() + ")"));
            for (Department d : departments.findAllByOrderByNameAsc()) {
                deptNames.put(d.getId(), d.getName());
                long count = everyone.stream().filter(t -> d.getId().equals(t.getDepartmentId())).count();
                out.add(new Recipient("dept:" + d.getId(), "Everyone in " + d.getName() + " (" + count + ")"));
            }
            for (AppUser t : everyone) {
                String where = t.getDepartmentId() == null ? "no department" : deptNames.get(t.getDepartmentId());
                out.add(new Recipient("user:" + t.getId(), t.getFullName() + " (" + where + ")"));
            }
            return out;
        }

        if (actor.getDepartmentId() == null) {
            return out;
        }
        Department dept = departments.findById(actor.getDepartmentId()).orElse(null);
        if (dept == null) {
            return out;
        }
        List<AppUser> mine = users.findByRoleAndDepartmentIdOrderByFullNameAsc(Role.TEACHER, dept.getId());
        out.add(new Recipient("dept:" + dept.getId(), "Everyone in " + dept.getName() + " (" + mine.size() + ")"));
        for (AppUser t : mine) {
            out.add(new Recipient("user:" + t.getId(), t.getFullName()));
        }
        return out;
    }

    /**
     * Gives the task to everyone in the chosen group.
     *
     * @return how many teachers got it
     * @throws IllegalArgumentException with a message to show, if the form is not acceptable
     * @throws AccessDeniedException    if this admin may not task that group
     */
    public int assign(Long actorId, String to, String title, LocalDate due) {
        AppUser actor = userService.requireStaff(actorId);
        String clean = TodoService.cleanTitle(title);
        List<AppUser> targets = resolve(actor, to);
        if (targets.isEmpty()) {
            throw new IllegalArgumentException("There are no teachers in that group yet.");
        }

        LocalDateTime now = LocalDateTime.now(zone);
        List<Todo> created = new ArrayList<>();
        for (AppUser teacher : targets) {
            created.add(new Todo(teacher.getId(), clean, due, now, actor.getId()));
        }
        todos.saveAll(created);
        return created.size();
    }

    /** Tasks this admin gave out (the super admin sees everyone's), newest first. */
    public List<AssignedView> assigned(Long actorId) {
        AppUser actor = userService.requireStaff(actorId);
        List<Todo> list = actor.getRole() == Role.SUPER_ADMIN
                ? todos.findByAssignedByIsNotNullOrderByCreatedAtDesc()
                : todos.findByAssignedByOrderByCreatedAtDesc(actor.getId());
        if (list.size() > MAX_LISTED) {
            list = list.subList(0, MAX_LISTED);
        }

        Set<Long> ids = new HashSet<>();
        for (Todo t : list) {
            ids.add(t.getOwnerId());
            if (t.getAssignedBy() != null) {
                ids.add(t.getAssignedBy());
            }
        }
        Map<Long, String> names = new HashMap<>();
        if (!ids.isEmpty()) {
            for (AppUser u : users.findAllById(ids)) {
                names.put(u.getId(), u.getFullName());
            }
        }

        List<AssignedView> out = new ArrayList<>();
        for (Todo t : list) {
            out.add(new AssignedView(t.getId(), t.getTitle(),
                    names.getOrDefault(t.getOwnerId(), "Unknown"),
                    t.getDueOn() == null ? "No due date" : t.getDueOn().format(DUE),
                    t.isDone(),
                    names.getOrDefault(t.getAssignedBy(), "")));
        }
        return out;
    }

    /** Takes a task back from a teacher. */
    public void unassign(Long actorId, Long todoId) {
        AppUser actor = userService.requireStaff(actorId);
        Todo todo = todos.findById(todoId).orElseThrow(NoSuchElementException::new);
        if (!Permissions.canUnassign(actor.getRole(), actor.getId(), todo.getAssignedBy())) {
            throw new AccessDeniedException("Not a task you gave");
        }
        todos.delete(todo);
    }

    /** Turns "all", "dept:3" or "user:7" into teachers, after checking this admin may task them. */
    private List<AppUser> resolve(AppUser actor, String to) {
        if (to == null || to.isBlank()) {
            throw new IllegalArgumentException("Choose who the task is for.");
        }

        if (to.equals("all")) {
            if (!Permissions.canAssignToAll(actor.getRole())) {
                throw new AccessDeniedException("Only the super admin can task everyone");
            }
            return users.findByRoleOrderByFullNameAsc(Role.TEACHER);
        }

        if (to.startsWith("dept:")) {
            Long deptId = parseId(to.substring(5));
            if (!Permissions.canAssignToDepartment(actor.getRole(), actor.getDepartmentId(), deptId)) {
                throw new AccessDeniedException("Not your department");
            }
            return users.findByRoleAndDepartmentIdOrderByFullNameAsc(Role.TEACHER, deptId);
        }

        if (to.startsWith("user:")) {
            AppUser teacher = users.findById(parseId(to.substring(5)))
                    .orElseThrow(() -> new IllegalArgumentException("That teacher no longer exists."));
            if (teacher.getRole() != Role.TEACHER) {
                throw new IllegalArgumentException("Tasks can only be given to teachers.");
            }
            if (!Permissions.canAssignToDepartment(actor.getRole(), actor.getDepartmentId(), teacher.getDepartmentId())) {
                throw new AccessDeniedException("Not your department");
            }
            return List.of(teacher);
        }

        throw new IllegalArgumentException("Choose who the task is for.");
    }

    private static Long parseId(String text) {
        try {
            return Long.parseLong(text.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Choose who the task is for.");
        }
    }
}
