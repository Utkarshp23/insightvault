package org.srh.search_service.consumer;

import org.srh.search_service.model.DocumentIndex;
import org.srh.search_service.repo.DocumentSearchRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

@Configuration
public class SearchIndexer {

    @Autowired
    private DocumentSearchRepository repository;

    @Bean
    public Consumer<DocumentAnalyzedEvent> indexDocument() {
        return event -> {
            System.out.println("Indexing document: " + event.getFilename());
            
            DocumentIndex doc = new DocumentIndex();
            doc.setId(event.getDocumentId());
            doc.setOwnerId(event.getOwnerId());
            doc.setFilename(event.getFilename());
            doc.setContent(event.getExtractedText());
            doc.setSummary(event.getSummary());
            doc.setKeywords(event.getKeywords());
            doc.setCategory(event.getCategory());

            repository.save(doc);
            System.out.println("Document indexed successfully!");
        };
    }

    // Needs to match the event published by AI Processor
    @lombok.Data
    public static class DocumentAnalyzedEvent {
        private UUID documentId;
        private String ownerId;
        private String filename;
        private String extractedText;
        private String summary;
        private List<String> keywords;
        private String category;
    }
}
