package com.igraded.web;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.igraded.security.AppUserDetails;

@Controller
public class ProfileController {

    @GetMapping("/profile")
    public String profile(@AuthenticationPrincipal AppUserDetails me, Model model) {
        model.addAttribute("active", "profile");
        model.addAttribute("me", me);
        return "profile";
    }
}
