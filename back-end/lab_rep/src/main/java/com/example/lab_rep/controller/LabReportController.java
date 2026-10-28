package com.example.lab_rep.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.lab_rep.model.LabReport;
import com.example.lab_rep.repository.AuthUserRepository;
import com.example.lab_rep.service.LabReportService;

import com.example.lab_rep.Security.ReportAccess;

@RestController
@RequestMapping("/api/reports")
// @CrossOrigin(origins = "*")
public class LabReportController {

    private final LabReportService reportService;
    private final AuthUserRepository authUserRepository;

    private final ReportAccess access;

    public LabReportController(LabReportService reportService, AuthUserRepository authUserRepository, ReportAccess access) {
        this.reportService = reportService;
        this.authUserRepository = authUserRepository;
        this.access = access;
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<byte[]> download(@PathVariable String id) throws java.io.IOException {
        LabReport report = reportService.requireReport(id);
        access.patientOrStaff(report.getPatientEmail());
        return ResponseEntity.ok().contentType(org.springframework.http.MediaType.APPLICATION_PDF)
            .header("Cache-Control", "no-store")
            .header("Content-Disposition", org.springframework.http.ContentDisposition.inline().filename(report.getFileName(), java.nio.charset.StandardCharsets.UTF_8).build().toString())
            .body(reportService.download(report));
    }

    @PostMapping("/{id}/retry")
    public ResponseEntity<LabReport> retry(@PathVariable String id) throws java.io.IOException {
        access.staff();
        return ResponseEntity.ok(reportService.retry(id));
    }

    @PostMapping("/{id}/migrate")
    public ResponseEntity<LabReport> migrate(@PathVariable String id) throws java.io.IOException {
        access.staff();
        return ResponseEntity.ok(reportService.migrate(id));
    }

    @PostMapping("/upload")
    public ResponseEntity<?> uploadReport(
            @RequestParam("patientName") String patientName,
            @RequestParam("patientEmail") String patientEmail,
            @RequestParam("reportType") String reportType,
            @RequestParam("file") MultipartFile file) {

        access.staff();
        try {
            Optional<com.example.lab_rep.model.AuthUser> user = authUserRepository.findByEmail(patientEmail);
            if (user.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "No registered patient found with this email."));
            }
            if (!"PATIENT".equalsIgnoreCase(user.get().getRole())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "This email is not registered as a patient."));
            }

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "File is empty"));
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

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Report storage is unavailable. Please try again."));
        }
    }

    @GetMapping ("/view")
    public ResponseEntity<List<LabReport>> getAllReports() {
        access.staff();
        return ResponseEntity.ok(reportService.getAllReports());
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<?> getReportByFileId(@PathVariable String fileId) {
        Optional<LabReport> report = reportService.getReportByFileId(fileId);
        if (report.isPresent()) {
            access.patientOrStaff(report.get().getPatientEmail());
            return ResponseEntity.ok(report.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Report not found with id: " + fileId));
        }
    }

    @GetMapping("/patient/{email}")
    public ResponseEntity<List<LabReport>> getReportsByPatient(@PathVariable String email) {
        access.patientOrStaff(email);
        return ResponseEntity.ok(reportService.getReportsByPatient(email));
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteReport(@PathVariable String id) {
        access.staff();
        reportService.deleteReport(id);
        return ResponseEntity.ok(Map.of("message", "Report deleted successfully"));
    }
}
