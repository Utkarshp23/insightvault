package org.doc.document_service.domain;

import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "documents",
       indexes = {
           @Index(name = "idx_documents_ownerid", columnList = "owner_id"),
           @Index(name = "idx_documents_status", columnList = "status")
       })
@Data
public class Document {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "filename", nullable = false)
    private String filename;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "size_bytes")
    private Long size;

    /**
     * Storage key in the object store; include document id in the path to avoid collisions.
     */
    @Column(name = "storage_key", nullable = false, unique = true)
    private String storageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private DocumentStatus status;

    @Column(name = "checksum", length = 128)
    private String checksum;

    /**
     * Visibility: private/shared/public (you may enforce values in app layer)
     */
    @Column(name = "visibility", length = 32)
    private String visibility;

    /**
     * Free-form metadata stored as JSON. For Postgres use jsonb; the column here is jsonb.
     * When not using Postgres, store as TEXT and parse in app layer.
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    @Column(name = "request_id", nullable = false)
    private String requestId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    // Constructors, getters, setters, equals/hashCode omitted for brevity — generate with IDE or Lombok

    public Document() {}

    // Convenience constructor for creation
    public Document(UUID id,
                    String ownerId,
                    String tenantId,
                    String filename,
                    String mimeType,
                    String storageKey,
                    DocumentStatus status,
                    String requestId) {
        this.id = id;
        this.ownerId = ownerId;
        this.tenantId = tenantId;
        this.filename = filename;
        this.mimeType = mimeType;
        this.storageKey = storageKey;
        this.status = status;
        this.requestId = requestId;
    }

}
