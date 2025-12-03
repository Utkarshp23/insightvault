package org.proc.ai_processor_service.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.proc.ai_processor_service.model.AnalysisResult;
import org.proc.ai_processor_service.security.TokenManager;

import java.util.Map;
import java.util.UUID;

@Component
public class MetadataClient {

    private final RestClient restClient;
    private final TokenManager tokenManager;

    public MetadataClient(
            @Value("${document-service.url}") String docServiceUrl,
            TokenManager tokenManager) {

        this.tokenManager = tokenManager;
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
                "ai_processed", true);
        System.out.println("Would call Document Service to update metadata for " + documentId + ": " + updates);
        String token = tokenManager.getAccessToken();
         System.out.println("***************Using token: " + token);   
        try {
            restClient.patch()
                    .uri("/documents/{id}/metadata", documentId)
                    .header("Authorization", "Bearer " + token)
                    .body(updates)
                    .retrieve()
                    .toBodilessEntity();
            
            System.out.println("Metadata updated successfully.");
        } catch (Exception e) {
            System.err.println("Failed to update metadata: " + e.getMessage());
        }
    }
}
