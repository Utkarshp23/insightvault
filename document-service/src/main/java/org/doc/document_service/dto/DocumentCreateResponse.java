package org.doc.document_service.dto;

import java.time.Instant;
import java.util.UUID;
import lombok.Data;

@Data
public class DocumentCreateResponse {

    private UUID documentId;
    private String storageKey;

    /**
     * Presigned PUT URL returned to client
     */
    private String presignedUrl;

    /**
     * ISO instant when presigned URL expires (optional)
     */
    private Instant presignedUrlExpiresAt;

    private Long ttlSeconds;

    private String status;
}
