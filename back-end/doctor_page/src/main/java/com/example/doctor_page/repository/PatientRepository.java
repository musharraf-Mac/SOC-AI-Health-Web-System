package com.example.doctor_page.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.doctor_page.model.Patient;
@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {
    Patient findByEmail(String email);
}
