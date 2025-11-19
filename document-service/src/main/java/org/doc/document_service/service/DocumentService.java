package org.doc.document_service.service;

import org.checkerframework.checker.units.qual.A;
import org.doc.document_service.domain.AuditEntry;
import org.doc.document_service.domain.Document;
import org.doc.document_service.domain.DocumentStatus;
import org.doc.document_service.domain.IdempotencyKey;
import org.doc.document_service.dto.DocumentCompleteRequest;
import org.doc.document_service.dto.DocumentCreateRequest;
import org.doc.document_service.dto.DocumentCreateResponse;
import org.doc.document_service.dto.DocumentMetadataResponse;
import org.doc.document_service.dto.DocumentStatusResponse;
import org.doc.document_service.mapper.DocumentMapper;
import org.doc.document_service.mapper.JsonMapper;
import org.doc.document_service.queue.ProcessingPublisher;
import org.doc.document_service.repository.AuditRepository;
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
import java.util.HashMap;
import java.util.Map;
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

    // @Autowired(required = false)
    // private ProcessingPublisher processingPublisher;

    @Autowired
    private AuditRepository auditRepository;

    @Autowired
    private IdempotencyKeyRepository idempotencyKeyRepository;

    // Configure TTL for presigned URL in seconds (could come from
    // application.properties)
    private final long presignedTtlSeconds = 15 * 60; // 15 minutes

    // Configure idempotency TTL (how long to remember an idempotency key)
    private final long idempotencyTtlSeconds = 24 * 3600; // 24 hours

    /**
     * Create an upload intent. Handles Idempotency-Key semantics.
     *
     * @param ownerId        the JWT subject
     * @param request        DTO with filename, mimeType, size etc.
     * @param requestId      correlation id (X-Request-Id)
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
                            .orElseThrow(
                                    () -> new IllegalStateException("Document referenced by idempotency key missing"));
                    // Map entity to response and regenerate a fresh presigned URL if needed
                    DocumentCreateResponse resp = documentMapper.toCreateResponse(doc);
                    PresignedUrlResponse presigned = storageService.generatePresignedPutUrl(doc.getStorageKey(),
                            presignedTtlSeconds);
                    resp.setPresignedUrl(presigned.getUrl());
                    resp.setPresignedUrlExpiresAt(presigned.getExpiresAt());
                    resp.setTtlSeconds(presigned.getTtlSeconds());
                    return resp;
                } else {
                    // expired idempotency entry → fallthrough and create fresh
                }
            }
        }

        // 2) Basic validations (size, mime) - example checks; replace with real policy
        // checks
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

    /**
     * Mark upload as complete. Idempotent: repeated calls are fine.
     */
    @Transactional
    public DocumentMetadataResponse completeUpload(UUID documentId, String callerSub, DocumentCompleteRequest req) {

        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("document not found"));

        // Authorization: owner or admin. Adjust per your roles
        if (!callerSub.equals(doc.getOwnerId()) && !isAdmin(callerSub)) {
            throw new SecurityException("Not allowed to complete upload for this document");
        }

        // If already processed beyond 'uploading' - be idempotent
        if (doc.getStatus() != null && doc.getStatus() != DocumentStatus.UPLOADING) {
            // If checksum provided and DB doesn't have it, update it
            boolean changed = false;
            if (req.getChecksum() != null
                    && (doc.getChecksum() == null || !doc.getChecksum().equals(req.getChecksum()))) {
                doc.setChecksum(req.getChecksum());
                changed = true;
            }
            if (req.getSize() != null && (doc.getSize() == null || !doc.getSize().equals(req.getSize()))) {
                doc.setSize(req.getSize());
                changed = true;
            }
            if (changed) {
                documentRepository.save(doc);
                // record audit
                Map<String, Object> details = new HashMap<>();
                details.put("notes", "idempotent update");
                if (req.getSize() != null)
                    details.put("size", req.getSize());
                if (req.getChecksum() != null)
                    details.put("checksum", req.getChecksum());

                AuditEntry audit = new AuditEntry(null, doc.getId(), callerSub, "COMPLETE_UPLOAD (idempotent update)",
                        jsonMapper.toJson(details));
                auditRepository.save(audit);

            }
            return documentMapper.toMetadataResponse(doc, jsonMapper);
        }

        // At this point status == UPLOADING, proceed to verify object exists
        boolean exists = storageService.exists(doc.getStorageKey());
        if (!exists) {
            throw new IllegalStateException("Uploaded object not found in storage; upload may have failed");
        }

        // Update size/checksum if provided
        if (req.getChecksum() != null) {
            doc.setChecksum(req.getChecksum());
        }
        if (req.getSize() != null) {
            doc.setSize(req.getSize());
        }

        doc.setStatus(DocumentStatus.UPLOADED);
        doc.setUpdatedAt(Instant.now());
        documentRepository.save(doc);

        // Audit the completion
        AuditEntry audit = new AuditEntry();
        audit.setDocumentId(doc.getId());
        audit.setActorId(callerSub);
        audit.setAction("COMPLETE_UPLOAD");
        Map<String, Object> details2 = new HashMap<>();
        if (req.getSize() != null)
            details2.put("size", req.getSize());
        if (req.getChecksum() != null)
            details2.put("checksum", req.getChecksum());
        details2.put("storageKey", doc.getStorageKey());

        audit.setDetails(jsonMapper.toJson(details2));
        auditRepository.save(audit);

        // Publish processing job (worker consumes and does scanning/OCR/indexing)
        // processingPublisher.publishProcessingJob(doc.getId(), doc.getStorageKey(),
        // doc.getRequestId());

        return documentMapper.toMetadataResponse(doc, jsonMapper);
    }

    /**
     * Return lightweight status (id, status, progress, lastUpdated).
     */
    @Transactional(readOnly = true)
    public DocumentStatusResponse getStatus(UUID documentId, String callerSub) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("document not found"));

        // Authorization: owner or admin
        if (!callerSub.equals(doc.getOwnerId()) && !isAdmin(callerSub)) {
            throw new SecurityException("Not allowed to view status of this document");
        }

        DocumentStatusResponse resp = documentMapper.toStatusResponse(doc);
        // Optionally fill processingProgress or error fields if you track them in DB
        return resp;
    }

    /**
     * Return full metadata. If download == true, include a presigned GET URL
     * (short-lived).
     */
    @Transactional(readOnly = true)
    public DocumentMetadataResponse getMetadata(UUID documentId, String callerSub, boolean download) {
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("document not found"));

        // Authorization: owner or admin or allowed by ACL (extend here)
        if (!callerSub.equals(doc.getOwnerId()) && !isAdmin(callerSub)) {
            throw new SecurityException("Not allowed to view this document");
        }

        DocumentMetadataResponse resp = documentMapper.toMetadataResponse(doc, jsonMapper);

        if (download) {
            // Only provide download if document is in a state that allows it
            if (doc.getStatus() == DocumentStatus.PROCESSED || doc.getStatus() == DocumentStatus.UPLOADED) {
                // ask storage for a presigned GET URL
                PresignedUrlResponse presigned = storageService.generatePresignedGetUrl(doc.getStorageKey(),
                        presignedTtlSeconds);

                resp.setPresignedGetUrl(presigned.getUrl());
                resp.setPresignedGetUrlExpiresAt(presigned.getExpiresAt());
                resp.setPresignedGetTtlSeconds(presigned.getTtlSeconds());
            } else {
                // If not ready for download, you may choose to return null or throw a 409
                // Here we just leave presigned fields null and client can show message.
            }
        }

        return resp;
    }

    private boolean isAdmin(String callerSub) {
        // placeholder — check if caller is an admin, e.g. by querying user service or
        // checking roles from token if available
        return false;
    }
}
