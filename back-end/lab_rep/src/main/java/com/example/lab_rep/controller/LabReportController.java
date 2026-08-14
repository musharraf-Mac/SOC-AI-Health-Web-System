package com.example.lab_rep.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.lab_rep.model.LabReport;
import com.example.lab_rep.service.LabReportService;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class LabReportController {

    @Autowired
    private LabReportService reportService;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadReport(
            @RequestParam("patientName") String patientName,
            @RequestParam("patientEmail") String patientEmail,
            @RequestParam("reportType") String reportType,
            @RequestParam("file") MultipartFile file) {

        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.equals("application/pdf")) {
                return ResponseEntity.badRequest().body(Map.of("error", "Only PDF files are allowed"));
            }

            LabReport report = reportService.uploadReport(patientName, patientEmail, reportType, file);

            Map<String, Object> response = new HashMap<>();
            response.put("message", "Report uploaded successfully");
            response.put("reportId", report.getId());
            response.put("patientName", report.getPatientName());
            response.put("status", report.getStatus());
            response.put("uploadedAt", report.getUploadedAt());
            response.put("analysis", report.getAnalysisResult());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<List<LabReport>> getAllReports() {
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getReportById(@PathVariable String id) {
        Optional<LabReport> report = reportService.getReportById(id);
        if (report.isPresent()) {
            return ResponseEntity.ok(report.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Report not found with id: " + id));
        }
    }

    @GetMapping("/patient/{email}")
    public ResponseEntity<List<LabReport>> getReportsByPatient(@PathVariable String email) {
        return ResponseEntity.ok(reportService.getReportsByPatient(email));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable String id) {
        try {
            reportService.deleteReport(id);
            return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Report not found"));
        }
    }
}