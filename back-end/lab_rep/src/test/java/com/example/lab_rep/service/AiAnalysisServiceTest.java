package com.example.lab_rep.service;

import java.util.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class AiAnalysisServiceTest {
    static final String REPORT = "MCV 80 fL 81 - 101\nMCHC 37.5 g/dL 31.5 - 34.5";
    static final String EXTRACTION = """
        {"measurements":[
          {"name":"MCV","value":"80","unit":"fL","referenceRange":"81 - 101","source":"MCV 80 fL 81 - 101"},
          {"name":"MCHC","value":"37.5","unit":"g/dL","referenceRange":"31.5 - 34.5","source":"MCHC 37.5 g/dL 31.5 - 34.5"}
        ]}
        """;
    private ClientResponse response(HttpStatus status, String body) {
        return ClientResponse.create(status).header("Content-Type", "application/json").body(body).build();
    }
    private String payload(boolean local, String content) {
        try {
            return new ObjectMapper().writeValueAsString(local
                ? Map.of("done", true, "message", Map.of("content", content))
                : Map.of("choices", List.of(Map.of("finish_reason", "stop", "message", Map.of("content", content)))));
        } catch (Exception e) { throw new RuntimeException(e); }
    }
    private MockEnvironment keys() {
        return new MockEnvironment().withProperty("ai.groq.api-key", "test").withProperty("ai.openrouter.api-key", "test");
    }

    @Test void localSuccessUsesVerifiedSummaryAndStopsFallback() {
        List<String> calls = new ArrayList<>();
        var client = WebClient.builder().exchangeFunction(request -> {
            calls.add(request.url().getHost());
            return Mono.just(response(HttpStatus.OK, payload(true, EXTRACTION)));
        }).build();
        var result = new AiAnalysisService(keys(), client).analyze(REPORT);
        assertEquals(List.of("localhost"), calls);
        assertEquals("ollama", result.provider());
        assertEquals("qwen3.5:4b", result.model());
        assertTrue(result.text().contains("Below the printed reference range"));
        assertTrue(result.text().contains("Above the printed reference range"));
        assertFalse(result.text().contains("untrusted"));
    }

    @Test void triesExactOrderIncludingBothGroqModels() {
        List<String> calls = new ArrayList<>();
        var client = WebClient.builder().exchangeFunction(request -> {
            calls.add(request.url().getHost());
            if (calls.size() == 1) return Mono.error(new java.net.ConnectException("unavailable"));
            if (calls.size() == 2) return Mono.just(response(HttpStatus.TOO_MANY_REQUESTS, "sensitive error"));
            if (calls.size() == 3) return Mono.just(response(HttpStatus.SERVICE_UNAVAILABLE, ""));
            return Mono.just(response(HttpStatus.OK, payload(false, EXTRACTION)));
        }).build();
        var result = new AiAnalysisService(keys(), client).analyze(REPORT);
        assertEquals(List.of("localhost", "api.groq.com", "openrouter.ai", "api.groq.com"), calls);
        assertEquals("groq2", result.provider());
        assertEquals("qwen/qwen3.8-27b", result.model());
    }

    @Test void unverifiableExtractionFallsBackToGroq1() {
        var client = WebClient.builder().exchangeFunction(request -> Mono.just(response(HttpStatus.OK,
            payload(request.url().getHost().equals("localhost"), request.url().getHost().equals("localhost")
                ? EXTRACTION.replace("81 - 101", "0 - 999").replace("31.5 - 34.5", "0 - 999") : EXTRACTION)))).build();
        var result = new AiAnalysisService(keys(), client).analyze(REPORT);
        assertEquals("groq1", result.provider());
        assertEquals("openai/gpt-oss-120b", result.model());
    }

    @Test void missingKeysSkipCloudAndGeminiCannotBeUsed() {
        List<String> calls = new ArrayList<>();
        var env = new MockEnvironment().withProperty("ai.gemini.api-key", "secret");
        var client = WebClient.builder().exchangeFunction(request -> {
            calls.add(request.url().getHost());
            return Mono.just(response(HttpStatus.NOT_FOUND, "secret report response"));
        }).build();
        var error = assertThrows(AiAnalysisService.AnalysisException.class,
            () -> new AiAnalysisService(env, client).analyze(REPORT));
        assertEquals(List.of("localhost"), calls);
        assertTrue(error.getMessage().contains("HTTP 404"));
        assertFalse(error.getMessage().contains("secret"));
        env.withProperty("ai.provider-order", "gemini");
        assertThrows(AiAnalysisService.AnalysisException.class, () -> new AiAnalysisService(env, client).analyze(REPORT));
        assertEquals(1, calls.size());
    }

    @Test void timeoutAdvancesToNextProvider() {
        var env = keys().withProperty("ai.ollama.timeout-seconds", "1");
        var client = WebClient.builder().exchangeFunction(request -> request.url().getHost().equals("localhost")
            ? Mono.never() : Mono.just(response(HttpStatus.OK, payload(false, EXTRACTION)))).build();
        assertEquals("groq1", new AiAnalysisService(env, client).analyze(REPORT).provider());
    }

    @Test void malformedEmptyAndTruncatedResponsesCannotSucceed() {
        for (String body : List.of("{}", "not json", payload(true, "{}"),
                "{\"done\":false,\"message\":{\"content\":\"partial\"}}")) {
            var client = WebClient.builder().exchangeFunction(r -> Mono.just(response(HttpStatus.OK, body))).build();
            assertThrows(AiAnalysisService.AnalysisException.class, () -> new AiAnalysisService(new MockEnvironment(), client).analyze(REPORT));
        }
    }

    @Test @EnabledIfSystemProperty(named = "ollama.live", matches = "true")
    void realLocalModelExtractsSyntheticAbnormalValues() {
        var result = new AiAnalysisService(new MockEnvironment().withProperty("ai.provider-order", "ollama")).analyze(REPORT);
        assertEquals("ollama", result.provider());
        assertTrue(result.text().contains("Below the printed reference range"));
        assertTrue(result.text().contains("Above the printed reference range"));
    }
}
