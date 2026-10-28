package com.example.lab_rep.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "lab_reports")
public class LabReport {
    
    @Id
    private String id;
    private String patientName;
    private String patientEmail;
    private String reportType;
    private String fileName;
    private String fileType;
    private long fileSize;
    /** MongoDB GridFS file id (stored in fs.files/fs.chunks), not a local path. */
    private String fileId;
    private String reportObjectPath;
    private String explanationObjectPath;
    private String analysisProvider;
    private String analysisModel;
    private String analysisError;
    private String extractedText;
    private String analysisResult;
    private String status;
    private LocalDateTime processingStartedAt;
    private LocalDateTime uploadedAt;
    private LocalDateTime processedAt;

    public LabReport() {
        this.uploadedAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    // Constructor for Step 1 (only basic info)
    public LabReport(String patientName, String patientEmail, String reportType, 
                     String fileName, String fileType, long fileSize, String fileId) {
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.reportType = reportType;
        this.fileName = fileName;
        this.fileType = fileType;
        this.fileSize = fileSize;
        this.fileId = fileId;
        this.uploadedAt = LocalDateTime.now();
        this.status = "PENDING";
    }

    public String getReportObjectPath() { return reportObjectPath; }
    public void setReportObjectPath(String value) { this.reportObjectPath = value; }
    public String getExplanationObjectPath() { return explanationObjectPath; }
    public void setExplanationObjectPath(String value) { this.explanationObjectPath = value; }
    public String getAnalysisProvider() { return analysisProvider; }
    public void setAnalysisProvider(String value) { this.analysisProvider = value; }
    public String getAnalysisModel() { return analysisModel; }
    public void setAnalysisModel(String value) { this.analysisModel = value; }
    public String getAnalysisError() { return analysisError; }
    public void setAnalysisError(String value) { this.analysisError = value; }
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public String getFileType() { return fileType; }
    public void setFileType(String fileType) { this.fileType = fileType; }

    public long getFileSize() { return fileSize; }
    public void setFileSize(long fileSize) { this.fileSize = fileSize; }

    public String getFileId() { return fileId; }
    public void setFileId(String fileId) { this.fileId = fileId; }

    public String getExtractedText() { return extractedText; }
    public void setExtractedText(String extractedText) { this.extractedText = extractedText; }

    public String getAnalysisResult() { return analysisResult; }
    public void setAnalysisResult(String analysisResult) { this.analysisResult = analysisResult; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(LocalDateTime uploadedAt) { this.uploadedAt = uploadedAt; }

    public LocalDateTime getProcessedAt() { return processedAt; }
    public void setProcessedAt(LocalDateTime processedAt) { this.processedAt = processedAt; }
}
