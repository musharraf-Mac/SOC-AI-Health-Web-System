package com.example.doctor_page.repository;
import com.example.doctor_page.model.RegisteredUser;
import org.springframework.data.mongodb.repository.MongoRepository;
public interface RegisteredUserRepository extends MongoRepository<RegisteredUser, String> {}
