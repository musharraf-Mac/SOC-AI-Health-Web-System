package com.example.doctor_page.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.doctor_page.model.Patient;

@Repository
public interface PatientRepository extends MongoRepository<Patient, String> {
    
    Optional<Patient> findByEmail(String email);    
    
    List<Patient> findAllByEmail(String email);
}