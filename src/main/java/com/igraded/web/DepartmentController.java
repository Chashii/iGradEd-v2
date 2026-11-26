package com.igraded.web;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.igraded.department.DepartmentService;

/** Departments are set up by the super admin only (see the /superadmin/** rule in SecurityConfig). */
@Controller
public class DepartmentController {

    private final DepartmentService departments;

    public DepartmentController(DepartmentService departments) {
        this.departments = departments;
    }

    @GetMapping("/superadmin/departments")
    public String page(Model model) {
        model.addAttribute("active", "departments");
        model.addAttribute("rows", departments.rows());
        return "admin-departments";
    }

    @PostMapping("/superadmin/departments")
    public String create(@RequestParam String name, RedirectAttributes redirect) {
        try {
            departments.create(name);
            redirect.addFlashAttribute("message", "Department added. Now set each head and teacher's department on the Users page.");
        } catch (IllegalArgumentException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/superadmin/departments";
    }

    @PostMapping("/superadmin/departments/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirect) {
        departments.delete(id);
        redirect.addFlashAttribute("message", "Department deleted.");
        return "redirect:/superadmin/departments";
    }
}
