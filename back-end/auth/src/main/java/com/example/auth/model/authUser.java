package com.example.auth.model;

import java.time.LocalDateTime;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection="users")
@Data
public class authUser {
    @Id
    private String id;
    private String username;
    @Indexed(unique = true)
    private String email;
    private String password;
    private String role; // PATIENT, DOCTOR, LAB_STAFF
    private LocalDateTime createdAt = LocalDateTime.now();

}
