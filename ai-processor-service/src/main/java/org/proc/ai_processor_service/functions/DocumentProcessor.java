package org.proc.ai_processor_service.functions;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
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

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }

    @Bean
    public Consumer<DocumentUploadedEvent> processDocument(MinioClient minioClient) {
        return event -> {
            System.out.println(">>> Received Event: " + event);
            
            try {
                // 1. Download from MinIO
                InputStream stream = minioClient.getObject(
                        GetObjectArgs.builder()
                                .bucket(bucket)
                                .object(event.getStorageKey())
                                .build()
                );

                // 2. Extract Text using Apache Tika
                Tika tika = new Tika();
                Metadata metadata = new Metadata();
                String extractedText = tika.parseToString(stream, metadata);

                
                System.out.println("--- Extracted Text Start ---");
                System.out.println(extractedText.substring(0, Math.min(extractedText.length(), 500)) + "...");
                System.out.println("--- Extracted Text End ---");
               
                // 3. TODO: Save extracted text or call AI Model
                // For now, we just log it. Next step is LLM integration.
                AnalysisResult analysis = llmService.analyzeText(extractedText);
                System.out.println(">>> Analysis Result: " + analysis);
                stream.close();

            } catch (Exception e) {
                System.err.println("Error processing document: " + e.getMessage());
                e.printStackTrace();
                // In a real app, you might want to send this to a Dead Letter Queue (DLQ)
            }
        };
    }
}