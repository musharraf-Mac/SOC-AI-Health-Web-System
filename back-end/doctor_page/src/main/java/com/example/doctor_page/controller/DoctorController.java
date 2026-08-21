package com.example.doctor_page.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.doctor_page.model.Patient;
import com.example.doctor_page.model.Prescription;
import com.example.doctor_page.repository.PatientRepository;
import com.example.doctor_page.repository.PrescriptionRepository;
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

    // =========================================================
    // 4. Get prescriptions by patient email
    // =========================================================
    @GetMapping("/prescriptions/{patientEmail}")
    public List<Prescription> getPrescriptions(@PathVariable String patientEmail) {
        return prescriptionRepo.findByPatientEmail(patientEmail);
    }
}