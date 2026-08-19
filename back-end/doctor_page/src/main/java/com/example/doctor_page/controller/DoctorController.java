package com.example.doctor_page.controller;

import org.springframework.beans.factory.annotation.Autowired;
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
    @GetMapping("/patients/{email}")
    public Patient getPatient(@PathVariable String email) {
        return patientRepo.findByEmail(email);
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
    @GetMapping("/prescriptions/{email}")
    public List<Prescription> getPrescriptions(
            @PathVariable String email) {

        return prescriptionRepo.findByPatientEmail(email);
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