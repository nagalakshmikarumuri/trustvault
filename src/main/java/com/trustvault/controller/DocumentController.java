package com.trustvault.controller;

import com.trustvault.entity.Document;
import com.trustvault.entity.User;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.DocumentService;
import com.trustvault.service.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;

/**
 * DocumentController
 *
 * Spring MVC controller managing secure document upload and listing for authenticated users.
 *
 * <p>Security considerations:
 * <ul>
 *   <li>The authenticated user's identity is resolved exclusively from Spring Security's
 *       {@link UserDetails} principal — the owner is never accepted from request parameters.</li>
 *   <li>Only the currently authenticated user's non-deleted documents are listed.</li>
 *   <li>CSRF protection is active on all POST endpoints (enforced by SecurityConfig).</li>
 *   <li>The upload storage directory is outside the classpath and is never mapped as a
 *       static resource, so stored files cannot be accessed directly via HTTP.</li>
 * </ul>
 */
@Controller
@RequestMapping("/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentStorageService documentStorageService;
    private final DocumentService documentService;
    private final UserRepository userRepository;

    public DocumentController(DocumentStorageService documentStorageService,
                               DocumentService documentService,
                               UserRepository userRepository) {
        this.documentStorageService = documentStorageService;
        this.documentService = documentService;
        this.userRepository = userRepository;
    }

    /**
     * Renders the documents page showing the upload form and the authenticated user's document list.
     *
     * @param principal the Spring Security principal for the authenticated user
     * @param model     Thymeleaf model
     * @return the "documents" Thymeleaf template
     */
    @GetMapping
    public String listDocuments(@AuthenticationPrincipal UserDetails principal, Model model) {
        User owner = resolveAuthenticatedUser(principal);
        List<Document> documents = documentService.getDocumentsByOwnerId(owner.getId());
        model.addAttribute("documents", documents);
        model.addAttribute("username", owner.getUsername());
        return "documents";
    }

    /**
     * Handles secure file upload for the authenticated user.
     *
     * <p>The owner is determined entirely from the Spring Security context; no owner identifier
     * is accepted from the form. Validation is delegated to {@link DocumentStorageService}.
     *
     * @param file               the multipart file from the upload form
     * @param category           optional document category submitted by the user
     * @param principal          the Spring Security principal for the authenticated user
     * @param redirectAttributes redirect attributes used to pass flash messages
     * @return redirect to {@code /documents} after processing
     */
    @PostMapping("/upload")
    public String uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false, defaultValue = "") String category,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes) {

        User owner = resolveAuthenticatedUser(principal);

        try {
            // Delegate file validation and physical storage to DocumentStorageService.
            String storedFilename = documentStorageService.store(file);

            // Build the Document metadata entity. Owner is set from the authenticated principal.
            Document document = new Document(
                    file.getOriginalFilename(),
                    storedFilename,
                    owner,
                    category.isBlank() ? null : category.trim(),
                    file.getContentType(),
                    file.getSize()
            );

            documentService.saveDocument(document);

            log.info("Document uploaded successfully: storedFilename={}, owner={}, size={}",
                    storedFilename, owner.getUsername(), file.getSize());

            redirectAttributes.addFlashAttribute("successMessage",
                    "File \"" + file.getOriginalFilename() + "\" uploaded successfully.");

        } catch (IllegalArgumentException e) {
            log.warn("Document upload rejected for user {}: {}", owner.getUsername(), e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        } catch (IOException e) {
            log.error("I/O error storing document for user {}: {}", owner.getUsername(), e.getMessage(), e);
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Upload failed due to a server error. Please try again.");
        }

        return "redirect:/documents";
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Resolves the authenticated {@link User} entity from the Spring Security principal.
     * The username is taken from the authenticated session — it is never read from a request parameter.
     *
     * @param principal the Spring Security {@link UserDetails} principal
     * @return the matching {@link User} entity from the database
     * @throws UsernameNotFoundException if no matching user record is found (should not happen in practice)
     */
    private User resolveAuthenticatedUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authenticated user not found in database: " + principal.getUsername()));
    }
}
