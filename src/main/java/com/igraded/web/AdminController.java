package com.igraded.web;

import java.util.NoSuchElementException;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.igraded.department.DepartmentService;
import com.igraded.security.AppUserDetails;
import com.igraded.user.Role;
import com.igraded.user.UserService;

/**
 * /admin/users              - Admin and Super Admin: see all users.
 * /superadmin/users/{id}/** - Super Admin only: change a user's role or department.
 * The URL rules are in SecurityConfig, so they are enforced before these methods run.
 */
@Controller
public class AdminController {

    private final UserService users;
    private final DepartmentService departments;

    public AdminController(UserService users, DepartmentService departments) {
        this.users = users;
        this.departments = departments;
    }

    @GetMapping("/admin/users")
    public String list(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("active", "users");
        model.addAttribute("users", users.rows());
        model.addAttribute("roles", Role.values());
        model.addAttribute("departments", departments.all());
        model.addAttribute("canEdit", me.getRole() == Role.SUPER_ADMIN);
        model.addAttribute("meId", me.getId());
        return "admin-users";
    }

    @PostMapping("/superadmin/users/{id}/role")
    public String changeRole(@PathVariable Long id,
                             @RequestParam Role role,
                             @AuthenticationPrincipal AppUserDetails me,
                             RedirectAttributes redirect) {
        if (id.equals(me.getId())) {
            // Stops the last Super Admin from demoting themselves and locking everyone out
            redirect.addFlashAttribute("error", "You can't change your own role.");
        } else {
            try {
                users.changeRole(id, role);
                redirect.addFlashAttribute("message",
                        "Role updated. The page access changes the next time that person logs in.");
            } catch (NoSuchElementException e) {
                redirect.addFlashAttribute("error", "That user no longer exists.");
            }
        }
        return "redirect:/admin/users";
    }

    /** An empty departmentId means "no department". */
    @PostMapping("/superadmin/users/{id}/department")
    public String changeDepartment(@PathVariable Long id,
                                   @RequestParam(required = false) Long departmentId,
                                   RedirectAttributes redirect) {
        try {
            users.changeDepartment(id, departmentId);
            redirect.addFlashAttribute("message", "Department updated.");
        } catch (NoSuchElementException e) {
            redirect.addFlashAttribute("error", "That user or department no longer exists.");
        }
        return "redirect:/admin/users";
    }
}
