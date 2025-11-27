package org.doc.document_service.repository;

import org.doc.document_service.domain.Document;
import org.doc.document_service.domain.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    // Add custom queries if needed, e.g. findByOwnerId, findByStatus, etc.
     Page<Document> findByOwnerId(String ownerId, Pageable pageable);

     // Find documents by owner, excluding specific status (e.g., DELETED)
    Page<Document> findByOwnerIdAndStatusNot(String ownerId, DocumentStatus status, Pageable pageable);
}
