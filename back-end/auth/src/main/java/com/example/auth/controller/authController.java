package com.example.auth.controller;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader; 

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.model.authUser;
import com.example.auth.service.authService;
import com.example.auth.security.JwtUtil;
import com.example.auth.repository.authrepo;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class authController {
    private final authService service;
    private final authrepo authrepo;
    private final JwtUtil jwtUtil;

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
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        try {
            log.info("Received request to /me endpoint");
            
            if (authHeader == null || authHeader.isEmpty()) {
                log.warn("Authorization header is missing");
                return ResponseEntity.status(401).body(Map.of("error", "Authorization header is required"));
            }
            
            if (!authHeader.startsWith("Bearer ")) {
                log.warn("Authorization header doesn't start with Bearer");
                return ResponseEntity.status(401).body(Map.of("error", "Invalid Authorization header format. Must start with 'Bearer '"));
            }

            String token = authHeader.substring(7);
            log.info("Token extracted: {}...", token.substring(0, Math.min(token.length(), 20)));
            
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid token provided");
                return ResponseEntity.status(401).body(Map.of("error", "Invalid or expired token"));
            }
            
            String userId = jwtUtil.getUserIdFromToken(token);
            log.info("User ID extracted: {}", userId);
            
            authUser user = authrepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
            return ResponseEntity.ok(Map.of(
                "fullName", user.getFullname(),
                "email", user.getEmail(),
                "role", user.getRole()
            ));
            
        } catch (Exception e) {
            log.error("Error in /me endpoint: ", e);
            return ResponseEntity.status(401).body(Map.of("error", "Authentication failed: " + e.getMessage()));
        }
    }
}
