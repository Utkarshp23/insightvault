package org.doc.document_service.repository;

import java.util.UUID;

import org.doc.document_service.domain.AuditEntry;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditEntry, UUID>{

}
