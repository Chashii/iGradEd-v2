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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.igraded.news.AnnouncementForm;
import com.igraded.news.AnnouncementKind;
import com.igraded.news.AnnouncementService;
import com.igraded.security.AppUserDetails;

import jakarta.validation.Valid;

/**
 * Posting, editing and removing announcements (admins and the super admin).
 * Who may do what is decided in AnnouncementService, not here.
 */
@Controller
public class AdminAnnouncementController {

    private final AnnouncementService announcements;

    public AdminAnnouncementController(AnnouncementService announcements) {
        this.announcements = announcements;
    }

    /** edit = the id of an announcement to change. Without it the form is for a new one. */
    @GetMapping("/admin/announcements")
    public String page(@AuthenticationPrincipal AppUserDetails me,
                       @RequestParam(required = false) Long edit,
                       Model model) {
        AnnouncementForm form;
        if (edit == null) {
            form = new AnnouncementForm();
        } else {
            try {
                form = announcements.formFor(me.getId(), edit);
            } catch (NoSuchElementException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        model.addAttribute("form", form);
        fill(model, me, edit);
        return "admin-announcements";
    }

    @PostMapping("/admin/announcements")
    public String create(@AuthenticationPrincipal AppUserDetails me,
                         @Valid @ModelAttribute("form") AnnouncementForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        return save(me, null, form, result, model, redirect);
    }

    @PostMapping("/admin/announcements/{id}")
    public String update(@AuthenticationPrincipal AppUserDetails me,
                         @PathVariable Long id,
                         @Valid @ModelAttribute("form") AnnouncementForm form,
                         BindingResult result,
                         Model model,
                         RedirectAttributes redirect) {
        return save(me, id, form, result, model, redirect);
    }

    @PostMapping("/admin/announcements/{id}/delete")
    public String delete(@AuthenticationPrincipal AppUserDetails me,
                         @PathVariable Long id,
                         RedirectAttributes redirect) {
        try {
            announcements.delete(me.getId(), id);
        } catch (NoSuchElementException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        redirect.addFlashAttribute("message", "Announcement removed.");
        return "redirect:/admin/announcements";
    }

    private String save(AppUserDetails me, Long id, AnnouncementForm form, BindingResult result,
                        Model model, RedirectAttributes redirect) {
        if (!result.hasErrors()) {
            try {
                announcements.save(me.getId(), id, form);
                redirect.addFlashAttribute("message", id == null ? "Announcement posted." : "Announcement updated.");
                return "redirect:/admin/announcements";
            } catch (IllegalArgumentException e) {
                model.addAttribute("error", e.getMessage());
            } catch (NoSuchElementException e) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
        }
        fill(model, me, id);
        return "admin-announcements";
    }

    private void fill(Model model, AppUserDetails me, Long editingId) {
        model.addAttribute("active", "announcements");
        model.addAttribute("editingId", editingId);
        model.addAttribute("kinds", AnnouncementKind.values());
        model.addAttribute("audiences", announcements.audiences(me.getId()));
        model.addAttribute("rows", announcements.manageList(me.getId()));
    }
}
