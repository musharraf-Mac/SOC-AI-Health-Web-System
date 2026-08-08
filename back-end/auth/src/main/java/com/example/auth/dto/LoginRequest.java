package com.example.auth.dto;

import lombok.Data;

@Data
public class LoginRequest {
    private String username;
    private String email;
    private String password;
    private String role;
}
