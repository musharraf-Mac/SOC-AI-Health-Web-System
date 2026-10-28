package com.example.lab_rep.Security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.server.ResponseStatusException;

@Component
public class ReportAccess {
    private final String secret;
    public ReportAccess(@Value("${JWT_SECRET}") String secret) { this.secret = secret; }
    private Claims claims() {
        var attributes = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        String header = attributes.getRequest().getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Login required");
        try {
            return Jwts.parser().verifyWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8)))
                    .build().parseSignedClaims(header.substring(7)).getPayload();
        } catch (Exception failure) { throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired login"); }
    }
    public void staff() {
        if (!"LAB_STAFF".equals(claims().get("role", String.class))) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Lab staff access required");
    }
    public void patientOrStaff(String email) {
        Claims user = claims();
        if ("LAB_STAFF".equals(user.get("role", String.class))) return;
        if (!"PATIENT".equals(user.get("role", String.class)) || email == null || !email.equalsIgnoreCase(user.get("email", String.class)))
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Report access denied");
    }
}
