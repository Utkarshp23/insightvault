package org.doc.document_service.storage;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.minio.MinioClient;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClient(StorageProperties props) {
        return MinioClient.builder()
                .endpoint(props.getEndpoint())     // http://localhost:9000
                .credentials(props.getAccessKey(), props.getSecretKey())
                .build();
    }
}
