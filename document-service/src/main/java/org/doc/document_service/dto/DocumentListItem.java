package org.doc.document_service.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class DocumentListItem {

    private UUID documentId;
    private String filename;
    private String mimeType;
    private Long size;
    private String status;
    private Instant createdAt;
}
