package com.example.doctor_page.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.doctor_page.model.Patient;
import com.example.doctor_page.model.Prescription;
import com.example.doctor_page.repository.PatientRepository;
import com.example.doctor_page.repository.PrescriptionRepository;
import com.example.doctor_page.security.JwtUtil;
import com.example.doctor_page.service.PrescriptionService;

@RestController
@RequestMapping("/api/doctor")
public class DoctorController {

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private PrescriptionRepository prescriptionRepo;

    @Autowired
    private PrescriptionService prescriptionService;

    private final JwtUtil jwtUtil;

    public DoctorController(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    // =========================================================
    // 1. Get all patients
    // =========================================================
    @GetMapping("/patients")
    public List<Patient> getAllPatients() {
        return patientRepo.findAll();
    }

    // =========================================================
    // 2. Get patient by token number
    // =========================================================
    @GetMapping("/patients/{tokenNumber}")
    public ResponseEntity<?> getPatient(@PathVariable String tokenNumber) {
        Optional<Patient> patient = patientRepo.findByTokenNumber(tokenNumber);
        if (patient.isPresent()) {
            return ResponseEntity.ok(patient.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("Patient not found with token number: " + tokenNumber);
        }
    }

    // =========================================================
    // 3. Save prescription
    // =========================================================
    @PostMapping("/prescriptions")
    public ResponseEntity<?> savePrescription(@RequestBody Prescription prescription) {
        try {
            // Optional: Validate patient exists
            List<Patient> patients = patientRepo.findByPatientEmail(prescription.getPatientEmail());
            if (patients.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Patient not found with email: " + prescription.getPatientEmail());
            }

           
            Prescription saved = prescriptionRepo.save(prescription);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body("Error saving prescription: " + e.getMessage());
        }
    }

    
    // 4. Get prescriptions by patient email
    
    @GetMapping("/prescriptions/me")
public ResponseEntity<?> getMyPrescriptions(
        @RequestHeader(value = "Authorization", required = false) String authHeader) {

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(401).body(Map.of("error", "Missing or invalid token"));
    }

    String token = authHeader.substring(7);
    if (!jwtUtil.validateToken(token)) {
        return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
    }

    // String userId = jwtUtil.getUserIdFromToken(token);
    String patientEmailFromToken = jwtUtil.getEmailFromToken(token); // Assuming you have a method to extract email from token    
    System.out.println("Extracted patient email from token: " + patientEmailFromToken); // Debugging line
    List<Prescription> prescriptions = prescriptionRepo.findByPatientEmail(patientEmailFromToken);
    return ResponseEntity.ok(prescriptions);
}
    @DeleteMapping("/prescriptions/delete/{id}")
    public ResponseEntity<?> deletePrescription(@PathVariable String id) {
        try {
            prescriptionRepo.deleteById(id);
            return ResponseEntity.ok().body("Prescription deleted successfully");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete prescription: " + e.getMessage()));
        }
    }

}