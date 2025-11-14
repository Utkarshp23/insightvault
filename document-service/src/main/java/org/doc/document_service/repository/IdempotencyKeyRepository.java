package org.doc.document_service.repository;

import org.doc.document_service.domain.IdempotencyKey;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface IdempotencyKeyRepository extends JpaRepository<IdempotencyKey, UUID> {

    Optional<IdempotencyKey> findByIdempotencyKeyAndOwnerId(String idempotencyKey, String ownerId);
}
