package org.doc.document_service.domain;
import jakarta.persistence.*;
import lombok.Data;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;

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
    @Column(name = "id", columnDefinition = "char(36)")
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, length = 512)
    private String idempotencyKey;

    @Column(name = "owner_id", nullable = false, length = 255)
    private String ownerId;

    /**
     * The document id returned for this idempotent request
     */
    @Column(name = "document_id", columnDefinition = "char(36)")
    private UUID documentId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, columnDefinition = "datetime(6)")
    private Instant createdAt;

    @Column(name = "expires_at", columnDefinition = "datetime(6)")
    private Instant expiresAt;

    @PrePersist
    public void ensureId() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
