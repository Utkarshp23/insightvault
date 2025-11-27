package org.proc.ai_processor_service.model;

import java.util.List;

public record AnalysisResult(
    String summary,
    String sentiment,
    List<String> keywords,
    String category
) {}
