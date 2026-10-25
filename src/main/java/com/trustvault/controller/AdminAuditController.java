package com.trustvault.controller;

import com.trustvault.entity.AuditAction;
import com.trustvault.entity.AuditLog;
import com.trustvault.entity.User;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.AuditService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin")
public class AdminAuditController {

    private final AuditService auditService;
    private final UserRepository userRepository;

    public AdminAuditController(AuditService auditService, UserRepository userRepository) {
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @GetMapping("/audit")
    public String auditView(
            @RequestParam(value = "action", required = false) String action,
            @RequestParam(value = "actor", required = false) String actor,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        User current = userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authenticated user not found in database: " + principal.getUsername()));

        AuditAction parsedAction = parseAction(action);
        List<AuditLog> logs = auditService.search(parsedAction, actor, from, to);

        model.addAttribute("logs", logs);
        model.addAttribute("actions", AuditAction.values());
        model.addAttribute("action", action);
        model.addAttribute("actor", actor);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        model.addAttribute("username", current.getUsername());
        model.addAttribute("isAdmin", true);
        return "audit";
    }

    private AuditAction parseAction(String action) {
        if (action == null || action.isBlank()) {
            return null;
        }
        try {
            return AuditAction.valueOf(action.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
