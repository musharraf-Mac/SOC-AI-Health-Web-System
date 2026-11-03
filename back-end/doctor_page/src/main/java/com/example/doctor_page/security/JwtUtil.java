package com.example.doctor_page.security;

import java.nio.charset.StandardCharsets;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

@Component
public class JwtUtil {
    @org.springframework.beans.factory.annotation.Autowired
    private com.example.doctor_page.repository.RegisteredUserRepository users;
    @Value("${jwt.secret}")
    private String secret;

    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String getUserIdFromToken(String token) {
    Claims claims = Jwts.parser().verifyWith(getSigningKey()).build()
        .parseSignedClaims(token).getPayload();
    return claims.getSubject();
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser().verifyWith(getSigningKey()).build()
        .parseSignedClaims(token).getPayload();
    return claims.get("email", String.class); 
    }

    public String requireDoctorEmail(String header) {
        if (header == null || !header.startsWith("Bearer ") || !validateToken(header.substring(7)))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Login required");
        var claims = Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(header.substring(7)).getPayload();
        if (!"DOCTOR".equals(claims.get("role", String.class)))
            throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Doctor access required");
        String id = claims.getSubject();
        if (id == null || id.isBlank()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Account missing from login");
        var user = users.findById(id).orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Account no longer exists"));
        if (!"DOCTOR".equals(user.getRole())) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Doctor access required");
        if (user.getEmail() == null || user.getEmail().isBlank()) throw new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "Account email missing");
        return user.getEmail();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
