package org.doc.document_service.domain;
import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "idempotency_keys",
       indexes = {
           @Index(name = "idx_idempotency_key_owner", columnList = "idempotency_key, owner_id")
       })
@Data
public class IdempotencyKey {

    @Id
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(name = "owner_id", nullable = false)
    private String ownerId;

    /**
     * The document id returned for this idempotent request
     */
    @Column(name = "document_id", columnDefinition = "uuid")
    private UUID documentId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    // Constructors, getters, setters

    public IdempotencyKey() {}

    public IdempotencyKey(UUID id, String idempotencyKey, String ownerId, UUID documentId, Instant expiresAt) {
        this.id = id;
        this.idempotencyKey = idempotencyKey;
        this.ownerId = ownerId;
        this.documentId = documentId;
        this.expiresAt = expiresAt;
    }

    // Getters/setters ...
}
