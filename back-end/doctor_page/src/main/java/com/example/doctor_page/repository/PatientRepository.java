package com.example.doctor_page.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.doctor_page.model.Patient;

@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {
    // ✅ Return List - handles multiple patients with same email
    List<Patient> findByPatientEmail(String patientEmail);
}