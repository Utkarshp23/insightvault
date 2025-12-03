package org.proc.ai_processor_service.event;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class DocumentAnalyzedEvent {
    private UUID documentId;
    private String ownerId;
    private String filename;
    private String extractedText;
    private String summary;
    private List<String> keywords;
    private String category;
}
