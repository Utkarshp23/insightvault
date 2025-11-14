package org.doc.document_service.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import lombok.Data;

@Data
public class DocumentMetadataResponse {
    private UUID documentId;
    private String ownerId;
    private String tenantId;
    private String filename;
    private String mimeType;
    private Long size;
    private String storageKey;
    private String status;
    private String checksum;
    private String visibility;
    private Map<String, Object> metadata;
    private Instant createdAt;
    private Instant updatedAt;
    private String requestId;
}
