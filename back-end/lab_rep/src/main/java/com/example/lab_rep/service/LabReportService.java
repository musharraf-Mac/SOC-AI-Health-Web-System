package com.example.lab_rep.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import com.example.lab_rep.model.LabReport;
import com.example.lab_rep.repository.LabReportRepository;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class LabReportService {
    private final LabReportRepository repository;
    private final OcrService ocr;
    private final AiAnalysisService ai;
    private final ReportStorage storage;
    private final GridFsTemplate legacy;
    private final ReportTaskQueue queue;
    private final org.springframework.data.mongodb.core.MongoTemplate mongo;
    private final ObjectMapper json = new ObjectMapper();

    public LabReportService(LabReportRepository repository, OcrService ocr, AiAnalysisService ai,
                            ReportStorage storage, GridFsTemplate legacy, ReportTaskQueue queue, org.springframework.data.mongodb.core.MongoTemplate mongo) {
        this.repository = repository;
        this.ocr = ocr;
        this.ai = ai;
        this.storage = storage;
        this.legacy = legacy;
        this.queue = queue;
        this.mongo = mongo;
    }

    public LabReport uploadReport(String patientName, String patientEmail, String reportType, MultipartFile file) throws IOException {
        if (file.isEmpty() || file.getSize() > 20L * 1024 * 1024) throw new IllegalArgumentException("PDF must be between 1 byte and 20 MB");
        byte[] bytes = file.getBytes();
        String text = ocr.extractTextFromPDF(bytes);
        if (text.length() > 60000) throw new IllegalArgumentException("Report exceeds the 60000 character analysis limit");
        String name = safeName(file.getOriginalFilename());
        String id = new ObjectId().toHexString();
        LabReport report = new LabReport(patientName, patientEmail, reportType, name, "application/pdf", bytes.length, id);
        report.setId(id);
        report.setReportObjectPath("reports/" + id + "/" + name);
        storage.put(report.getReportObjectPath(), bytes, "application/pdf");
        try { repository.save(report); }
        catch (RuntimeException failure) { storage.delete(report.getReportObjectPath()); throw failure; }
        return queue.enabled() ? enqueue(report) : process(report, text);
    }

    private LabReport enqueue(LabReport report) {
        report.setStatus("PENDING");
        report.setAnalysisError(null);
        report.setAnalysisResult(null);
        repository.save(report);
        try { queue.enqueue(report.getId()); }
        catch (Exception failure) {
            report.setStatus("FAILED");
            report.setAnalysisError("Report saved but processing could not be queued. Retry explanation.");
            repository.save(report);
        }
        return report;
    }

    public LabReport processQueued(String id) throws IOException {
        // Lease prevents duplicate task delivery from running the same report simultaneously.
        var eligible = new Criteria().andOperator(Criteria.where("_id").is(id), new Criteria().orOperator(
            Criteria.where("status").in("PENDING", "FAILED"),
            new Criteria().andOperator(Criteria.where("status").is("PROCESSING"),
                Criteria.where("processingStartedAt").lt(LocalDateTime.now().minusMinutes(6)))));
        LabReport report = mongo.findAndModify(new Query(eligible),
            new org.springframework.data.mongodb.core.query.Update().set("status", "PROCESSING").set("processingStartedAt", LocalDateTime.now()),
            org.springframework.data.mongodb.core.FindAndModifyOptions.options().returnNew(true), LabReport.class);
        if (report == null) {
            report = requireReport(id);
            if ("COMPLETED".equals(report.getStatus())) return report;
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Report already processing; retry later");
        }
        try { return process(report, ocr.extractTextFromPDF(download(report))); }
        catch (Exception failure) {
            report.setStatus("FAILED");
            report.setAnalysisError("Report could not be read for processing. Retry later.");
            return repository.save(report);
        }
    }

    private LabReport process(LabReport report, String text) {
        report.setStatus("PROCESSING");
        report.setAnalysisError(null);
        report.setAnalysisResult(null);
        repository.save(report);
        try {
            if (text.length() > 60000) throw new IllegalArgumentException("Report exceeds analysis text limit");
            AiAnalysisService.Analysis result = ai.analyze(text);
            String name = report.getFileName().replaceFirst("(?i)\\.pdf$", "");
            String path = "explanations/" + report.getId() + "/" + name + "-explanation.json";
            storage.put(path, json.writeValueAsBytes(Map.of("reportId", report.getId(), "fileName", report.getFileName(),
                    "explanation", result.text(), "provider", result.provider(), "model", result.model(),
                    "generatedAt", java.time.Instant.now().toString())), "application/json");
            report.setExplanationObjectPath(path);
            report.setAnalysisProvider(result.provider());
            report.setAnalysisModel(result.model());
            report.setExtractedText(null);
            report.setProcessedAt(LocalDateTime.now());
            report.setStatus("COMPLETED");
        } catch (Exception failure) {
            report.setStatus("FAILED");
            report.setAnalysisError(failure instanceof AiAnalysisService.OllamaException
                ? failure.getMessage() : "Explanation could not be generated or saved. Check storage configuration and retry.");
        }
        repository.save(report);
        return hydrate(report);
    }

    private LabReport hydrate(LabReport report) {
        if (report.getExplanationObjectPath() != null && "COMPLETED".equals(report.getStatus())) {
            try { report.setAnalysisResult(json.readTree(storage.read(report.getExplanationObjectPath())).path("explanation").asText()); }
            catch (Exception failure) { report.setAnalysisError("Stored explanation is temporarily unavailable."); }
        }
        return report;
    }
    public List<LabReport> getAllReports() { return repository.findAll().stream().map(this::hydrate).toList(); }
    public List<LabReport> getReportsByPatient(String email) { return repository.findByPatientEmail(email).stream().map(this::hydrate).toList(); }
    public Optional<LabReport> getReportByFileId(String id) { return repository.findById(id).or(() -> repository.findByFileId(id)).map(this::hydrate); }
    public Optional<LabReport> getReportById(String id) { return getReportByFileId(id); }
    public LabReport requireReport(String id) { return getReportById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Report not found")); }
    public byte[] download(LabReport report) throws IOException {
        if (report.getReportObjectPath() != null) return storage.read(report.getReportObjectPath());
        var file = legacy.findOne(new Query(Criteria.where("_id").is(new ObjectId(report.getFileId()))));
        if (file == null) throw new IOException("Legacy file not found");
        try (var input = legacy.getResource(file).getInputStream()) { return input.readAllBytes(); }
    }
    public LabReport retry(String id) throws IOException {
        LabReport report = requireReport(id);
        if ("COMPLETED".equals(report.getStatus())) return report;
        if ("PROCESSING".equals(report.getStatus())) throw new IllegalArgumentException("Report is already processing");
        return queue.enabled() ? enqueue(report) : process(report, ocr.extractTextFromPDF(download(report)));
    }
    // Copy and verify; retain legacy GridFS data until separately reviewed.
    public LabReport migrate(String id) throws IOException {
        LabReport report = requireReport(id);
        if (report.getReportObjectPath() != null) return report;
        String path = "reports/" + report.getId() + "/" + safeName(report.getFileName());
        byte[] bytes = download(report);
        storage.put(path, bytes, "application/pdf");
        if (!Arrays.equals(bytes, storage.read(path))) throw new IOException("Migration verification failed");
        if (report.getAnalysisResult() != null && "COMPLETED".equals(report.getStatus())) {
            String explanation = "explanations/" + report.getId() + "/" + safeName(report.getFileName()) + "-explanation.json";
            byte[] payload = json.writeValueAsBytes(Map.of("reportId", report.getId(), "fileName", report.getFileName(), "explanation", report.getAnalysisResult()));
            storage.put(explanation, payload, "application/json");
            if (!Arrays.equals(payload, storage.read(explanation))) throw new IOException("Explanation verification failed");
            report.setExplanationObjectPath(explanation);
        }
        report.setReportObjectPath(path);
        report.setAnalysisResult(null);
        report.setExtractedText(null);
        return hydrate(repository.save(report));
    }
    public void deleteReport(String id) {
        LabReport report = requireReport(id);
        storage.delete(report.getExplanationObjectPath());
        storage.delete(report.getReportObjectPath());
        if (report.getFileId() != null && !report.getFileId().equals(report.getId()) && ObjectId.isValid(report.getFileId())) {
            legacy.delete(new Query(Criteria.where("_id").is(new ObjectId(report.getFileId()))));
        }
        repository.delete(report);
    }
    private String safeName(String original) {
        String name = original == null ? "report.pdf" : original.replace((char)92, '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("[^a-zA-Z0-9._-]", "_");
        if (name.length() > 120) name = name.substring(name.length() - 120);
        return name.toLowerCase(Locale.ROOT).endsWith(".pdf") ? name : name + ".pdf";
    }
}
