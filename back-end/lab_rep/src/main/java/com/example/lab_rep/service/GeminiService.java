package com.example.lab_rep.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeminiService {
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.model:gemini-flash-latest}")
    private String model;
    
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    
    public GeminiService() {
        this.webClient = WebClient.builder().build();
        this.objectMapper = new ObjectMapper();
    }
    
    public String analyzeLabReport(String extractedText) {
        if (extractedText == null || extractedText.trim().isEmpty()) {
            return "No text to analyze. The PDF may be empty or scanned.";
        }

        String prompt = """
            You are a medical lab report analyzer. 
            Analyze the following lab report and provide:
            1. Key findings
            2. Any abnormal values
            3. Recommendations
            
            Lab Report:
            %s
            """.formatted(extractedText.substring(0, Math.min(extractedText.length(), 15000)));
        
        try {
            // Escape special characters for JSON
            String escapedPrompt = prompt.replace("\"", "\\\"").replace("\n", " ");
            
            String requestBody = """
                {
                    "contents": [{
                        "parts": [{
                            "text": "%s"
                        }]
                    }]
                }
                """.formatted(escapedPrompt);
            
            String url = "https://generativelanguage.googleapis.com/v1beta/models/" 
                         + model + ":generateContent?key=" + apiKey;
            
            String response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            
            if (response != null) {
                return parseGeminiResponse(response);
            }
            return "No response from Gemini API.";
            
        } catch (Exception e) {
            return "Analysis failed: " + e.getMessage();
        }
    }
    
    private String parseGeminiResponse(String response) {
        try {
            JsonNode root = objectMapper.readTree(response);
            JsonNode candidates = root.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }
            return "Could not parse Gemini response.";
        } catch (Exception e) {
            return "Error parsing response: " + e.getMessage();
        }
    }
}
