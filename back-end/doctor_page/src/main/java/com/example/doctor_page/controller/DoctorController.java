package com.example.doctor_page.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.doctor_page.model.Patient;
import com.example.doctor_page.model.Prescription;
import com.example.doctor_page.repository.PatientRepository;
import com.example.doctor_page.repository.PrescriptionRepository;
import com.example.doctor_page.service.PrescriptionService;


import java.io.File;
import java.nio.file.Files;
import java.util.List;

@RestController
@RequestMapping("/api/doctor")
// @CrossOrigin(origins = "*")
public class DoctorController {

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private PrescriptionRepository prescriptionRepo;

    @Autowired
    private PrescriptionService prescriptionService;


    // =========================================================
    // 1. Get all booked patients
    // =========================================================
    @GetMapping("/patients")
    public List<Patient> getAllPatients() {
        return patientRepo.findAll();
    }

    // =========================================================
    // 2. Get patient by email
    // =========================================================
    @GetMapping("/patients/{patientEmail}")
    public ResponseEntity<?> getPatientsByEmail(@PathVariable String patientEmail) {
    try {
        System.out.println("Searching for patients with email: " + patientEmail);
        
        // Returns List of patients with this email
        List<Patient> patients = patientRepo.findByPatientEmail(patientEmail);
        
        if (patients.isEmpty()) {
            System.out.println("No patients found with email: " + patientEmail);
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body("No patients found with email: " + patientEmail);
        }
        
        System.out.println("Found " + patients.size() + " patients with email: " + patientEmail);
        
        // ⭐ Return ALL patients with that email (just like /patients endpoint)
        return ResponseEntity.ok(patients);
        
    } catch (Exception e) {
        System.err.println("Error fetching patients: " + e.getMessage());
        e.printStackTrace();
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body("Error fetching patients: " + e.getMessage());
    }
}

    // =========================================================
    // 3. Save prescription + generate Webpage + PDF + QR
    // =========================================================
    @PostMapping("/prescriptions")
    public Prescription savePrescription(
            @RequestBody Prescription prescription) throws Exception {

        // -----------------------------------------------------
        // Step 1: Generate webpage
        // -----------------------------------------------------
        String url = prescriptionService.generateWebPage(prescription);

        // Save webpage URL into prescription object
        prescription.setUrl(url);

        // -----------------------------------------------------
        // Step 2: Generate PDF
        // -----------------------------------------------------
        String pdfPath = prescriptionService.generatePdf(prescription);

        // Save PDF path
        prescription.setPdfPath(pdfPath);


        // -----------------------------------------------------
        // Step 3: Generate QR Code
        // -----------------------------------------------------
        String qrPath = prescriptionService.generateQr(prescription);

        // Save QR path
        prescription.setQrCode(qrPath);


        // -----------------------------------------------------
        // Step 4: Save prescription into MongoDB
        // -----------------------------------------------------
        return prescriptionRepo.save(prescription);
    }


    // =========================================================
    // 4. Retrieve prescriptions by patient email
    // =========================================================
    @GetMapping("/prescriptions/{patientEmail}")
    public List<Prescription> getPrescriptions(
            @PathVariable String patientEmail) {

        return prescriptionRepo.findByPatientEmail(patientEmail);
    }


    // =========================================================
    // 5. Open generated prescription webpage
    // =========================================================
    @GetMapping(
            value = "/webpage/{fileName}",
            produces = MediaType.TEXT_HTML_VALUE
    )
    public ResponseEntity<String> getWebPage(
            @PathVariable String fileName) throws Exception {

        // Find file inside webpages folder
        File file = new File("webpages", fileName);


        // If file does not exist
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        // Read HTML file
        String content = Files.readString(file.toPath());

        // Return HTML webpage
        return ResponseEntity.ok(content);
    }
}