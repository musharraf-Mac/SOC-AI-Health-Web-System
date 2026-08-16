package com.example.lab_rep.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import lombok.Data;

@Document(collection = "users")
@Data
public class AuthUser {
    @Id
    private String id;
    private String fullname;
    private String email;
    private String role;
}