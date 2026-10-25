package com.trustvault.controller;

import com.trustvault.dto.DocumentListItem;
import com.trustvault.entity.AuditAction;
import com.trustvault.entity.AuditOutcome;
import com.trustvault.entity.Document;
import com.trustvault.entity.DocumentPermission;
import com.trustvault.entity.User;
import com.trustvault.exception.DocumentAccessDeniedException;
import com.trustvault.repository.UserRepository;
import com.trustvault.service.AuditService;
import com.trustvault.service.DocumentAccessService;
import com.trustvault.service.DocumentPermissionService;
import com.trustvault.service.DocumentService;
import com.trustvault.service.DocumentStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * DocumentController
 *
 * Spring MVC controller managing secure document upload, listing, download,
 * delete, metadata edit, and search for authenticated users.
 *
 * <p>Security considerations:
 * <ul>
 *   <li>The authenticated user's identity is resolved exclusively from Spring Security's
 *       {@link UserDetails} principal — the owner is never accepted from request parameters.</li>
 *   <li>Authorization is evaluated server-side before any file is read.</li>
 *   <li>CSRF protection is active on all POST endpoints (enforced by SecurityConfig).</li>
 *   <li>The upload storage directory is outside the classpath and is never mapped as a
 *       static resource, so stored files cannot be accessed directly via HTTP.</li>
 *   <li>Physical storage filenames are never returned to the client.</li>
 * </ul>
 */
@Controller
@RequestMapping("/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentStorageService documentStorageService;
    private final DocumentService documentService;
    private final UserRepository userRepository;
    private final DocumentAccessService documentAccessService;
    private final DocumentPermissionService documentPermissionService;
    private final AuditService auditService;

    public DocumentController(DocumentStorageService documentStorageService,
                              DocumentService documentService,
                              UserRepository userRepository,
                              DocumentAccessService documentAccessService,
                              DocumentPermissionService documentPermissionService,
                              AuditService auditService) {
        this.documentStorageService = documentStorageService;
        this.documentService = documentService;
        this.userRepository = userRepository;
        this.documentAccessService = documentAccessService;
        this.documentPermissionService = documentPermissionService;
        this.auditService = auditService;
    }

    /**
     * Renders the documents page showing the upload form and the authenticated user's document list.
     */
    @GetMapping
    public String listDocuments(@AuthenticationPrincipal UserDetails principal, Model model) {
        User owner = resolveAuthenticatedUser(principal);
        List<Document> documents = documentService.getDocumentsByOwnerId(owner.getId());
        List<DocumentListItem> ownedItems = documents.stream()
                .map(doc -> toListItem(owner, doc, "OWNER"))
                .toList();

        List<DocumentListItem> sharedItems = new ArrayList<>();
        for (DocumentPermission permission : documentPermissionService.listSharedWith(owner)) {
            sharedItems.add(toListItem(owner, permission.getDocument(), permission.getPermissionType().name()));
        }

        model.addAttribute("documents", documents);
        model.addAttribute("ownedItems", ownedItems);
        model.addAttribute("sharedItems", sharedItems);
        addNavAttributes(model, principal, owner);
        return "documents";
    }

    /**
     * Handles secure file upload for the authenticated user.
     */
    @PostMapping("/upload")
    public String uploadDocument(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "category", required = false, defaultValue = "") String category,
            @AuthenticationPrincipal UserDetails principal,
            RedirectAttributes redirectAttributes) {

        User owner = resolveAuthenticatedUser(principal);

        try {
            String storedFilename = documentStorageService.store(file);

            Document document = new Document(
                    file.getOriginalFilename(),
                    storedFilename,
                    owner,
                    category.isBlank() ? null : category.trim(),
                    file.getContentType(),
                    file.getSize()
            );

            documentService.saveDocument(document);
            auditService.record(owner, AuditAction.UPLOAD, document, null,
                    AuditOutcome.SUCCESS, "Document uploaded");

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

    @GetMapping("/search")
    public String searchDocuments(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "owner", required = false) String owner,
            @RequestParam(value = "category", required = false) String category,
            @RequestParam(value = "from", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @AuthenticationPrincipal UserDetails principal,
            Model model) {

        User actor = resolveAuthenticatedUser(principal);
        List<Document> results = documentService.searchAuthorized(actor.getId(), name, owner, category, from, to);
        List<DocumentListItem> items = results.stream()
                .map(doc -> toListItem(actor, doc, documentAccessService.isOwner(actor, doc) ? "OWNER"
                        : documentAccessService.findGrant(actor, doc)
                        .map(p -> p.getPermissionType().name())
                        .orElse("SHARED")))
                .toList();

        model.addAttribute("results", items);
        model.addAttribute("name", name);
        model.addAttribute("owner", owner);
        model.addAttribute("category", category);
        model.addAttribute("from", from);
        model.addAttribute("to", to);
        addNavAttributes(model, principal, actor);
        return "search";
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> downloadDocument(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails principal) {

        User actor = resolveAuthenticatedUser(principal);
        Document document = documentService.findDocumentById(id).orElse(null);
        if (document == null || !documentAccessService.canDownload(actor, document)) {
            auditService.record(actor, AuditAction.ACCESS_DENIED, document, null,
                    AuditOutcome.DENIED, "Unauthorized download");
            throw new DocumentAccessDeniedException();
        }

        try {
            Resource resource = documentStorageService.loadAsResource(document.getStoredFilename());
            auditService.record(actor, AuditAction.DOWNLOAD, document, null,
                    AuditOutcome.SUCCESS, "Document downloaded");

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (document.getContentType() != null) {
                try {
                    mediaType = MediaType.parseMediaType(document.getContentType());
                } catch (Exception ignored) {
                    mediaType = MediaType.APPLICATION_OCTET_STREAM;
                }
            }

            ContentDisposition disposition = ContentDisposition.attachment()
                    .filename(document.getName(), StandardCharsets.UTF_8)
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                    .contentType(mediaType)
                    .body(resource);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Download failed for authorized user {} document id {}: {}",
                    actor.getUsername(), id, e.getMessage());
            auditService.record(actor, AuditAction.DOWNLOAD, document, null,
                    AuditOutcome.FAILURE, "Stored file unavailable");
            throw new DocumentAccessDeniedException();
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable("id") Long id,
                           @AuthenticationPrincipal UserDetails principal,
                           Model model) {
        User actor = resolveAuthenticatedUser(principal);
        Document document = requireEditable(actor, id);
        model.addAttribute("document", document);
        addNavAttributes(model, principal, actor);
        return "document-edit";
    }

    @PostMapping("/{id}/edit")
    public String editDocument(@PathVariable("id") Long id,
                               @RequestParam("name") String name,
                               @RequestParam(value = "category", required = false) String category,
                               @AuthenticationPrincipal UserDetails principal,
                               RedirectAttributes redirectAttributes) {
        User actor = resolveAuthenticatedUser(principal);
        Document document;
        try {
            document = requireEditable(actor, id);
        } catch (com.trustvault.exception.DocumentAccessDeniedException e) {
            // Unauthorized edit attempt – audit already recorded in requireEditable
            redirectAttributes.addFlashAttribute("errorMessage", "Access denied.");
            return "redirect:/documents";
        }
        try {
            documentService.updateMetadata(document, name, category);
            auditService.record(actor, AuditAction.UPDATE, document, null,
                    AuditOutcome.SUCCESS, "Metadata updated");
            redirectAttributes.addFlashAttribute("successMessage", "Document updated.");
            return "redirect:/documents";
        } catch (IllegalArgumentException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
            return "redirect:/documents/" + id + "/edit";
        }
    }

    @PostMapping("/{id}/delete")
    public String deleteDocument(@PathVariable("id") Long id,
                                 @AuthenticationPrincipal UserDetails principal,
                                 RedirectAttributes redirectAttributes) {
        User actor = resolveAuthenticatedUser(principal);
        Document document = documentService.findDocumentById(id).orElse(null);
        if (document == null || !documentAccessService.canDelete(actor, document)) {
            auditService.record(actor, AuditAction.ACCESS_DENIED, document, null,
                    AuditOutcome.DENIED, "Unauthorized delete");
            throw new DocumentAccessDeniedException();
        }
        documentService.softDelete(document);
        auditService.record(actor, AuditAction.DELETE, document, null,
                AuditOutcome.SUCCESS, "Document soft-deleted");
        redirectAttributes.addFlashAttribute("successMessage", "Document deleted.");
        return "redirect:/documents";
    }

    private Document requireEditable(User actor, Long id) {
        Document document = documentService.findDocumentById(id).orElse(null);
        if (document == null || !documentAccessService.canEdit(actor, document)) {
            auditService.record(actor, AuditAction.ACCESS_DENIED, document, null,
                    AuditOutcome.DENIED, "Unauthorized edit");
            throw new DocumentAccessDeniedException();
        }
        return document;
    }

    private DocumentListItem toListItem(User actor, Document document, String permissionLabel) {
        boolean owner = documentAccessService.isOwner(actor, document);
        return new DocumentListItem(
                document,
                owner,
                documentAccessService.canDownload(actor, document),
                documentAccessService.canEdit(actor, document),
                documentAccessService.canDelete(actor, document),
                documentAccessService.canManagePermissions(actor, document),
                permissionLabel
        );
    }

    private void addNavAttributes(Model model, UserDetails principal, User actor) {
        model.addAttribute("username", actor.getUsername());
        model.addAttribute("isAdmin", hasAdminRole(principal));
    }

    static boolean hasAdminRole(UserDetails principal) {
        return principal.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }

    /**
     * Resolves the authenticated {@link User} entity from the Spring Security principal.
     * The username is taken from the authenticated session — it is never read from a request parameter.
     */
    private User resolveAuthenticatedUser(UserDetails principal) {
        return userRepository.findByUsername(principal.getUsername())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "Authenticated user not found in database: " + principal.getUsername()));
    }
}
