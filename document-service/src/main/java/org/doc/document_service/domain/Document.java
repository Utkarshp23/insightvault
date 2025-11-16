package org.doc.document_service.domain;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documents",
       indexes = {
           @Index(name = "idx_documents_ownerid", columnList = "owner_id"),
           @Index(name = "idx_documents_status", columnList = "status")
       })
@Data
public class Document {

    @Id
    @Column(name = "id", columnDefinition = "char(36)")
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID id;

    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    @Column(name = "tenant_id", length = 255)
    private String tenantId;

    @Column(name = "filename", nullable = false, length = 1024)
    private String filename;

    @Column(name = "mime_type", length = 255)
    private String mimeType;

    @Column(name = "size_bytes")
    private Long size;

    /**
     * Storage key in the object store; include document id in the path to avoid collisions.
     */
    @Column(name = "storage_key", nullable = false, unique = true, length = 1024)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DocumentStatus status;

    @Column(name = "checksum", length = 128)
    private String checksum;

    /**
     * Visibility: private/shared/public (enforce in app layer)
     */
    @Column(name = "visibility", length = 32)
    private String visibility;

    /**
     * Free-form metadata stored as JSON (MySQL JSON type)
     * Stored as String in the entity; convert to/from Map in service/mapper.
     */
    @Column(name = "metadata", columnDefinition = "json")
    private String metadata;

    @Column(name = "request_id", nullable = false, length = 255)
    private String requestId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", columnDefinition = "datetime(6)")
    private Instant updatedAt;

    // Auto-generate id if not set before persistence
    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
