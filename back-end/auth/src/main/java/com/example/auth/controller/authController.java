package com.example.auth.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
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
            
            if (authHeader == null || authHeader.isEmpty()) {
                log.warn("Authorization header is missing or empty");
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Authorization header is required",
                    "details", "Please provide a Bearer token"
                ));
            }
            
            if (!authHeader.startsWith("Bearer ")) {
                log.warn("Authorization header doesn't start with 'Bearer '");
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid Authorization header format",
                    "details", "Header must start with 'Bearer '"
                ));
            }

            String token = authHeader.substring(7);
            log.info("Token extracted: {}...", token.substring(0, Math.min(token.length(), 30)));
            
            boolean isValid = jwtUtil.validateToken(token);
            log.info("Token validation result: {}", isValid);
            
            if (!isValid) {
                log.warn("Token validation failed");
                return ResponseEntity.status(401).body(Map.of(
                    "error", "Invalid or expired token",
                    "details", "Please login again to get a new token"
                ));
            }
            
            String userId = jwtUtil.getUserIdFromToken(token);
            log.info("User ID extracted from token: {}", userId);
            
            authUser user = authrepo.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));
            
            log.info("User found: {}", user.getEmail());
            
            // DEBUG: Log all user fields
            log.info("User fullname: '{}'", user.getFullname());
            log.info("User email: '{}'", user.getEmail());
            log.info("User role: '{}'", user.getRole());
            
            // Use HashMap instead of Map.of() to handle null values
            Map<String, Object> response = new HashMap<>();
            
            // Add each field with null checking
            response.put("fullname", user.getFullname() != null ? user.getFullname() : "Unknown");
            response.put("email", user.getEmail() != null ? user.getEmail() : "No email");
            response.put("role", user.getRole() != null ? user.getRole() : "No role");
            
            log.info("Response map: {}", response);
            
            return ResponseEntity.ok(response);
            
        } catch (NullPointerException e) {
            log.error("NullPointerException in /me endpoint: ", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "details", "A null value was encountered. Please check user data."
            ));
        } catch (RuntimeException e) {
            log.error("Runtime error in /me endpoint: {}", e.getMessage());
            return ResponseEntity.status(401).body(Map.of(
                "error", "Authentication failed",
                "details", e.getMessage()
            ));
        } catch (Exception e) {
            log.error("Unexpected error in /me endpoint: ", e);
            return ResponseEntity.status(500).body(Map.of(
                "error", "Internal server error",
                "details", e.getMessage() != null ? e.getMessage() : "Unknown error occurred"
            ));
        }
    }
    @PostMapping("/logout")
public ResponseEntity<?> logout(@RequestHeader(value = "Authorization", required = false) String authHeader) {
    // JWT is stateless — nothing to invalidate server-side.
    // Real logout happens on the frontend by clearing localStorage.
    // This endpoint exists for API completeness / Swagger docs.
    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return ResponseEntity.status(401).body(Map.of(
            "error", "Authorization header is required"
        ));
    }
    return ResponseEntity.ok(Map.of("message", "Logged out successfully"));
}

@DeleteMapping("/delete")
public ResponseEntity<?> deleteAccount(@RequestHeader(value = "Authorization", required = false) String authHeader) {
    try {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "Authorization header is required"
            ));
        }

        String token = authHeader.substring(7);

        if (!jwtUtil.validateToken(token)) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "Invalid or expired token",
                "details", "Please login again to get a new token"
            ));
        }

        String userId = jwtUtil.getUserIdFromToken(token);
        log.info("Delete account requested for userId: {}", userId);

        authUser user = authrepo.findById(userId)
            .orElseThrow(() -> new RuntimeException("User not found with id: " + userId));

        authrepo.delete(user);
        log.info("Account deleted for email: {}", user.getEmail());

        return ResponseEntity.ok(Map.of("message", "Account deleted successfully"));

    } catch (RuntimeException e) {
        log.error("Runtime error in /delete-account endpoint: {}", e.getMessage());
        return ResponseEntity.status(404).body(Map.of(
            "error", "Account deletion failed",
            "details", e.getMessage()
        ));
    } catch (Exception e) {
        log.error("Unexpected error in /delete-account endpoint: ", e);
        return ResponseEntity.status(500).body(Map.of(
            "error", "Internal server error",
            "details", e.getMessage() != null ? e.getMessage() : "Unknown error occurred"
        ));
    }
}
   
}
