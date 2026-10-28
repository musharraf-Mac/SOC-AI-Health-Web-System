package com.example.lab_rep.service;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class AiAnalysisServiceTest {
    @Test void triesLocalFirstAndFallsBackThroughThreeCloudProviders() {
        List<String> hosts = new ArrayList<>();
        var env = new MockEnvironment().withProperty("ai.gemini.api-key", "test")
                .withProperty("ai.groq.api-key", "test").withProperty("ai.openrouter.api-key", "test");
        var client = WebClient.builder().exchangeFunction(request -> {
            hosts.add(request.url().getHost());
            if (hosts.size() == 1) return Mono.error(new RuntimeException("Connection refused"));
            if (hosts.size() == 2) return Mono.just(ClientResponse.create(HttpStatus.TOO_MANY_REQUESTS).build());
            if (hosts.size() == 3) return Mono.just(ClientResponse.create(HttpStatus.SERVICE_UNAVAILABLE).build());
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                    .body("{\"choices\":[{\"message\":{\"content\":\"Explanation\"}}]}").build());
        }).build();
        var result = new AiAnalysisService(env, client).analyze("Hemoglobin 12 g/dL");
        assertEquals(List.of("localhost", "generativelanguage.googleapis.com", "api.groq.com", "openrouter.ai"), hosts);
        assertEquals("openrouter", result.provider());
        assertEquals("Explanation", result.text());
    }

    @Test void localSuccessSkipsCloudAndEmptyResponseFailsOver() {
        var client = WebClient.builder().exchangeFunction(request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header("Content-Type", "application/json").body("{\"message\":{\"content\":\"Local result\"}}").build())).build();
        var result = new AiAnalysisService(new MockEnvironment(), client).analyze("Report text");
        assertEquals("ollama", result.provider());
        assertEquals("qwen3.5:4b", result.model());
    }

    @Test void emptyResponsesAndMissingKeysDoNotBecomeSuccessfulAnalysis() {
        List<String> calls = new ArrayList<>();
        var client = WebClient.builder().exchangeFunction(request -> {
            calls.add(request.url().getHost());
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json").body("{}").build());
        }).build();
        assertThrows(IllegalStateException.class, () -> new AiAnalysisService(new MockEnvironment(), client).analyze("Report"));
        assertEquals(List.of("localhost"), calls);
    }

    @Test void timeoutAdvancesToNextConfiguredProvider() {
        var env = new MockEnvironment().withProperty("ai.ollama.timeout-seconds", "0")
                .withProperty("ai.groq.api-key", "test");
        var client = WebClient.builder().exchangeFunction(request -> {
            if (request.url().getHost().equals("localhost")) return Mono.never();
            return Mono.just(ClientResponse.create(HttpStatus.OK).header("Content-Type", "application/json")
                    .body("{\"choices\":[{\"message\":{\"content\":\"Cloud result\"}}]}").build());
        }).build();
        assertEquals("groq", new AiAnalysisService(env, client).analyze("Report").provider());
    }
}
