package com.example.auth.service;

import java.util.Map;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.auth.dto.LoginRequest;
import com.example.auth.dto.RegisterRequest;
import com.example.auth.model.authUser;
import com.example.auth.repository.authrepo;
import com.example.auth.security.JwtUtil;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class authService {
    private final authrepo Repo;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public authUser register(RegisterRequest req) {
        if (Repo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        authUser user = new authUser();
        user.setUsername(req.getUsername());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole()); // "PATIENT" | "DOCTOR" | "LAB_ASSISTANT"
        return Repo.save(user);
    }
    public Map<String, String> login(LoginRequest req) {
        authUser user = Repo.findByEmail(req.getEmail())
            .orElseThrow(() -> new RuntimeException("Invalid credentials"));
        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }
        String token = jwtUtil.generateToken(user.getId(), user.getRole());
        return Map.of("token", token, "role", user.getRole());
    }
}

