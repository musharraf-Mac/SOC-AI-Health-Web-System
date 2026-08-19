package com.example.bmi.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.bmi.model.BmiRecord;

public interface BmiRepository
        extends MongoRepository<BmiRecord, String> {

    List<BmiRecord> findByUserIdOrderByCalculatedAtDesc(
            String userId
    );
    void deleteByUserId(String userId);
}