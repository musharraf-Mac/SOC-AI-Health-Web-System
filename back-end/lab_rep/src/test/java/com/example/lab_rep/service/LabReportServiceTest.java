package com.example.lab_rep.service;

import com.example.lab_rep.model.LabReport;
import com.example.lab_rep.repository.LabReportRepository;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class LabReportServiceTest {
    @Test void queuedUploadWaitsForWorkerWithoutCallingModels() throws Exception {
        var repo = mock(LabReportRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var ocr = mock(OcrService.class);
        when(ocr.extractTextFromPDF(any())).thenReturn("Report");
        var ai = mock(AiAnalysisService.class);
        var queue = mock(ReportTaskQueue.class);
        when(queue.enabled()).thenReturn(true);
        var result = new LabReportService(repo, ocr, ai, mock(ReportStorage.class), mock(GridFsTemplate.class), queue,
            mock(org.springframework.data.mongodb.core.MongoTemplate.class)).uploadReport("Patient", "p@example.com", "Blood",
            new MockMultipartFile("file", "blood.pdf", "application/pdf", new byte[]{1}));
        assertEquals("PENDING", result.getStatus());
        verify(queue).enqueue(result.getId());
        verifyNoInteractions(ai);
    }

    @Test void queueFailureRetainsUploadedReportForManualRetry() throws Exception {
        var repo = mock(LabReportRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var ocr = mock(OcrService.class);
        when(ocr.extractTextFromPDF(any())).thenReturn("Report");
        var queue = mock(ReportTaskQueue.class);
        when(queue.enabled()).thenReturn(true);
        doThrow(new IllegalStateException("Queue unavailable")).when(queue).enqueue(any());
        var storage = mock(ReportStorage.class);
        var result = new LabReportService(repo, ocr, mock(AiAnalysisService.class), storage, mock(GridFsTemplate.class), queue,
            mock(org.springframework.data.mongodb.core.MongoTemplate.class)).uploadReport("Patient", "p@example.com", "Blood",
            new MockMultipartFile("file", "blood.pdf", "application/pdf", new byte[]{1}));
        assertEquals("FAILED", result.getStatus());
        assertNotNull(result.getReportObjectPath());
        verify(storage, never()).delete(any());
    }

    @Test void duplicateCompletedTaskDoesNotCallModels() throws Exception {
        var repo = mock(LabReportRepository.class);
        var report = new LabReport();
        report.setId("id");
        report.setStatus("COMPLETED");
        when(repo.findById("id")).thenReturn(Optional.of(report));
        var ai = mock(AiAnalysisService.class);
        var result = new LabReportService(repo, mock(OcrService.class), ai, mock(ReportStorage.class), mock(GridFsTemplate.class),
            mock(ReportTaskQueue.class), mock(org.springframework.data.mongodb.core.MongoTemplate.class)).processQueued("id");
        assertEquals("COMPLETED", result.getStatus());
        verifyNoInteractions(ai);
    }

    @Test void savesSeparateObjectsAndMetadataWithoutExplanationBody() throws Exception {
        var repo = mock(LabReportRepository.class);
        var ocr = mock(OcrService.class);
        var ai = mock(AiAnalysisService.class);
        var objects = new HashMap<String, byte[]>();
        ReportStorage storage = new ReportStorage() {
            public void put(String p, byte[] b, String type) { objects.put(p, b); }
            public byte[] read(String p) { return objects.get(p); }
            public void delete(String p) { objects.remove(p); }
        };
        when(ocr.extractTextFromPDF(any())).thenReturn("Report text");
        when(ai.analyze(any())).thenReturn(new AiAnalysisService.Analysis("Explanation", "ollama", "qwen3.5:4b"));
        var savedAnalysis = new ArrayList<String>();
        when(repo.save(any())).thenAnswer(inv -> {
            LabReport r = inv.getArgument(0);
            savedAnalysis.add(r.getAnalysisResult());
            return r;
        });
        var service = new LabReportService(repo, ocr, ai, storage, mock(GridFsTemplate.class), mock(ReportTaskQueue.class), mock(org.springframework.data.mongodb.core.MongoTemplate.class));
        var report = service.uploadReport("Patient", "p@example.com", "Blood", new MockMultipartFile("file", "blood.pdf", "application/pdf", new byte[]{1}));
        assertEquals("COMPLETED", report.getStatus());
        assertEquals("Explanation", report.getAnalysisResult());
        assertTrue(report.getReportObjectPath().startsWith("reports/" + report.getId()));
        assertTrue(report.getExplanationObjectPath().endsWith("/blood-explanation.json"));
        assertEquals(2, objects.size());
        assertTrue(savedAnalysis.stream().allMatch(Objects::isNull));
    }

    @Test void providerFailureRetainsOriginalAndMarksFailed() throws Exception {
        var repo = mock(LabReportRepository.class);
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var ocr = mock(OcrService.class);
        when(ocr.extractTextFromPDF(any())).thenReturn("Report");
        var ai = mock(AiAnalysisService.class);
        when(ai.analyze(any())).thenThrow(new AiAnalysisService.AnalysisException("OLLAMA_CONNECTION: backend cannot reach localhost:11434."));
        var storage = mock(ReportStorage.class);
        var result = new LabReportService(repo, ocr, ai, storage, mock(GridFsTemplate.class), mock(ReportTaskQueue.class), mock(org.springframework.data.mongodb.core.MongoTemplate.class)).uploadReport(
                "Patient", "p@example.com", "Blood", new MockMultipartFile("file", "blood.pdf", "application/pdf", new byte[]{1}));
        assertEquals("FAILED", result.getStatus());
        assertNotNull(result.getReportObjectPath());
        assertNull(result.getExplanationObjectPath());
        assertTrue(result.getAnalysisError().startsWith("OLLAMA_CONNECTION:"));
        assertNull(result.getAnalysisResult());
        verify(storage, never()).delete(any());
    }
}
