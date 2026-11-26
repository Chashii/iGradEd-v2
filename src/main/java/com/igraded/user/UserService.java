package com.igraded.user;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.igraded.department.Department;
import com.igraded.department.DepartmentRepository;
import com.igraded.security.Permissions;

@Service
public class UserService {

    private final UserRepository users;
    private final DepartmentRepository departments;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, DepartmentRepository departments, PasswordEncoder encoder) {
        this.users = users;
        this.departments = departments;
        this.encoder = encoder;
    }

    /** Emails are compared trimmed and lower-case everywhere. */
    public static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    public boolean emailTaken(String email) {
        return users.existsByEmail(normalizeEmail(email));
    }

    @Transactional
    public AppUser create(String fullName, String email, String rawPassword, Role role) {
        AppUser user = new AppUser(
                fullName.trim(),
                normalizeEmail(email),
                encoder.encode(rawPassword),
                role);
        return users.save(user);
    }

    /**
     * Loads the person from the database and checks they are an admin or super admin right now.
     * The login session can be out of date after a role change, so powers are always checked against the database.
     */
    @Transactional(readOnly = true)
    public AppUser requireStaff(Long actorId) {
        AppUser actor = users.findById(actorId).orElseThrow(() -> new AccessDeniedException("Unknown user"));
        if (!actor.isEnabled() || !Permissions.isStaff(actor.getRole())) {
            throw new AccessDeniedException("Only admins can do this");
        }
        return actor;
    }

    @Transactional(readOnly = true)
    public List<UserRow> rows() {
        Map<Long, String> names = departments.findAll().stream()
                .collect(Collectors.toMap(Department::getId, Department::getName));
        return users.findAllByOrderByFullNameAsc().stream()
                .map(u -> new UserRow(u.getId(), u.getFullName(), u.getEmail(), u.getRole(),
                        u.getDepartmentId(),
                        u.getDepartmentId() == null ? null : names.get(u.getDepartmentId()),
                        u.getCreatedOn()))
                .toList();
    }

    @Transactional
    public void changeRole(Long id, Role role) {
        AppUser user = users.findById(id).orElseThrow(() -> new NoSuchElementException("No user " + id));
        user.setRole(role);
    }

    /** departmentId null means "no department". */
    @Transactional
    public void changeDepartment(Long id, Long departmentId) {
        AppUser user = users.findById(id).orElseThrow(() -> new NoSuchElementException("No user " + id));
        if (departmentId != null && !departments.existsById(departmentId)) {
            throw new NoSuchElementException("No department " + departmentId);
        }
        user.setDepartmentId(departmentId);
    }
}
