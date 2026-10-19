package com.trustvault.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * HomeController
 *
 * Spring MVC controller that serves the initial landing page via Thymeleaf.
 */
@Controller
public class HomeController {

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("appName", "TrustVault");
        model.addAttribute("tagline", "Secure Document-Sharing Application");
        model.addAttribute("phase", "Phase 1: Project Initialization");
        model.addAttribute("status", "ONLINE");
        return "index";
    }
}
