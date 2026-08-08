package com.example.auth.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.auth.model.authUser;

public interface authrepo extends MongoRepository<authUser, String> {
    Optional<authUser> findByEmail(String email);
    boolean existsByEmail(String email);
}
