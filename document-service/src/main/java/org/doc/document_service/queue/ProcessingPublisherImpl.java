package org.doc.document_service.queue;

import org.doc.document_service.event.DocumentUploadedEvent;
import org.doc.document_service.repository.DocumentRepository;
import org.doc.document_service.domain.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProcessingPublisherImpl implements ProcessingPublisher {
    @Autowired
    private StreamBridge streamBridge;

    @Autowired
    private DocumentRepository documentRepository;

    @Override
    @Transactional(readOnly = true)
    public void publishProcessingJob(UUID documentId, String storageKey, String requestId) {
        // 1. Fetch details needed for the event
        Document doc = documentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Document not found for event publishing"));

        // 2. Create your designed event object
        DocumentUploadedEvent event = new DocumentUploadedEvent(
                doc.getId(),
                doc.getOwnerId(),
                doc.getFilename(),
                doc.getMimeType(),
                doc.getSize(),
                doc.getStorageKey());

        System.out.println("Publishing event to RabbitMQ for doc: " + documentId);

        // 3. Send to the exchange "documentUploaded-out-0"
        // Spring Cloud Stream will map this name to the actual RabbitMQ exchange
        streamBridge.send("documentUploaded-out-0", event);
    }
}
