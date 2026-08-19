package com.example.doc_booking.service;

import com.example.doc_booking.model.FileStorage;
import com.example.doc_booking.repository.FileStorageRepository;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class FileStorageService {
    
    private final FileStorageRepository fileStorageRepository;
    private final GridFsTemplate gridFsTemplate;
    private final MongoTemplate mongoTemplate;

    public FileStorageService(FileStorageRepository fileStorageRepository, 
                              GridFsTemplate gridFsTemplate, 
                              MongoTemplate mongoTemplate) {
        this.fileStorageRepository = fileStorageRepository;
        this.gridFsTemplate = gridFsTemplate;
        this.mongoTemplate = mongoTemplate;
    }
    
    // Get file by ID
    public GridFsResource getFile(String fileId) {
        Query query = Query.query(Criteria.where("_id").is(new ObjectId(fileId)));
        GridFSFile gridFSFile = gridFsTemplate.findOne(query);
        
        if (gridFSFile == null) {
            throw new RuntimeException("File not found with ID: " + fileId);
        }
        
        GridFSBucket gridFSBucket = GridFSBuckets.create(mongoTemplate.getDb());
        return new GridFsResource(gridFSFile, 
            gridFSBucket.openDownloadStream(gridFSFile.getObjectId()));
    }
    
    // Get file metadata
    public FileStorage getFileMetadata(String fileId) {
        return fileStorageRepository.findById(fileId)
            .orElseThrow(() -> new RuntimeException("File metadata not found"));
    }
    
    // Get all files for a patient
    public List<FileStorage> getFilesByPatient(String patientEmail) {
        return fileStorageRepository.findByPatientEmail(patientEmail);
    }
    
    // Get prescriptions for a patient
    public List<FileStorage> getPrescriptionsByPatient(String patientEmail) {
        return fileStorageRepository.findByPatientEmailAndFileCategory(patientEmail, "PRESCRIPTION");
    }
    
    // Get lab reports for a patient
    public List<FileStorage> getLabReportsByPatient(String patientEmail) {
        return fileStorageRepository.findByPatientEmailAndFileCategory(patientEmail, "LAB_REPORT");
    }
    
    // Get files for a specific booking
    public List<FileStorage> getFilesByBooking(String bookingId) {
        return fileStorageRepository.findByBookingId(bookingId);
    }
}
