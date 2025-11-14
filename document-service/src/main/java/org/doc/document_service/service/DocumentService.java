package org.doc.document_service.service;

import org.doc.document_service.domain.Document;
import org.doc.document_service.domain.DocumentStatus;
import org.doc.document_service.domain.IdempotencyKey;
import org.doc.document_service.dto.DocumentCreateRequest;
import org.doc.document_service.dto.DocumentCreateResponse;
import org.doc.document_service.mapper.DocumentMapper;
import org.doc.document_service.mapper.JsonMapper;
import org.doc.document_service.repository.DocumentRepository;
import org.doc.document_service.repository.IdempotencyKeyRepository;
import org.doc.document_service.storage.PresignedUrlResponse;
import org.doc.document_service.storage.StorageService;
import org.doc.document_service.util.StorageKeyUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.netflix.discovery.converters.Auto;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentService {

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private StorageService storageService;

    @Autowired
    private DocumentMapper documentMapper;

    @Autowired
    private JsonMapper jsonMapper;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    // Configure TTL for presigned URL in seconds (could come from application.properties)
    private final long presignedTtlSeconds = 15 * 60; // 15 minutes

    // Configure idempotency TTL (how long to remember an idempotency key)
    private final long idempotencyTtlSeconds = 24 * 3600; // 24 hours

    
    /**
     * Create an upload intent. Handles Idempotency-Key semantics.
     *
     * @param ownerId the JWT subject
     * @param request DTO with filename, mimeType, size etc.
     * @param requestId correlation id (X-Request-Id)
     * @param idempotencyKey optional idempotency key
     */
    @Transactional
    public DocumentCreateResponse createUploadIntent(String ownerId,
                                                     DocumentCreateRequest request,
                                                     String requestId,
                                                     String idempotencyKey) {
        // 1) If idempotencyKey present, attempt to return existing mapping
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            Optional<IdempotencyKey> existing = idempotencyKeyRepository
                    .findByIdempotencyKeyAndOwnerId(idempotencyKey, ownerId);
            if (existing.isPresent()) {
                IdempotencyKey key = existing.get();
                if (key.getExpiresAt() != null && key.getExpiresAt().isAfter(Instant.now())) {
                    UUID docId = key.getDocumentId();
                    // Return the existing document create response if it's present
                    Document doc = documentRepository.findById(docId)
                            .orElseThrow(() -> new IllegalStateException("Document referenced by idempotency key missing"));
                    // Map entity to response and regenerate a fresh presigned URL if needed
                    DocumentCreateResponse resp = documentMapper.toCreateResponse(doc);
                    PresignedUrlResponse presigned = storageService.generatePresignedPutUrl(doc.getStorageKey(), presignedTtlSeconds);
                    resp.setPresignedUrl(presigned.getUrl());
                    resp.setPresignedUrlExpiresAt(presigned.getExpiresAt());
                    resp.setTtlSeconds(presigned.getTtlSeconds());
                    return resp;
                } else {
                    // expired idempotency entry → fallthrough and create fresh
                }
            }
        }

        // 2) Basic validations (size, mime) - example checks; replace with real policy checks
        if (request.getSize() != null && request.getSize() > 100L * 1024 * 1024) { // example 100 MB max
            throw new IllegalArgumentException("file size exceeds allowed limit");
        }
        // TODO: add mime type whitelist checks, quota checks etc.

        // 3) Create Document entity (provisional)
        UUID documentId = UUID.randomUUID();
        String tenantId = null; // if you decode tenant from JWT claims, set it here
        String storageKey = StorageKeyUtil.generateStorageKey(tenantId, documentId, request.getFilename());

        Document doc = new Document();
        doc.setId(documentId);
        doc.setOwnerId(ownerId);
        doc.setTenantId(tenantId);
        doc.setFilename(request.getFilename());
        doc.setMimeType(request.getMimeType());
        doc.setSize(request.getSize());
        doc.setStorageKey(storageKey);
        doc.setStatus(DocumentStatus.UPLOADING);
        doc.setChecksum(null);
        doc.setVisibility(request.getVisibility());
        // store metadata as JSON string via mapper or directly
        doc.setMetadata(jsonMapper.toJson(request.getMetadata()));
        doc.setRequestId(requestId != null ? requestId : UUID.randomUUID().toString());
        documentRepository.save(doc);

        // 4) Persist idempotency mapping if provided
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            IdempotencyKey key = new IdempotencyKey();
            key.setId(UUID.randomUUID());
            key.setIdempotencyKey(idempotencyKey);
            key.setOwnerId(ownerId);
            key.setDocumentId(documentId);
            key.setExpiresAt(Instant.now().plus(idempotencyTtlSeconds, ChronoUnit.SECONDS));
            idempotencyKeyRepository.save(key);
        }

        // 5) Generate presigned PUT URL
        PresignedUrlResponse presigned = storageService.generatePresignedPutUrl(storageKey, presignedTtlSeconds);

        // 6) Map to create-response and attach presigned URL info
        DocumentCreateResponse response = documentMapper.toCreateResponse(doc);
        response.setPresignedUrl(presigned.getUrl());
        response.setPresignedUrlExpiresAt(presigned.getExpiresAt());
        response.setTtlSeconds(presigned.getTtlSeconds());

        // Return response
        return response;
    }
}
