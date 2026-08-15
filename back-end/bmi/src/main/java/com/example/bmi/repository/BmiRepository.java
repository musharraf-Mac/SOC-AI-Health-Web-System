package com.example.bmi.repository;

import com.example.bmi.model.BmiRecord;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BmiRepository
        extends MongoRepository<BmiRecord, String> {

    List<BmiRecord> findByUserIdOrderByCalculatedAtDesc(
            String userId
    );
}