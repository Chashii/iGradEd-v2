package com.igraded.department;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.igraded.user.UserRepository;

@Service
public class DepartmentService {

    /** One line of the Departments page. */
    public record Row(Long id, String name, long people) {
    }

    private final DepartmentRepository departments;
    private final UserRepository users;

    public DepartmentService(DepartmentRepository departments, UserRepository users) {
        this.departments = departments;
        this.users = users;
    }

    public List<Department> all() {
        return departments.findAllByOrderByNameAsc();
    }

    public List<Row> rows() {
        return departments.findAllByOrderByNameAsc().stream()
                .map(d -> new Row(d.getId(), d.getName(), users.countByDepartmentId(d.getId())))
                .toList();
    }

    @Transactional
    public void create(String rawName) {
        String name = rawName == null ? "" : rawName.trim().replaceAll("\\s+", " ");
        if (name.length() < 2 || name.length() > 80) {
            throw new IllegalArgumentException("Use 2 to 80 characters for the department name.");
        }
        if (departments.existsByNameIgnoreCase(name)) {
            throw new IllegalArgumentException("There is already a department called " + name + ".");
        }
        departments.save(new Department(name));
    }

    /** The database clears the department of its people and deletes its announcements. */
    @Transactional
    public void delete(Long id) {
        departments.deleteById(id);
    }
}
