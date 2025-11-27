package org.proc.ai_processor_service.event;

import lombok.Data;
import java.util.UUID;

@Data
public class DocumentUploadedEvent {
    private UUID documentId;
    private String ownerId;
    private String filename;
    private String mimeType;
    private Long size;
    private String storageKey;
}
