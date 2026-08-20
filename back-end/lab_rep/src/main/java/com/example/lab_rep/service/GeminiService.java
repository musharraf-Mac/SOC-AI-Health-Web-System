package com.example.lab_rep.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

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

        int maxAttempts = 3;
        long delayMs = 2000;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
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

            } catch (WebClientResponseException e) {
                HttpStatusCode status = e.getStatusCode();
                boolean retryable = status.value() == 503 || status.value() == 429;

                if (retryable && attempt < maxAttempts) {
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    delayMs *= 2;
                    continue;
                }
                return "Analysis failed: " + status.value() + " " + status + " from POST " + e.getRequest().getURI();

            } catch (Exception e) {
                return "Analysis failed: " + e.getMessage();
            }
        }
        return "Analysis failed after retries.";
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