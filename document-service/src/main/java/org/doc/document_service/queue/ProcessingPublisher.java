package org.doc.document_service.queue;

import java.util.UUID;

public interface ProcessingPublisher {
    /**
     * Publish a processing job (documentId + storageKey + requestId)
     */
    void publishProcessingJob(UUID documentId, String storageKey, String requestId);
}
