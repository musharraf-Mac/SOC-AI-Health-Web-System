package com.example.lab_rep.service;

import com.example.lab_rep.Security.ReportAccess;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import static org.junit.jupiter.api.Assertions.*;

class ReportAccessTest {
    private final String secret = "test-secret-with-more-than-thirty-two-characters";
    private final ReportAccess access = new ReportAccess(secret);
    private void login(String email, String role) {
        String token = Jwts.builder().subject("user-id").claim("email", email).claim("role", role)
                .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
        var request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }
    @AfterEach void clear() { RequestContextHolder.resetRequestAttributes(); }
    @Test void patientsCanOnlyReadTheirOwnReports() {
        login("patient@example.com", "PATIENT");
        assertDoesNotThrow(() -> access.patientOrStaff("patient@example.com"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, () -> access.patientOrStaff("other@example.com"));
        assertThrows(org.springframework.web.server.ResponseStatusException.class, access::staff);
    }
    @Test void staffCanManageReports() {
        login("lab@example.com", "LAB_STAFF");
        assertDoesNotThrow(access::staff);
        assertDoesNotThrow(() -> access.patientOrStaff("patient@example.com"));
    }
}
