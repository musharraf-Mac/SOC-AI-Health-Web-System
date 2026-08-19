package com.example.doc_booking.repository;

import com.example.doc_booking.model.FileStorage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface FileStorageRepository extends MongoRepository<FileStorage, String> {
    List<FileStorage> findByPatientEmail(String patientEmail);
    List<FileStorage> findByBookingId(String bookingId);
    List<FileStorage> findByFileCategory(String fileCategory);
    List<FileStorage> findByPatientEmailAndFileCategory(String patientEmail, String fileCategory);
}
