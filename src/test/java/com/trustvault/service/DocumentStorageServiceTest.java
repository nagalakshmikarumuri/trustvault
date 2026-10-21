package com.trustvault.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * DocumentStorageServiceTest
 *
 * Pure unit tests for DocumentStorageService that exercise validation rules and
 * physical storage behavior without requiring a Spring application context or database.
 *
 * Uses JUnit 5's @TempDir to ensure all written files are automatically cleaned up
 * after each test run, preventing cross-test contamination.
 */
class DocumentStorageServiceTest {

    @TempDir
    Path tempStorageDir;

    private DocumentStorageService storageService;

    @BeforeEach
    void setUp() {
        // Instantiate service with the JUnit-managed temp directory.
        storageService = new DocumentStorageService(tempStorageDir.toString());
    }

    // ─── Happy-path tests ───────────────────────────────────────────────────

    @Test
    @DisplayName("store should write file to storage directory and return a UUID-based filename")
    void shouldStoreValidFileAndReturnUuidFilename() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "report.pdf", "application/pdf", "PDF content".getBytes());

        String storedFilename = storageService.store(file);

        assertThat(storedFilename).isNotBlank();
        assertThat(storedFilename).endsWith(".pdf");
        // Stored filename must not equal the original name (it is UUID-based).
        assertThat(storedFilename).doesNotContain("report");

        // The physical file must exist on disk.
        Path writtenFile = tempStorageDir.resolve(storedFilename);
        assertThat(Files.exists(writtenFile)).isTrue();
        assertThat(Files.size(writtenFile)).isEqualTo("PDF content".getBytes().length);
    }

    @Test
    @DisplayName("store should preserve file extension in the generated storage filename")
    void shouldPreserveFileExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "image.PNG", "image/png", new byte[]{1, 2, 3});

        String storedFilename = storageService.store(file);

        // Extension should be lower-cased.
        assertThat(storedFilename).endsWith(".png");
    }

    @Test
    @DisplayName("store should handle a file with no extension gracefully")
    void shouldHandleFileWithNoExtension() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "myfile", "application/octet-stream", new byte[]{42});

        String storedFilename = storageService.store(file);

        assertThat(storedFilename).isNotBlank();
        // UUID only, no trailing dot
        assertThat(storedFilename).doesNotContain(".");
    }

    @Test
    @DisplayName("store should produce unique filenames for two uploads of the same file")
    void shouldProduceUniqueFilenamesForSameContent() throws IOException {
        byte[] content = "hello".getBytes();
        MockMultipartFile file1 = new MockMultipartFile("file", "hello.txt", "text/plain", content);
        MockMultipartFile file2 = new MockMultipartFile("file", "hello.txt", "text/plain", content);

        String name1 = storageService.store(file1);
        String name2 = storageService.store(file2);

        assertThat(name1).isNotEqualTo(name2);
    }

    @Test
    @DisplayName("getStorageRoot should return the configured directory")
    void shouldReturnConfiguredStorageRoot() {
        assertThat(storageService.getStorageRoot()).isEqualTo(tempStorageDir.toAbsolutePath().normalize());
    }

    // ─── Validation rejection tests ─────────────────────────────────────────

    @Test
    @DisplayName("store should reject a null or empty file")
    void shouldRejectEmptyFile() {
        MockMultipartFile emptyFile = new MockMultipartFile(
                "file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> storageService.store(emptyFile))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("empty");
    }

    @Test
    @DisplayName("store should reject a file exceeding 10 MB")
    void shouldRejectFileThatExceedsMaxSize() {
        // 10 MB + 1 byte = just over the limit.
        byte[] oversized = new byte[10 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.pdf", "application/pdf", oversized);

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("10 MB");
    }

    @Test
    @DisplayName("store should reject a file with a blank original filename")
    void shouldRejectBlankFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "   ", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("blank");
    }

    @Test
    @DisplayName("store should reject a filename containing path traversal characters")
    void shouldRejectPathTraversalFilename() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "../etc/passwd", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("store should reject a filename with special shell characters")
    void shouldRejectFilenameWithSpecialCharacters() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "file;rm -rf /.txt", "text/plain", "data".getBytes());

        assertThatThrownBy(() -> storageService.store(file))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("unsafe characters");
    }

    @Test
    @DisplayName("store should accept exactly a 10 MB file (boundary)")
    void shouldAcceptExactlyMaxSizeFile() throws IOException {
        byte[] exact = new byte[10 * 1024 * 1024];
        MockMultipartFile file = new MockMultipartFile(
                "file", "maxsize.bin", "application/octet-stream", exact);

        String storedFilename = storageService.store(file);
        assertThat(storedFilename).isNotBlank();
    }
}
