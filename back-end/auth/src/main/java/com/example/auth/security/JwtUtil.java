package com.example.auth.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    private final long expirationMs = 3600_000; // 1 hour

    private SecretKey getSigningKey() {
        if (secret == null || secret.isEmpty()) {
            log.error("JWT_SECRET is not configured");
            throw new IllegalStateException("JWT secret is not configured");
        }
        
        if (secret.length() < 32) {
            log.warn("JWT_SECRET is too short ({} characters). Should be at least 32 characters.", secret.length());
        }
        
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String userId, String role, String email) {
        try {
            Date now = new Date();
            Date expiryDate = new Date(now.getTime() + expirationMs);
            
            log.info("Generating token for user: {}, role: {}, email: {}", userId, role, email);
            
            return Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .claim("email", email)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(getSigningKey())
                .compact();
        } catch (Exception e) {
            log.error("Error generating token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate JWT token: " + e.getMessage());
        }
    }

    public String getUserIdFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return claims.getSubject();
        } catch (ExpiredJwtException e) {
            log.error("Token expired: {}", e.getMessage());
            throw new RuntimeException("Token has expired. Please login again.");
        } catch (MalformedJwtException e) {
            log.error("Malformed token: {}", e.getMessage());
            throw new RuntimeException("Invalid token format. Please provide a valid token.");
        } catch (SignatureException e) {
            log.error("Invalid signature: {}", e.getMessage());
            throw new RuntimeException("Invalid token signature. Token may have been tampered with.");
        } catch (UnsupportedJwtException e) {
            log.error("Unsupported token: {}", e.getMessage());
            throw new RuntimeException("Unsupported token format.");
        } catch (Exception e) {
            log.error("Error extracting userId from token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse token: " + e.getMessage());
        }
    }

    public String getRoleFromToken(String token) {
        try {
            Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return claims.get("role", String.class);
        } catch (Exception e) {
            log.error("Error extracting role from token: {}", e.getMessage());
            throw new RuntimeException("Failed to extract role from token: " + e.getMessage());
        }
    }

    public boolean validateToken(String token) {
        try {
            if (token == null || token.isEmpty()) {
                log.warn("Token is null or empty");
                return false;
            }
            
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token);
            
            log.info("Token validated successfully");
            return true;
            
        } catch (ExpiredJwtException e) {
            log.warn("Token validation failed: Token expired");
            return false;
        } catch (MalformedJwtException e) {
            log.warn("Token validation failed: Malformed token");
            return false;
        } catch (SignatureException e) {
            log.warn("Token validation failed: Invalid signature");
            return false;
        } catch (UnsupportedJwtException e) {
            log.warn("Token validation failed: Unsupported token");
            return false;
        } catch (IllegalArgumentException e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        } catch (Exception e) {
            log.warn("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}