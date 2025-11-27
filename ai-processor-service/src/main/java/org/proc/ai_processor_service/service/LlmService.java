package org.proc.ai_processor_service.service;

import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
// Keep ChatModel and Prompt usage generic so the code compiles across Spring AI versions
import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.proc.ai_processor_service.model.AnalysisResult;

@Service
public class LlmService {

    private final ChatModel chatModel;

    @Autowired
    public LlmService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public AnalysisResult analyzeText(String text) {
        // Truncate text if too long to avoid token limits
        String truncatedText = text.substring(0, Math.min(text.length(), 10000));

        BeanOutputConverter<AnalysisResult> converter = new BeanOutputConverter<>(AnalysisResult.class);
        String format = converter.getFormat();

        String userPrompt = """
                Analyze the following text and provide a summary, sentiment (POSITIVE, NEGATIVE, NEUTRAL), 
                a list of up to 5 keywords, and a general category for the document.
                
                TEXT:
                %s
                
                %s
                """.formatted(truncatedText, format);

        // Call the AI model
        ChatResponse response = chatModel.call(new Prompt(userPrompt));

        // FIX: Get the actual text content from the generation result
        String responseContent = response.getResult().getOutput().getText();

        // Convert the clean JSON string to AnalysisResult
        return converter.convert(responseContent);
    }
}