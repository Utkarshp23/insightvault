package org.doc.document_service.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentUploadedEvent {

    private UUID documentId;
    private String ownerId;
    private String filename;
    private String mimeType;
    private Long size;
    private String storageKey;
}
