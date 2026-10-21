package com.trustvault.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * LoginController
 *
 * Spring MVC controller that serves the TrustVault login view.
 */
@Controller
public class LoginController {

    /**
     * Handles GET /login and displays the login page.
     *
     * @return the name of the Thymeleaf view template ("login")
     */
    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
