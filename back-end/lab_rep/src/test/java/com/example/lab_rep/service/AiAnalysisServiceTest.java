package com.example.lab_rep.service;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import reactor.core.publisher.Mono;
import static org.junit.jupiter.api.Assertions.*;

class AiAnalysisServiceTest {
    private ClientResponse response(HttpStatus status, String body) {
        return ClientResponse.create(status).header("Content-Type", "application/json").body(body).build();
    }

    @Test void onlyCallsOllamaEvenWithOldCloudConfiguration() {
        List<String> hosts = new ArrayList<>();
        var env = new MockEnvironment().withProperty("ai.provider-order", "gemini,groq,openrouter,ollama")
                .withProperty("ai.gemini.api-key", "test").withProperty("ai.groq.api-key", "test")
                .withProperty("ai.openrouter.api-key", "test").withProperty("ai.ollama.enabled", "false");
        var client = WebClient.builder().exchangeFunction(request -> {
            hosts.add(request.url().getHost());
            return Mono.just(response(HttpStatus.OK, "{\"done\":true,\"message\":{\"content\":\"Local result\"}}"));
        }).build();
        var result = new AiAnalysisService(env, client).analyze("Report text");
        assertEquals(List.of("localhost"), hosts);
        assertEquals("ollama", result.provider());
        assertEquals("qwen3.5:4b", result.model());
    }

    @Test void missingModelProducesSafeDiagnosticWithoutCloudFallback() {
        List<String> hosts = new ArrayList<>();
        var client = WebClient.builder().exchangeFunction(request -> {
            hosts.add(request.url().getHost());
            return Mono.just(response(HttpStatus.NOT_FOUND, "secret response body"));
        }).build();
        var error = assertThrows(AiAnalysisService.OllamaException.class,
            () -> new AiAnalysisService(new MockEnvironment(), client).analyze("private report"));
        assertTrue(error.getMessage().contains("OLLAMA_NOT_FOUND"));
        assertFalse(error.getMessage().contains("secret"));
        assertEquals(List.of("localhost"), hosts);
    }

    @Test void connectionFailureIdentifiesAddress() {
        var client = WebClient.builder().exchangeFunction(request -> Mono.error(new WebClientRequestException(
            new ConnectException("secret detail"), request.method(), request.url(), request.headers()))).build();
        var error = assertThrows(AiAnalysisService.OllamaException.class,
            () -> new AiAnalysisService(new MockEnvironment(), client).analyze("Report"));
        assertTrue(error.getMessage().contains("OLLAMA_CONNECTION"));
        assertTrue(error.getMessage().contains("localhost:11434"));
        assertFalse(error.getMessage().contains("secret"));
    }

    @Test void timeoutFailsInsteadOfCallingCloud() {
        var env = new MockEnvironment().withProperty("ai.ollama.timeout-seconds", "1");
        var client = WebClient.builder().exchangeFunction(request -> Mono.never()).build();
        var error = assertThrows(AiAnalysisService.OllamaException.class,
            () -> new AiAnalysisService(env, client).analyze("Report"));
        assertTrue(error.getMessage().contains("OLLAMA_TIMEOUT"));
    }

    @Test void emptyOrMalformedOrIncompleteResponseIsNotSuccess() {
        for (String body : List.of("{}", "not json", "{\"done\":true,\"message\":{\"content\":\"\"}}",
                "{\"done\":false,\"message\":{\"content\":\"Partial\"}}")) {
            var client = WebClient.builder().exchangeFunction(request -> Mono.just(response(HttpStatus.OK, body))).build();
            assertThrows(AiAnalysisService.OllamaException.class,
                () -> new AiAnalysisService(new MockEnvironment(), client).analyze("Report"));
        }
    }

    @Test void invalidEndpointNeverLeaksCredentials() {
        var env = new MockEnvironment().withProperty("ai.ollama.base-url", "http://user:secret@localhost:11434");
        var error = assertThrows(AiAnalysisService.OllamaException.class,
            () -> new AiAnalysisService(env).analyze("Report"));
        assertTrue(error.getMessage().contains("OLLAMA_CONFIG"));
        assertFalse(error.getMessage().contains("secret"));
    }

    @Test
    @EnabledIfSystemProperty(named = "ollama.live", matches = "true")
    void realLocalModelAnswersThroughTheApplicationClient() {
        var result = new AiAnalysisService(new MockEnvironment()).analyze(
            "Synthetic connectivity test only. Hemoglobin 14 g/dL, printed reference range 12-16 g/dL. Explain briefly.");
        assertEquals("ollama", result.provider());
        assertFalse(result.text().isBlank());
    }
}
