package org.doc.document_service.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.*;
import java.util.Map;
import lombok.Data;

@Data
public class DocumentCreateRequest {
    @NotBlank(message = "filename is required")
    @Size(max = 1024, message = "filename too long")
    private String filename;

    @JsonProperty("mimeType")
    @Pattern(regexp = "^[\\w\\-\\.]+\\/[\\w\\-\\.\\+]+$", message = "invalid mime type")
    private String mimeType;

    @Positive(message = "size must be positive")
    private Long size;

    @Pattern(regexp = "^(private|shared|public)?$", message = "invalid visibility")
    private String visibility;

    private Map<String, Object> metadata;
}
