package com.example.doctor_page.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Objects;

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
    @org.springframework.web.bind.annotation.ModelAttribute
    public void privateResponse(jakarta.servlet.http.HttpServletResponse response) { response.setHeader("Cache-Control", "no-store"); }
    @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.web.server.ResponseStatusException.class)
    public ResponseEntity<?> accessError(org.springframework.web.server.ResponseStatusException error) {
        return ResponseEntity.status(error.getStatusCode()).header("Cache-Control", "no-store").body(Map.of("error", Objects.requireNonNullElse(error.getReason(), "Access denied")));
    }
    @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<?> storageError() {
        return ResponseEntity.status(503).header("Cache-Control", "no-store").body(Map.of("error", "Appointment records are temporarily unavailable."));
    }

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
    public List<Patient> getAllPatients(@RequestHeader(value = "Authorization", required = false) String authorization) {
        return patientRepo.findByDoctorEmailIgnoreCase(jwtUtil.requireDoctorEmail(authorization));
    }

    // =========================================================
    // 2. Get patient by token number
    // =========================================================
    @GetMapping("/patients/{tokenNumber}")
    public ResponseEntity<?> getPatient(@PathVariable String tokenNumber, @RequestHeader(value = "Authorization", required = false) String authorization) {
        Optional<Patient> patient = patientRepo.findByTokenNumberAndDoctorEmailIgnoreCase(tokenNumber, jwtUtil.requireDoctorEmail(authorization));
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
    public ResponseEntity<?> savePrescription(@RequestBody Prescription prescription, @RequestHeader(value = "Authorization", required = false) String authorization) {
        String doctorEmail = jwtUtil.requireDoctorEmail(authorization);
        try {
            var appointment = prescription.getTokenNumber() == null ? Optional.<Patient>empty() : patientRepo.findByTokenNumberAndDoctorEmailIgnoreCase(prescription.getTokenNumber(), doctorEmail);
            if (appointment.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Appointment not found for this doctor."));
            }
            var booking = appointment.get();
            prescription.setId(null); // Creates only: a supplied ID must never overwrite another record.
            prescription.setDoctorEmail(doctorEmail); prescription.setDoctorName(booking.getDoctorName());
            prescription.setPatientEmail(booking.getPatientEmail()); prescription.setPatientName(booking.getpatientName());
            prescription.setCreatedAt(java.time.LocalDateTime.now()); prescription.setUpdatedAt(java.time.LocalDateTime.now());
            prescription.setStatus("ACTIVE");
            Prescription saved = prescriptionRepo.insert(prescription);
            return ResponseEntity.status(HttpStatus.CREATED).body(saved);
            
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Unable to save prescription. Please try again."));
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
    List<Prescription> prescriptions = prescriptionRepo.findByPatientEmail(patientEmailFromToken);
    return ResponseEntity.ok(prescriptions);
}
    @GetMapping("/prescriptions/{email}")
    public List<Prescription> patientPrescriptions(@PathVariable String email,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        String doctorEmail = jwtUtil.requireDoctorEmail(authorization);
        if (patientRepo.findByPatientEmailIgnoreCaseAndDoctorEmailIgnoreCase(email, doctorEmail).isEmpty())
            throw new org.springframework.web.server.ResponseStatusException(HttpStatus.FORBIDDEN, "Patient is not booked with this doctor");
        return prescriptionRepo.findByPatientEmailAndDoctorEmailIgnoreCase(email, doctorEmail);
    }

    @DeleteMapping("/prescriptions/delete/{id}")
    public ResponseEntity<?> deletePrescription(@PathVariable String id, @RequestHeader(value = "Authorization", required = false) String authorization) {
        String doctorEmail = jwtUtil.requireDoctorEmail(authorization);
        var prescription = prescriptionRepo.findById(id).filter(item -> doctorEmail.equalsIgnoreCase(item.getDoctorEmail()))
            .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(HttpStatus.NOT_FOUND, "Prescription not found for this doctor"));
        try {
            prescriptionRepo.delete(prescription);
            return ResponseEntity.ok().body(Map.of("message", "Prescription deleted successfully"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to delete prescription. Please try again."));
        }
    }

}
