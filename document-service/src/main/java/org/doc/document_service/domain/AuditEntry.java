package org.doc.document_service.domain;
import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_audit",
       indexes = {
           @Index(name = "idx_audit_documentid", columnList = "document_id"),
           @Index(name = "idx_audit_actor", columnList = "actor_id")
       })
@Data
public class AuditEntry {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "document_id", columnDefinition = "uuid", nullable = false)
    private UUID documentId;

    /**
     * Who performed the action (sub claim).
     */
    @Column(name = "actor_id", nullable = false)
    private String actorId;

    /**
     * Action type: CREATE / COMPLETE_UPLOAD / PROCESS_START / PROCESS_SUCCESS / PROCESS_FAIL / DELETE / QUARANTINE
     */
    @Column(name = "action", nullable = false)
    private String action;

    /**
     * Optional free-form data about the action (jsonb)
     */
    @Column(name = "details", columnDefinition = "jsonb")
    private String details;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors, getters, setters

    public AuditEntry() {}

    public AuditEntry(UUID id, UUID documentId, String actorId, String action, String details) {
        this.id = id;
        this.documentId = documentId;
        this.actorId = actorId;
        this.action = action;
        this.details = details;
    }

    // Getters/setters ...
}
