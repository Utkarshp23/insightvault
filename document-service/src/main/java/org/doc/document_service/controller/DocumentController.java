package org.doc.document_service.controller;

import org.doc.document_service.dto.DocumentCreateRequest;
import org.doc.document_service.dto.DocumentCreateResponse;
import org.doc.document_service.service.DocumentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

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
    @PostMapping
    public ResponseEntity<DocumentCreateResponse> createUploadIntent(
            @RequestHeader(value = "X-Request-Id", required = false) String xRequestId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal Jwt jwt,
            @Validated @RequestBody DocumentCreateRequest request) {
        // Extract ownerId from JWT 'sub' claim
        String ownerId = jwt.getSubject();

        DocumentCreateResponse resp = documentService.createUploadIntent(
                ownerId,
                request,
                xRequestId,
                idempotencyKey);

        // Return 201 Created with location header pointing to document metadata
        // endpoint
        URI location = URI.create("/documents/" + resp.getDocumentId());
        return ResponseEntity.created(location).body(resp);
    }
}
