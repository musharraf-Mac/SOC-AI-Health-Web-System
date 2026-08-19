package com.example.doc_booking.controller;

import com.example.doc_booking.model.FileStorage;
import com.example.doc_booking.service.FileStorageService;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/doc_booking/files")
@CrossOrigin(origins = "*")
public class FileController {
    
    private final FileStorageService fileStorageService;

    public FileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }
    
    // 1. Get all files for a patient
    @GetMapping("/patient/{patientEmail}")
    public ResponseEntity<List<FileStorage>> getPatientFiles(@PathVariable String patientEmail) {
        List<FileStorage> files = fileStorageService.getFilesByPatient(patientEmail);
        return ResponseEntity.ok(files);
    }
    
    // 2. Get prescriptions for a patient
    @GetMapping("/patient/{patientEmail}/prescriptions")
    public ResponseEntity<List<FileStorage>> getPatientPrescriptions(@PathVariable String patientEmail) {
        List<FileStorage> files = fileStorageService.getPrescriptionsByPatient(patientEmail);
        return ResponseEntity.ok(files);
    }
    
    // 3. Get lab reports for a patient
    @GetMapping("/patient/{patientEmail}/labreports")
    public ResponseEntity<List<FileStorage>> getPatientLabReports(@PathVariable String patientEmail) {
        List<FileStorage> files = fileStorageService.getLabReportsByPatient(patientEmail);
        return ResponseEntity.ok(files);
    }
    
    // 4. Get files for a specific booking
    @GetMapping("/booking/{bookingId}")
    public ResponseEntity<List<FileStorage>> getBookingFiles(@PathVariable String bookingId) {
        List<FileStorage> files = fileStorageService.getFilesByBooking(bookingId);
        return ResponseEntity.ok(files);
    }
    
    // 5. Get file metadata
    @GetMapping("/{fileId}/metadata")
    public ResponseEntity<FileStorage> getFileMetadata(@PathVariable String fileId) {
        FileStorage file = fileStorageService.getFileMetadata(fileId);
        return ResponseEntity.ok(file);
    }
    
    // 6. View/Download the actual file
    @GetMapping("/{fileId}/view")
    public ResponseEntity<?> viewFile(@PathVariable String fileId) {
        try {
            GridFsResource resource = fileStorageService.getFile(fileId);
            FileStorage metadata = fileStorageService.getFileMetadata(fileId);
            
            String contentType = metadata.getContentType();
            if (contentType == null) {
                contentType = "application/pdf";
            }
            
            return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, 
                    "inline; filename=\"" + metadata.getFilename() + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(resource.getInputStream().readAllBytes());
                
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "File not found: " + e.getMessage());
            return ResponseEntity.status(404).body(error);
        }
    }
}
