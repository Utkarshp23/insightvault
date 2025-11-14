package org.doc.document_service.dto;

import java.time.Instant;
import java.util.UUID;

import lombok.Data;

@Data
public class DocumentStatusResponse {

    private UUID documentId;
    private String status;
    private Integer processingProgress; // optional 0-100
    private String error; // if any
    private Instant lastUpdated;
}
