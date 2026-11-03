package com.example.doc_booking.repository;
import com.example.doc_booking.model.RegisteredUser;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.*;
public interface RegisteredUserRepository extends MongoRepository<RegisteredUser, String> {
    List<RegisteredUser> findByRole(String role);
    Optional<RegisteredUser> findByEmailIgnoreCase(String email);
}
