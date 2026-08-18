package com.example.auth.service;

import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.model.authUser;
import com.example.auth.repository.authrepo;
import com.example.auth.security.JwtUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Service
@Slf4j
@RequiredArgsConstructor
public class authService {
    private final authrepo Repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public authUser register(RegisterRequest req) {
        
        try{
            log.info("Verifying database connection...");
        
        if (Repo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        authUser user = new authUser();
        user.setFullname(req.getFullname());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole()); // "PATIENT" | "DOCTOR" | "LAB_ASSISTANT"

        authUser savedUser = Repo.save(user); //Force save to database
        if (savedUser == null || savedUser.getId() == null){
                throw new RuntimeException("Failed to save user - no ID generated");
        }

        boolean exists = Repo.existsByEmail(req.getEmail());
        if (!exists){
            throw new RuntimeException("User was not saved to database");
        }

        log.info("User registered successfully with ID: {}", savedUser.getId());
        return savedUser;
    }
    catch (DataAccessResourceFailureException e) {
            // This catches database connection failures
            log.error("Database connection failed! Cannot save to tb_soc_health_db", e);
            throw new RuntimeException("Database connection failed: Cannot access tb_soc_health_db", e);
            
        } catch (DataAccessException e) {
            // This catches other database errors
            log.error("Database error while saving user", e);
            throw new RuntimeException("Database error: Could not save user to tb_soc_health_db", e);
            
        } catch (Exception e) {
            log.error("Unexpected error while saving user", e);
            throw new RuntimeException("Failed to save user: " + e.getMessage(), e);
        }
        
    }

    public Map<String, String> login(LoginRequest req) {
        try {
            Repo.count();           
        
        authUser user = Repo.findByEmail(req.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        log.info("Token generated for user ID : {}", user.getEmail());
        return Map.of("token", token, "role", user.getRole());
    }
    catch (DataAccessResourceFailureException e) {
            log.error("Cannot access soc_db for login", e);
            throw new RuntimeException("Database connection failed: Cannot access tb_soc_health_db", e);
}
}
}

