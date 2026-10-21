package com.trustvault.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentTest
 *
 * Focused unit test verifying Document entity instantiation, field manipulation,
 * User ownership association, and lifecycle callback behavior without requiring a database.
 */
class DocumentTest {

    @Test
    @DisplayName("Should correctly construct Document and expose all properties")
    void shouldConstructDocumentAndExposeProperties() {
        User owner = new User("document_owner", "$2a$10$hashedPasswordSample", Role.OWNER, true);
        owner.setId(42L);

        Document doc = new Document(
                "Financial_Report.pdf",
                "fin_report_uuid_12345.pdf",
                owner,
                "Finance",
                "application/pdf",
                1048576L
        );

        assertThat(doc.getName()).isEqualTo("Financial_Report.pdf");
        assertThat(doc.getStoredFilename()).isEqualTo("fin_report_uuid_12345.pdf");
        assertThat(doc.getOwner()).isSameAs(owner);
        assertThat(doc.getOwner().getId()).isEqualTo(42L);
        assertThat(doc.getCategory()).isEqualTo("Finance");
        assertThat(doc.getContentType()).isEqualTo("application/pdf");
        assertThat(doc.getFileSize()).isEqualTo(1048576L);
        assertThat(doc.isDeleted()).isFalse();
        assertThat(doc.getCreatedAt()).isNotNull();
        assertThat(doc.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("Should support soft deletion flag toggling")
    void shouldToggleSoftDelete() {
        Document doc = new Document();
        assertThat(doc.isDeleted()).isFalse();

        doc.setDeleted(true);
        assertThat(doc.isDeleted()).isTrue();

        doc.setDeleted(false);
        assertThat(doc.isDeleted()).isFalse();
    }

    @Test
    @DisplayName("Lifecycle callback onCreate should initialize createdAt and updatedAt if null")
    void onCreateShouldInitializeTimestamps() {
        Document doc = new Document();
        assertThat(doc.getCreatedAt()).isNull();
        assertThat(doc.getUpdatedAt()).isNull();

        doc.onCreate();

        assertThat(doc.getCreatedAt()).isNotNull();
        assertThat(doc.getUpdatedAt()).isNotNull();
        assertThat(doc.getCreatedAt()).isEqualTo(doc.getUpdatedAt());
    }

    @Test
    @DisplayName("Lifecycle callback onUpdate should update updatedAt timestamp")
    void onUpdateShouldRefreshTimestamp() throws InterruptedException {
        Document doc = new Document();
        doc.onCreate();
        LocalDateTime initialUpdated = doc.getUpdatedAt();

        Thread.sleep(10);
        doc.onUpdate();

        assertThat(doc.getUpdatedAt()).isAfterOrEqualTo(initialUpdated);
    }

    @Test
    @DisplayName("toString should safely include document metadata and owner ID without throwing")
    void toStringShouldBeSafeAndInformative() {
        User owner = new User("alice", "secret_hash", Role.OWNER);
        owner.setId(10L);

        Document doc = new Document("Test.txt", "uuid_123.txt", owner, "General", "text/plain", 512L);
        doc.setId(1L);

        String result = doc.toString();

        assertThat(result)
                .contains("id=1")
                .contains("name='Test.txt'")
                .contains("ownerId=10")
                .contains("deleted=false");
    }
}
