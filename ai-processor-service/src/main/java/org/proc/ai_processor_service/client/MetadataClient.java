package org.proc.ai_processor_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.proc.ai_processor_service.model.AnalysisResult;
import java.util.Map;
import java.util.UUID;

@Component
public class MetadataClient {

    private final RestClient restClient;

    public MetadataClient(@Value("${document-service.url}") String docServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(docServiceUrl)
                .build();
    }

    public void updateDocumentMetadata(UUID documentId, AnalysisResult analysis) {
        Map<String, Object> updates = Map.of(
            "summary", analysis.summary(),
            "sentiment", analysis.sentiment(),
            "keywords", analysis.keywords(),
            "category", analysis.category(),
            "ai_processed", true
        );
        System.out.println("Would call Document Service to update metadata for " + documentId + ": " + updates);
    }
}
