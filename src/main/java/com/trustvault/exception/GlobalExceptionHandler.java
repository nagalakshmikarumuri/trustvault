package com.trustvault.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.view.RedirectView;

/**
 * Maps domain access failures to responses that do not leak document existence.
 */
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(DocumentAccessDeniedException.class)
    public Object handleDocumentAccessDenied(DocumentAccessDeniedException ex,
                                             jakarta.servlet.http.HttpServletRequest request,
                                             RedirectAttributes redirectAttributes) {
        String accept = request.getHeader("Accept");
        boolean prefersHtml = accept == null || accept.contains(MediaType.TEXT_HTML_VALUE);
        String method = request.getMethod();
        boolean isDownload = request.getRequestURI() != null && request.getRequestURI().endsWith("/download");

        if ("GET".equalsIgnoreCase(method) && isDownload) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
        }

        if ("GET".equalsIgnoreCase(method) && prefersHtml) {
            redirectAttributes.addFlashAttribute("errorMessage", "Document not found.");
            return new RedirectView("/documents", true);
        }

        if ("POST".equalsIgnoreCase(method)) {
            redirectAttributes.addFlashAttribute("errorMessage", "Document not found.");
            String referer = request.getHeader("Referer");
            if (referer != null && referer.contains("/documents")) {
                return new RedirectView("/documents", true);
            }
            return new RedirectView("/documents", true);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
    }
}
