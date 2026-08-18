package com.example.lab_rep.dto;

import org.springframework.web.multipart.MultipartFile;

public class LabReportUploadRequest {
    private String patientName;
    private String reportType;
    private MultipartFile file;

    // Constructors
    public LabReportUploadRequest() {}

    public LabReportUploadRequest(String patientName, String reportType, MultipartFile file) {
        this.patientName = patientName;
        this.reportType = reportType;
        this.file = file;
    }

    // Getters and Setters
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }

    public String getReportType() { return reportType; }
    public void setReportType(String reportType) { this.reportType = reportType; }

    public MultipartFile getFile() { return file; }
    public void setFile(MultipartFile file) { this.file = file; }
}