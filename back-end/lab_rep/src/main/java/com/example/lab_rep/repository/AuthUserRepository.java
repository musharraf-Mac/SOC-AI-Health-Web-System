package com.example.lab_rep.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;

import com.example.lab_rep.model.AuthUser;

public interface AuthUserRepository extends MongoRepository<AuthUser, String> {
    Optional<AuthUser> findByEmail(String email);
}