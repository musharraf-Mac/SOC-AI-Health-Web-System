package com.example.auth.service;

import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import com.example.auth.repository.authrepo;
import com.example.auth.model.authUser;
import org.springframework.security.crypto.password.PasswordEncoder;
import com.example.auth.dto.RegisterRequest;


@Service
@RequiredArgsConstructor
public class authService {
    private final authrepo Repo;
    private final PasswordEncoder passwordEncoder;

    public authUser register(RegisterRequest req) {
        if (Repo.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered");
        }
        authUser user = new authUser();
        user.setFullName(req.getFullName());
        user.setEmail(req.getEmail());
        user.setPassword(passwordEncoder.encode(req.getPassword()));
        user.setRole(req.getRole()); // "PATIENT" | "DOCTOR" | "LAB_ASSISTANT"
        return Repo.save(user);
    }
}

