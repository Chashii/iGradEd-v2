package com.igraded.web;

import java.util.NoSuchElementException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.igraded.security.AppUserDetails;
import com.igraded.todo.AssignmentService;

import jakarta.validation.Valid;

/**
 * Giving tasks to teachers (admins and the super admin).
 * Who may be tasked is decided in AssignmentService, not here.
 */
@Controller
public class AdminTaskController {

    private final AssignmentService assignments;

    public AdminTaskController(AssignmentService assignments) {
        this.assignments = assignments;
    }

    @GetMapping("/admin/tasks")
    public String page(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("form", new TaskForm());
        fill(model, me);
        return "admin-tasks";
    }

    @PostMapping("/admin/tasks")
    public String assign(@AuthenticationPrincipal AppUserDetails me,
                         @Valid @ModelAttribute("form") TaskForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                int count = assignments.assign(me.getId(), form.getTo(), form.getTitle(), form.getDue());
                redirect.addFlashAttribute("message",
                        count == 1 ? "Task given to 1 teacher." : "Task given to " + count + " teachers.");
                return "redirect:/admin/tasks";
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            }
        }
        fill(model, me);
        return "admin-tasks";
    }

    @PostMapping("/admin/tasks/{id}/delete")
    public String unassign(@AuthenticationPrincipal AppUserDetails me,
                           @PathVariable Long id,
                           RedirectAttributes redirect) {
        try {
            assignments.unassign(me.getId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redirect.addFlashAttribute("message", "Task removed.");
        return "redirect:/admin/tasks";
    }

    private void fill(Model model, AppUserDetails me) {
        model.addAttribute("active", "tasks");
        model.addAttribute("recipients", assignments.recipients(me.getId()));
        model.addAttribute("assigned", assignments.assigned(me.getId()));
    }
}
