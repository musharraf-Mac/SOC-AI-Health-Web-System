package com.example.lab_rep.service;

import com.google.auth.oauth2.GoogleCredentials;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

/** Cloud Tasks persists work independently of the upload request/Cloud Run instance. */
@Service
public class ReportTaskQueue {
    private final Environment env;
    public ReportTaskQueue(Environment env) { this.env = env; }
    public boolean enabled() { return !env.getProperty("reports.tasks.queue", "").isBlank(); }
    public void enqueue(String reportId) throws Exception {
        String queue = env.getRequiredProperty("reports.tasks.queue");
        String url = env.getRequiredProperty("reports.tasks.service-url");
        String account = env.getRequiredProperty("reports.tasks.service-account");
        var credentials = GoogleCredentials.getApplicationDefault().createScoped(List.of("https://www.googleapis.com/auth/cloud-platform"));
        credentials.refreshIfExpired();
        var request = Map.of("task", Map.of("dispatchDeadline", "300s", "httpRequest", Map.of(
            "httpMethod", "POST", "url", url + "/internal/reports/" + reportId + "/process",
            "headers", Map.of("Content-Type", "application/json", "X-API-KEY", env.getRequiredProperty("api.key")),
            "body", Base64.getEncoder().encodeToString("{}".getBytes(StandardCharsets.UTF_8)),
            "oidcToken", Map.of("serviceAccountEmail", account, "audience", url))));
        WebClient.create().post().uri("https://cloudtasks.googleapis.com/v2/" + queue + "/tasks")
            .headers(h -> h.setBearerAuth(credentials.getAccessToken().getTokenValue()))
            .header("Content-Type", "application/json").bodyValue(new ObjectMapper().writeValueAsString(request))
            .retrieve().toBodilessEntity().block(Duration.ofSeconds(15));
    }
}
