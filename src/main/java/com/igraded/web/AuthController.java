package com.igraded.web;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

import com.igraded.user.Role;
import com.igraded.user.UserService;

import jakarta.validation.Valid;

/**
 * Login and sign-up pages. The login POST itself is handled by Spring Security (see SecurityConfig).
 * Anyone who signs up becomes a TEACHER; admin roles can only be given by a Super Admin.
 */
@Controller
public class AuthController {

    private final UserService users;

    public AuthController(UserService users) {
        this.users = users;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/signup")
    public String signupForm(Model model) {
        model.addAttribute("form", new SignupForm());
        return "signup";
    }

    @PostMapping("/signup")
    public String signup(@Valid @ModelAttribute("form") SignupForm form, BindingResult result) {
        if (!result.hasFieldErrors("password") && !result.hasFieldErrors("confirmPassword")
                && !form.getPassword().equals(form.getConfirmPassword())) {
            result.rejectValue("confirmPassword", "mismatch", "Passwords do not match");
        }
        if (!result.hasFieldErrors("email") && users.emailTaken(form.getEmail())) {
            result.rejectValue("email", "taken", "An account with this email already exists");
        }
        if (result.hasErrors()) {
            return "signup";
        }

        try {
            users.create(form.getFullName(), form.getEmail(), form.getPassword(), Role.TEACHER);
        } catch (DataIntegrityViolationException e) {
            // Two people registering the same email at the same moment
            result.rejectValue("email", "taken", "An account with this email already exists");
            return "signup";
        }
        return "redirect:/login?registered";
    }
}
