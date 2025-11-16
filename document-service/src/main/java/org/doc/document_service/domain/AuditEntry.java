package org.doc.document_service.domain;

import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_audit", indexes = {
        @Index(name = "idx_audit_documentid", columnList = "document_id"),
        @Index(name = "idx_audit_actor", columnList = "actor_id")
})
@Data
public class AuditEntry {

    @Id
    @Column(name = "id", columnDefinition = "char(36)")
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID id;

    @Column(name = "document_id", columnDefinition = "char(36)", nullable = false)
     @JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID documentId;

    /**
     * Who performed the action (sub claim).
     */
    @Column(name = "actor_id", nullable = false, length = 255)
    private String actorId;

    /**
     * Action type: CREATE / COMPLETE_UPLOAD / PROCESS_START / PROCESS_SUCCESS /
     * PROCESS_FAIL / DELETE / QUARANTINE
     */
    @Column(name = "action", nullable = false, length = 255)
    private String action;

    /**
     * Optional free-form data about the action (MySQL JSON)
     */
    @Column(name = "details", columnDefinition = "json")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

    public AuditEntry(UUID id, UUID documentId, String actorId, String action, String details) {
        this.id = id;
        this.documentId = documentId;
        this.actorId = actorId;
        this.action = action;
        this.details = details;
    }
    public AuditEntry() {
    }

}
