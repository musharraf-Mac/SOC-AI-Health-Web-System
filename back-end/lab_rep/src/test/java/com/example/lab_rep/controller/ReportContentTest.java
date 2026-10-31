package com.example.lab_rep.controller;

import com.example.lab_rep.Security.ReportAccess;
import com.example.lab_rep.model.LabReport;
import com.example.lab_rep.repository.*;
import com.example.lab_rep.service.*;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReportContentTest {
    private static final String SECRET = "test-only-signing-key-with-at-least-32-bytes";
    private final LabReportRepository repository = mock(LabReportRepository.class);
    private final ReportStorage storage = mock(ReportStorage.class);
    private MockMvc mvc;
    private LabReport report;

    @BeforeEach void setup() {
        report = new LabReport("Patient", "patient@example.test", "BLOOD_TEST", "blood.pdf", "application/pdf", 12, "r1");
        report.setId("r1");
        report.setStatus("COMPLETED");
        report.setReportObjectPath("reports/r1/blood.pdf");
        report.setExplanationObjectPath("explanations/r1/blood-explanation.json");
        when(repository.findById("r1")).thenReturn(Optional.of(report));
        when(repository.findAll()).thenReturn(List.of(report));
        when(storage.read(report.getReportObjectPath())).thenReturn("%PDF-sample".getBytes(StandardCharsets.UTF_8));
        when(storage.read(report.getExplanationObjectPath())).thenReturn(
            "{\"reportId\":\"r1\",\"explanation\":\"Saved explanation\",\"provider\":\"ollama\",\"model\":\"qwen3.5:4b\"}".getBytes(StandardCharsets.UTF_8));
        var service = new LabReportService(repository, mock(OcrService.class), mock(AiAnalysisService.class), storage,
            mock(GridFsTemplate.class), mock(ReportTaskQueue.class), mock(MongoTemplate.class));
        mvc = MockMvcBuilders.standaloneSetup(new LabReportController(service, mock(AuthUserRepository.class), new ReportAccess(SECRET))).build();
    }
    private String token(String role, String email) {
        return "Bearer " + Jwts.builder().claim("role", role).claim("email", email)
            .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8))).compact();
    }
    private String staff() { return token("LAB_STAFF", "staff@example.test"); }

    @Test void staffReadsSavedJsonAndBothPdfRoutes() throws Exception {
        mvc.perform(get("/api/reports/r1/explanation").header("Authorization", staff()))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.reportId").value("r1")).andExpect(jsonPath("$.explanation").value("Saved explanation"))
            .andExpect(jsonPath("$.model").value("qwen3.5:4b"));
        for (String route : List.of("pdf", "file")) {
            mvc.perform(get("/api/reports/r1/" + route).header("Authorization", staff()))
                .andExpect(status().isOk()).andExpect(content().contentType("application/pdf"))
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(content().bytes("%PDF-sample".getBytes(StandardCharsets.UTF_8)));
        }
        verify(storage, times(1)).read(report.getExplanationObjectPath());
    }
    @Test void ownerCanReadButOtherPatientsAndAnonymousCannotTouchStorage() throws Exception {
        for (String route : List.of("pdf", "explanation")) {
            mvc.perform(get("/api/reports/r1/" + route)).andExpect(status().isUnauthorized());
            mvc.perform(get("/api/reports/r1/" + route).header("Authorization", token("PATIENT", "other@example.test")))
                .andExpect(status().isForbidden());
        }
        verifyNoInteractions(storage);
        mvc.perform(get("/api/reports/r1/explanation").header("Authorization", token("PATIENT", "patient@example.test")))
            .andExpect(status().isOk());
    }
    @Test void missingReportReturns404() throws Exception {
        for (String route : List.of("pdf", "explanation")) {
            mvc.perform(get("/api/reports/missing/" + route).header("Authorization", staff())).andExpect(status().isNotFound());
        }
        verifyNoInteractions(storage);
    }
    @Test void unfinishedReportsReturnStatusWithoutReadingOldObjects() throws Exception {
        for (String state : List.of("PENDING", "PROCESSING", "FAILED")) {
            report.setStatus(state);
            mvc.perform(get("/api/reports/r1/explanation").header("Authorization", staff()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value(state))
                .andExpect(jsonPath("$.explanation").isEmpty());
        }
        verifyNoInteractions(storage);
    }
    @Test void storageFailureIs503WithSafeMessage() throws Exception {
        when(storage.read(anyString())).thenThrow(new IllegalStateException("Private bucket detail"));
        for (String route : List.of("pdf", "explanation")) {
            mvc.perform(get("/api/reports/r1/" + route).header("Authorization", staff()))
                .andExpect(status().isServiceUnavailable()).andExpect(jsonPath("$.error").exists())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("Private bucket"))));
        }
    }
    @Test void malformedOrMismatchedExplanationIsNotShown() throws Exception {
        for (String document : List.of("not json", "{}", "{\"reportId\":\"wrong\",\"explanation\":\"Other patient\"}")) {
            when(storage.read(report.getExplanationObjectPath())).thenReturn(document.getBytes(StandardCharsets.UTF_8));
            mvc.perform(get("/api/reports/r1/explanation").header("Authorization", staff()))
                .andExpect(status().isServiceUnavailable());
        }
    }
    @Test void metadataListAvoidsCloudReadsAndLegacyExplanationStillWorks() throws Exception {
        mvc.perform(get("/api/reports/view?includeExplanation=false").header("Authorization", staff()))
            .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value("r1"));
        report.setExplanationObjectPath(null);
        report.setAnalysisResult("Legacy explanation");
        mvc.perform(get("/api/reports/r1/explanation").header("Authorization", staff()))
            .andExpect(status().isOk()).andExpect(jsonPath("$.explanation").value("Legacy explanation"));
        verifyNoInteractions(storage);
    }
}
