package com.trustvault.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * DocumentStorageService
 *
 * Handles secure physical storage of uploaded files outside public static/resource directories.
 * Key responsibilities:
 * - Validates file size against a configurable maximum (default 10 MB).
 * - Rejects empty files and unsafe/suspicious original filenames.
 * - Generates a random UUID-based storage filename to prevent directory traversal and name collisions.
 * - Writes the file to an OS-level directory that is never served statically.
 */
@Service
public class DocumentStorageService {

    /** Maximum allowed upload size in bytes (default: 10 MB). Configurable via application.properties. */
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024; // 10 MB

    /**
     * Pattern that a safe original filename must match.
     * Allows alphanumeric characters, dashes, underscores, dots, and spaces.
     * Rejects path separators, null bytes, and other special characters.
     */
    private static final Pattern SAFE_FILENAME_PATTERN =
            Pattern.compile("^[\\w\\s\\-\\.]+$");

    private final Path storageRoot;

    /**
     * Constructs the service and ensures the configured storage directory exists.
     *
     * @param storageDir path to the upload storage directory; configurable via
     *                   {@code trustvault.storage.dir} in application.properties.
     *                   Defaults to a {@code trustvault-uploads} folder inside the
     *                   system temp directory so it is never publicly accessible.
     * @throws RuntimeException if the directory cannot be created
     */
    public DocumentStorageService(
            @Value("${trustvault.storage.dir:#{T(java.lang.System).getProperty('java.io.tmpdir') + '/trustvault-uploads'}}") String storageDir) {
        this.storageRoot = Paths.get(storageDir).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.storageRoot);
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload storage directory: " + this.storageRoot, e);
        }
    }

    /**
     * Stores the given multipart file securely.
     *
     * <p>Validation steps performed before writing:
     * <ol>
     *   <li>Rejects null or empty files.</li>
     *   <li>Rejects files exceeding {@value #MAX_FILE_SIZE_BYTES} bytes.</li>
     *   <li>Sanitizes and validates the original filename; rejects blank names and unsafe characters.</li>
     * </ol>
     *
     * @param file the multipart upload from the browser
     * @return the UUID-based storage filename (without path) that was written to disk
     * @throws IllegalArgumentException if the file fails any validation check
     * @throws IOException              if an I/O error occurs while writing the file
     */
    public String store(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Upload rejected: file is empty or missing.");
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new IllegalArgumentException(
                    "Upload rejected: file size " + file.getSize() + " bytes exceeds the 10 MB limit.");
        }

        String originalFilename = file.getOriginalFilename();
        validateOriginalFilename(originalFilename);

        // Extract the file extension from the original name (lower-cased for safety).
        String extension = extractExtension(originalFilename);

        // Generate a collision-resistant, non-guessable storage filename.
        String storedFilename = UUID.randomUUID().toString() + (extension.isEmpty() ? "" : "." + extension);

        Path targetPath = this.storageRoot.resolve(storedFilename).normalize();

        // Paranoia check: ensure the resolved path stays inside storageRoot.
        if (!targetPath.startsWith(this.storageRoot)) {
            throw new IllegalArgumentException("Upload rejected: resolved path escapes storage root.");
        }

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }

        return storedFilename;
    }

    /**
     * Returns the absolute path to the storage root directory.
     * Intended for internal use or administrative tooling only — never exposed via web routes.
     */
    public Path getStorageRoot() {
        return storageRoot;
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    /**
     * Validates that the original filename is non-blank and contains only safe characters.
     *
     * <p>Validation is applied to the raw original filename string supplied by the browser,
     * before any path-component extraction occurs. This ensures that filenames containing
     * path separators ({@code /}, {@code \}) or other dangerous characters are rejected
     * outright, regardless of how the OS interprets them.
     *
     * @param originalFilename the filename supplied by the browser
     * @throws IllegalArgumentException if the filename is null, blank, or contains unsafe characters
     */
    private void validateOriginalFilename(String originalFilename) {
        if (!StringUtils.hasText(originalFilename)) {
            throw new IllegalArgumentException("Upload rejected: original filename is blank or missing.");
        }

        // Apply the safe-character check directly to the raw original filename.
        // This catches path separators (/ \), null bytes, semicolons, shell metacharacters, etc.
        if (!SAFE_FILENAME_PATTERN.matcher(originalFilename).matches()) {
            throw new IllegalArgumentException(
                    "Upload rejected: filename contains unsafe characters. "
                    + "Only letters, digits, spaces, hyphens, underscores, and dots are allowed.");
        }
    }

    /**
     * Extracts the lowercase file extension from a filename, or returns an empty string
     * if no extension is present.
     *
     * @param filename the original filename
     * @return the lowercase extension without a leading dot, or "" if absent
     */
    private String extractExtension(String filename) {
        if (filename == null) return "";
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) return "";
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
