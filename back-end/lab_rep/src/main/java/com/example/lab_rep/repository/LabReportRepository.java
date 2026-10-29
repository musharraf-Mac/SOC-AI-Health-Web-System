package com.example.lab_rep.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.lab_rep.model.LabReport;

@Repository
public interface LabReportRepository extends MongoRepository<LabReport, String> {
    List<LabReport> findByPatientEmail(String patientEmail);
    List<LabReport> findByPatientName(String patientName);
    List<LabReport> findByStatus(String status);
    Optional<LabReport> findByFileId(String fileId);
}