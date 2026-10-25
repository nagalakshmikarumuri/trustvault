package com.trustvault.controller;

import com.trustvault.entity.Document;
import com.trustvault.entity.DocumentPermission;
import com.trustvault.entity.PermissionType;
import com.trustvault.entity.User;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.DocumentPermissionService;
import com.trustvault.service.DocumentService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/documents")
public class DocumentPermissionController {

    private final DocumentPermissionService documentPermissionService;
    private final DocumentService documentService;
    private final UserRepository userRepository;

    public DocumentPermissionController(DocumentPermissionService documentPermissionService,
                                        DocumentService documentService,
                                        UserRepository userRepository) {
        this.documentPermissionService = documentPermissionService;
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    // Convert access denied exceptions to 404 for permission listing
    @ExceptionHandler(com.trustvault.exception.DocumentAccessDeniedException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public void handleAccessDenied() {
        // No body – simply return 404
    }

    @GetMapping("/{id}/permissions")
    public String listPermissions(@PathVariable("id") Long id,
                                  @AuthenticationPrincipal UserDetails principal,
                                  Model model) {
        User actor = resolveAuthenticatedUser(principal);
        List<DocumentPermission> permissions = documentPermissionService.listPermissions(actor, id);
        Document document = documentService.findDocumentById(id)
                .orElseThrow(com.trustvault.exception.DocumentAccessDeniedException::new);
        model.addAttribute("document", document);
        model.addAttribute("permissions", permissions);
        model.addAttribute("username", actor.getUsername());
        model.addAttribute("isAdmin", DocumentController.hasAdminRole(principal));
        return "permissions";
    }

    @PostMapping("/{id}/permissions/grant")
    public String grant(@PathVariable("id") Long id,
                         @RequestParam("username") String username,
                         @RequestParam("permissionType") String permissionType,
                         @AuthenticationPrincipal UserDetails principal,
                         RedirectAttributes redirectAttributes) {
        User actor = resolveAuthenticatedUser(principal);
        try {
            PermissionType type = PermissionType.valueOf(permissionType.trim().toUpperCase());
            documentPermissionService.grant(actor, id, username, type);
            redirectAttributes.addFlashAttribute("successMessage", "Permission granted.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (com.trustvault.exception.DocumentAccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied.");
            return "redirect:/documents";
        }
        return "redirect:/documents/" + id + "/permissions";
    }

    @PostMapping("/{id}/permissions/{permissionId}/revoke")
    public String revoke(@PathVariable("id") Long id,
                         @PathVariable("permissionId") Long permissionId,
                         @AuthenticationPrincipal UserDetails principal,
                         RedirectAttributes redirectAttributes) {
        User actor = resolveAuthenticatedUser(principal);
        try {
            documentPermissionService.revoke(actor, id, permissionId);
            redirectAttributes.addFlashAttribute("successMessage", "Permission revoked.");
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (com.trustvault.exception.DocumentAccessDeniedException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied.");
            return "redirect:/documents";
        }
        return "redirect:/documents/" + id + "/permissions";
    }

    private User resolveAuthenticatedUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authenticated user not found in database: " + principal.getUsername()));
    }
}
