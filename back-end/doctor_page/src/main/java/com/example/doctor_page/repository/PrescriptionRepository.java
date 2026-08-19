package com.example.doctor_page.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.doctor_page.model.Prescription;

@Repository
public interface PrescriptionRepository extends MongoRepository<Prescription, String> {
    List<Prescription> findByPatientEmail(String email);
}
