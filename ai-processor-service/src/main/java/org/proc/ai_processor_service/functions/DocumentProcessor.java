package org.proc.ai_processor_service.functions;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;

import org.proc.ai_processor_service.client.MetadataClient;
import org.proc.ai_processor_service.event.DocumentAnalyzedEvent;
import org.proc.ai_processor_service.event.DocumentUploadedEvent;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.proc.ai_processor_service.service.LlmService;
import org.proc.ai_processor_service.model.AnalysisResult;
import org.springframework.beans.factory.annotation.Autowired;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.function.Function;

@Configuration
public class DocumentProcessor {

    @Value("${storage.bucket}")
    private String bucket;

    @Value("${storage.endpoint}")
    private String endpoint;

    @Value("${storage.accessKey}")
    private String accessKey;

    @Value("${storage.secretKey}")
    private String secretKey;

    @Autowired
    private LlmService llmService;

    @Autowired
    private MetadataClient metadataClient;

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public Function<DocumentUploadedEvent, DocumentAnalyzedEvent> processDocument(MinioClient minioClient) {
        return event -> {
            System.out.println(">>> Received Event: " + event);

            try {
                // 1. Download
                InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(event.getStorageKey())
                                .build());

                // 2. Extract
                Tika tika = new Tika();
                Metadata metadata = new Metadata();
                String extractedText = tika.parseToString(stream, metadata);
                stream.close();

                System.out.println("Text extracted (" + extractedText.length() + " chars)");

                // 3. Analyze
                AnalysisResult analysis = llmService.analyzeText(extractedText);
                System.out.println(">>> Analysis Result: " + analysis);

                // 4. Save (Call Document Service)
                metadataClient.updateDocumentMetadata(event.getDocumentId(), analysis);
                // 4. Create Event for Search Service
                DocumentAnalyzedEvent searchEvent = DocumentAnalyzedEvent.builder()
                        .documentId(event.getDocumentId())
                        .ownerId(event.getOwnerId())
                        .filename(event.getFilename())
                        .extractedText(extractedText) // The full text!
                        .summary(analysis.summary())
                        .keywords(analysis.keywords())
                        .category(analysis.category())
                        .build();

                System.out.println(">>> Publishing Search Event for: " + event.getFilename());
                return searchEvent; // This goes to RabbitMQ "search-exchange"
                
            } catch (Exception e) {
                System.err.println("Error processing document: " + e.getMessage());
                e.printStackTrace();
                return null;
            }
        };
    }
}