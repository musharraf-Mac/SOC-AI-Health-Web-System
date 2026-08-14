package com.example.lab_rep.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.lab_rep.model.LabReport;
import com.example.lab_rep.repository.LabReportRepository;

@Service
public class LabReportService {

    @Autowired
    private LabReportRepository repository;

    @Autowired
    private OCRService ocrService;

    @Autowired
    private GeminiService geminiService;

    private final String UPLOAD_DIR = "./uploads";

    public LabReport uploadReport(String patientName, String patientEmail, 
                                  String reportType, MultipartFile file) throws IOException {
        
        Path uploadPath = Paths.get(UPLOAD_DIR);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
        }

        String originalFileName = file.getOriginalFilename();
        String fileName = System.currentTimeMillis() + "_" + originalFileName;
        Path filePath = uploadPath.resolve(fileName);

        Files.copy(file.getInputStream(), filePath);

        LabReport report = new LabReport(
            patientName,
            patientEmail,
            reportType,
            originalFileName,
            file.getContentType(),
            file.getSize(),
            filePath.toString()
        );
        
        // Save initial record
        LabReport savedReport = repository.save(report);
        
        try {
            // Step 2: Extract text using OCR
            String extractedText = ocrService.extractTextFromPDF(filePath.toString());
            savedReport.setExtractedText(extractedText);
            savedReport.setStatus("PROCESSING");
            repository.save(savedReport);
            
            // Step 3: Analyze with Gemini
            String analysis = geminiService.analyzeLabReport(extractedText);
            savedReport.setAnalysisResult(analysis);
            savedReport.setStatus("COMPLETED");
            
        } catch (Exception e) {
            savedReport.setStatus("FAILED");
            savedReport.setAnalysisResult("Error: " + e.getMessage());
        }
        
        return repository.save(savedReport);
    }

    public List<LabReport> getAllReports() {
        return repository.findAll();
    }

    public Optional<LabReport> getReportById(String id) {
        return repository.findById(id);
    }

    public List<LabReport> getReportsByPatient(String patientEmail) {
        return repository.findByPatientEmail(patientEmail);
    }

    public void deleteReport(String id) {
        repository.deleteById(id);
    }
}