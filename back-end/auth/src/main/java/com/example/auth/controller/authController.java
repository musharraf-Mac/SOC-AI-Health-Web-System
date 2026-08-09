package com.example.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.model.authUser;
import com.example.auth.service.authService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class authController {
    private final authService service;

    @PostMapping("/register")
    public ResponseEntity<?> register (@RequestBody RegisterRequest req) {
        try {
            authUser user = service.register(req);
            return ResponseEntity.ok(Map.of("message", "Registered successfully to database", "id", user.getId()));
        } catch (RuntimeException e) {
            return ResponseEntity.status(409).body(Map.of("error", e.getMessage()));
        }
    }
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) {
        try {
            return ResponseEntity.ok(service.login(req));
        } catch (RuntimeException e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}
