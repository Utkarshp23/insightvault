package org.doc.document_service.repository;

import org.doc.document_service.domain.Document;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    // Add custom queries if needed, e.g. findByOwnerId, findByStatus, etc.
}
