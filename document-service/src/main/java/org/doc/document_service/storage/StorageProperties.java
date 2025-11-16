package org.doc.document_service.storage;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;


@Component
@ConfigurationProperties(prefix = "storage")
@Data
public class StorageProperties {
    private String bucket;
    private String endpoint;
    private String accessKey;
    private String secretKey;
}
