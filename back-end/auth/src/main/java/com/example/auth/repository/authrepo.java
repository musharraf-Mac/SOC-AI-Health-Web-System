package com.example.auth.repository;

import java.util.Optional;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.auth.model.authUser;

@Repository
public interface authrepo extends MongoRepository<authUser, String> {
    Optional<authUser> findByEmail(String email);
    boolean existsByEmail(String email);

    default void verifyDataBaseConnection() {
        count(); // This will throw an exception if the database connection is not established
    }
}
