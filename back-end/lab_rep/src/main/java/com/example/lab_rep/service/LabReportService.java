package com.example.lab_rep.service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.lab_rep.model.LabReport;
import com.example.lab_rep.repository.LabReportRepository;

@Service
public class LabReportService {

    @Autowired
    private LabReportRepository repository;

    @Autowired
    private OcrService ocrService;

    @Autowired
    private GeminiService geminiService;

    @Autowired
    private GridFsTemplate gridFsTemplate;

    public LabReport uploadReport(String patientName, String patientEmail, 
                                  String reportType, MultipartFile file) throws IOException {
        
        String originalFileName = file.getOriginalFilename();
        Document metadata = new Document("patientEmail", patientEmail)
            .append("reportType", reportType);
        ObjectId gridFsFileId = gridFsTemplate.store(
            file.getInputStream(), originalFileName, file.getContentType(), metadata);

        LabReport report = new LabReport(
            patientName,
            patientEmail,
            reportType,
            originalFileName,
            file.getContentType(),
            file.getSize(),
            gridFsFileId.toHexString()
        );
        
        // Save initial record
        LabReport savedReport = repository.save(report);
        
        try {
            // Step 2: Extract text using OCR
            // OCR receives the uploaded content directly; no server file is created.
            String extractedText = ocrService.extractTextFromPDF(file.getBytes());
            savedReport.setExtractedText(extractedText);
            savedReport.setStatus("PROCESSING");
            repository.save(savedReport);
            
            // Step 3: Analyze with Gemini
            String analysis = geminiService.analyzeLabReport(extractedText);
            savedReport.setAnalysisResult(analysis);
            savedReport.setStatus("COMPLETED");
            savedReport.setProcessedAt(LocalDateTime.now());
            
        } catch (Exception e) {
            savedReport.setStatus("FAILED");
            savedReport.setAnalysisResult("Error: " + e.getMessage());
        }
        
        return repository.save(savedReport);
    }

    public List<LabReport> getAllReports() {
        return repository.findAll();
    }

    public Optional<LabReport> getReportByFileId(String fileId) {
        return repository.findByFileId(fileId);
    }

    public Optional<LabReport> getReportById(String id) {
        return repository.findById(id);
    }

    public List<LabReport> getReportsByPatient(String patientEmail) {
        return repository.findByPatientEmail(patientEmail);
    }

    public void deleteReport(String id) {
        repository.findById(id).ifPresent(report -> {
            if (report.getFileId() != null && ObjectId.isValid(report.getFileId())) {
                gridFsTemplate.delete(new Query(Criteria.where("_id").is(new ObjectId(report.getFileId()))));
            }
            repository.delete(report);
        });
    }
}
