package com.example.auth.security;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;

import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.Claims;


import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    private final long expirationMs = 3600000; // 1 hour in milliseconds

    public String generateToken(String userId, String role){
        return Jwts.builder()
                .setSubject(userId)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationMs))
                .signWith(Keys.hmacShaKeyFor(secret.getBytes()), SignatureAlgorithm.HS256)
                .compact();
    }
    public String getUserIdFromToken(String token) {
    Claims claims = Jwts.parser()
        .setSigningKey(Keys.hmacShaKeyFor(secret.getBytes()))
        .parseClaimsJws(token)
        .getBody();
    return claims.getSubject();
}
}
