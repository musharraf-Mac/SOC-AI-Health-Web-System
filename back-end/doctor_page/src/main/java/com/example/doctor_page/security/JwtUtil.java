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

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(getSigningKey()).build().parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
