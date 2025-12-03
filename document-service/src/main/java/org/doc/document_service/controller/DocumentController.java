package org.doc.document_service.controller;

import org.doc.document_service.dto.DocumentCompleteRequest;
import org.doc.document_service.dto.DocumentCreateRequest;
import org.doc.document_service.dto.DocumentCreateResponse;
import org.doc.document_service.dto.DocumentListResponse;
import org.doc.document_service.dto.DocumentMetadataResponse;
import org.doc.document_service.dto.DocumentStatusResponse;
import org.doc.document_service.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/documents")
public class DocumentController {

    @Autowired
    private DocumentService documentService;

    /**
     * Create an upload intent and return a presigned PUT URL.
     *
     * Required scope: SCOPE_doc:create (enforced via Spring Security config).
     *
     * Headers:
     * - X-Request-Id (optional)
     * - Idempotency-Key (optional; recommended)
     */
    @PreAuthorize("hasAuthority('SCOPE_doc:create')")
    @PostMapping
    public ResponseEntity<DocumentCreateResponse> createUploadIntent(
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            Authentication authentication, // <- use Authentication
            @Validated @RequestBody DocumentCreateRequest request) {
        String ownerId = extractOwnerId(authentication);
        DocumentCreateResponse resp = documentService.createUploadIntent(
                ownerId,
                request,
                xRequestId,
                idempotencyKey);

        URI location = URI.create("/documents/" + resp.getDocumentId());
        return ResponseEntity.created(location).body(resp);
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<DocumentMetadataResponse> completeUpload(
            @PathVariable("id") UUID documentId,
            Authentication authentication,
            @Validated @RequestBody DocumentCompleteRequest request) {
        String ownerId = extractOwnerId(authentication);
        DocumentMetadataResponse resp = documentService.completeUpload(documentId, ownerId, request);
        return ResponseEntity.ok(resp);
    }

    /**
     * Lightweight status endpoint for UI polling.
     * GET /documents/{id}/status
     */
    @GetMapping("/{id}/status")
    public ResponseEntity<DocumentStatusResponse> getStatus(
            @PathVariable("id") UUID documentId,
            Authentication authentication) {
        String caller = extractOwnerId(authentication);
        DocumentStatusResponse resp = documentService.getStatus(documentId, caller);
        return ResponseEntity.ok(resp);
    }

    /**
     * Full metadata endpoint.
     * If ?download=true is provided, server will include a presigned GET URL
     * (short-lived).
     * GET /documents/{id}?download=true
     */
    @PreAuthorize("hasAuthority('SCOPE_doc:read')")
    @GetMapping("/{id}")
    public ResponseEntity<DocumentMetadataResponse> getDocument(
            @PathVariable("id") UUID documentId,
            @RequestParam(value = "download", required = false, defaultValue = "false") boolean download,
            Authentication authentication) {
        String caller = extractOwnerId(authentication);
        DocumentMetadataResponse resp = documentService.getMetadata(documentId, caller, download);
        return ResponseEntity.ok(resp);
    }

    /**
     * List documents with pagination.
     * GET /documents?page=0&size=10
     */
    @PreAuthorize("hasAuthority('SCOPE_doc:read')")
    @GetMapping
    public ResponseEntity<DocumentListResponse> listDocuments(
            Authentication authentication,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size) {

        String ownerId = extractOwnerId(authentication);
        DocumentListResponse resp = documentService.listDocuments(ownerId, page, size);
        return ResponseEntity.ok(resp);
    }

    /**
     * Delete a document.
     * DELETE /documents/{id}
     */
    @PreAuthorize("hasAuthority('SCOPE_doc:delete')") 
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteDocument(
            @PathVariable("id") UUID documentId,
            Authentication authentication) {
        String ownerId = extractOwnerId(authentication);
        documentService.deleteDocument(documentId, ownerId);
        return ResponseEntity.noContent().build();
    }

    private String extractOwnerId(Authentication authentication) {
        if (authentication == null) {
            throw new IllegalStateException("No authentication present");
        }

        Object principal = authentication.getPrincipal();
        // Real mode: principal is a Jwt
        if (principal instanceof Jwt jwt) {
            return jwt.getSubject();
        }

        // If principal is a string (DevAuthFilter sets principal to String username)
        if (principal instanceof String) {
            return (String) principal;
        }

        // Fallback: try name
        if (authentication.getName() != null) {
            return authentication.getName();
        }

        throw new IllegalStateException("Unable to determine owner id from authentication");
    }

    /**
     * Internal endpoint for services to update metadata (e.g. AI analysis).
     * In a real system, you'd secure this with a system-to-system token or network restriction.
     */
    @PatchMapping("/{id}/metadata")
    public ResponseEntity<Void> updateMetadata(
            @PathVariable("id") UUID documentId,
            @RequestBody Map<String, Object> metadata) {
        
        // Note: We are bypassing "ownerId" check here because this is a system call.
        // In production, ensure this endpoint is not exposed to public internet 
        // or require a special scope/role like "SCOPE_system".
        
        documentService.updateMetadata(documentId, metadata);
        return ResponseEntity.noContent().build();
    }

}
