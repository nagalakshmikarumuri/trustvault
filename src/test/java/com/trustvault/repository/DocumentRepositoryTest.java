package com.trustvault.repository;

import com.trustvault.entity.Document;
import com.trustvault.entity.Role;
import com.trustvault.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * DocumentRepositoryTest
 *
 * Focused integration test verifying the custom query methods of DocumentRepository.
 * Uses @Transactional to ensure test isolation with automatic rollback after each test.
 */
@SpringBootTest
@Transactional
class DocumentRepositoryTest {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    @DisplayName("findByOwnerIdAndDeletedFalse should return only active documents for the specified owner")
    void shouldFindActiveDocumentsByOwnerId() {
        User owner1 = userRepository.save(new User("owner_alpha", "$2a$10$hashSample1", Role.OWNER, true));
        User owner2 = userRepository.save(new User("owner_beta", "$2a$10$hashSample2", Role.OWNER, true));

        Document activeDoc1 = new Document("Contract.pdf", "uuid_c1.pdf", owner1, "Legal", "application/pdf", 1024L);
        Document deletedDoc = new Document("Draft.pdf", "uuid_d1.pdf", owner1, "Legal", "application/pdf", 2048L);
        deletedDoc.setDeleted(true);
        Document otherUserDoc = new Document("Other.pdf", "uuid_o1.pdf", owner2, "Legal", "application/pdf", 512L);

        documentRepository.save(activeDoc1);
        documentRepository.save(deletedDoc);
        documentRepository.save(otherUserDoc);

        List<Document> owner1Docs = documentRepository.findByOwnerIdAndDeletedFalse(owner1.getId());

        assertThat(owner1Docs).hasSize(1);
        assertThat(owner1Docs.get(0).getName()).isEqualTo("Contract.pdf");
        assertThat(owner1Docs.get(0).isDeleted()).isFalse();
        assertThat(owner1Docs.get(0).getOwner().getId()).isEqualTo(owner1.getId());
    }

    @Test
    @DisplayName("findByIdAndDeletedFalse should return active document and empty for soft-deleted document")
    void shouldFindByIdAndExcludeDeleted() {
        User owner = userRepository.save(new User("owner_gamma", "$2a$10$hashSample3", Role.OWNER, true));

        Document activeDoc = documentRepository.save(
                new Document("Invoice.pdf", "uuid_inv.pdf", owner, "Finance", "application/pdf", 4096L));
        Document softDeletedDoc = new Document("Archived.pdf", "uuid_arch.pdf", owner, "Finance", "application/pdf", 8192L);
        softDeletedDoc.setDeleted(true);
        softDeletedDoc = documentRepository.save(softDeletedDoc);

        Optional<Document> foundActive = documentRepository.findByIdAndDeletedFalse(activeDoc.getId());
        Optional<Document> foundDeleted = documentRepository.findByIdAndDeletedFalse(softDeletedDoc.getId());

        assertThat(foundActive).isPresent();
        assertThat(foundActive.get().getName()).isEqualTo("Invoice.pdf");
        assertThat(foundDeleted).isEmpty();
    }
}
