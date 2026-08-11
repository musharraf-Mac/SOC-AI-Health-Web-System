package com.example.auth.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.model.authUser;
import com.example.auth.repository.authrepo;
import com.example.auth.security.JwtUtil;
import com.example.auth.service.authService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

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
            log.info("=== /me endpoint called ===");
            log.info("Authorization header: {}", authHeader);
            
            // Check if Authorization header exists
            if (authHeader == null || authHeader.isEmpty()) {
                log.warn("Authorization header is missing or empty");
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Authorization header is required",
                    "details", "Please provide a Bearer token"
                ));
            }
            
            // Check if header starts with Bearer
            if (!authHeader.startsWith("Bearer ")) {
                log.warn("Authorization header doesn't start with 'Bearer '");
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid Authorization header format",
                    "details", "Header must start with 'Bearer '"
                ));
            }

            // Extract token
            String token = authHeader.substring(7); // Remove "Bearer "
            log.info("Token extracted: {}...", token.substring(0, Math.min(token.length(), 30)));
            
            // Validate token
            boolean isValid = jwtUtil.validateToken(token);
            log.info("Token validation result: {}", isValid);
            
            if (!isValid) {
                log.warn("Token validation failed");
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid or expired token",
                    "details", "Please login again to get a new token"
                ));
            }
            
            // Extract userId from token
            String userId = jwtUtil.getUserIdFromToken(token);
            log.info("User ID extracted from token: {}", userId);
            
            // Find user
            authUser user = authrepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
            log.info("User found: {}", user.getEmail());
            
            // Return user info
            return ResponseEntity.ok(Map.of(
                "fullName", user.getFullname(),
                "email", user.getEmail(),
                "role", user.getRole()
            ));
            
        } catch (RuntimeException e) {
            log.error("Runtime error in /me endpoint: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                "error", "Authentication failed",
                "details", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error in /me endpoint: ", e);
            return ResponseEntity.status(401).body(Map.of(
                "error", "Authentication failed",
                "details", e.getMessage() != null ? e.getMessage() : "Unknown error occurred"
            ));
        }
    }
    @GetMapping("/debug-token")
public ResponseEntity<?> debugToken(@RequestHeader(value = "Authorization", required = false) String authHeader) {
    try {
        log.info("=== DEBUG TOKEN ENDPOINT ===");
        
        if (authHeader == null) {
            return ResponseEntity.ok(Map.of(
                "message", "No Authorization header provided",
                "headers", Map.of("Authorization", "null")
            ));
        }
        
        String token = authHeader.replace("Bearer ", "");
        
        // Check if token is valid
        boolean isValid = jwtUtil.validateToken(token);
        
        Map<String, Object> result = Map.of(
            "tokenPresent", true,
            "tokenLength", token.length(),
            "tokenPreview", token.substring(0, Math.min(token.length(), 30)) + "...",
            "isValid", isValid
        );
        
        if (isValid) {
            String userId = jwtUtil.getUserIdFromToken(token);
            String role = jwtUtil.getRoleFromToken(token);
            result = Map.of(
                "tokenPresent", true,
                "tokenLength", token.length(),
                "tokenPreview", token.substring(0, Math.min(token.length(), 30)) + "...",
                "isValid", true,
                "userId", userId,
                "role", role
            );
        }
        
        return ResponseEntity.ok(result);
        
    } catch (Exception e) {
        return ResponseEntity.status(500).body(Map.of(
            "error", e.getMessage(),
            "stackTrace", e.getStackTrace().toString()
        ));
    }
}
}
