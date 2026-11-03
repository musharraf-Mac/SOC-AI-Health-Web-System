package com.example.doctor_page;

import com.example.doctor_page.controller.DoctorController;
import com.example.doctor_page.model.*;
import com.example.doctor_page.repository.*;
import com.example.doctor_page.security.JwtUtil;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DoctorAppointmentsTest {
    final String secret = "test-only-signing-key-at-least-32-characters";
    final PatientRepository bookings = mock(PatientRepository.class);
    final PrescriptionRepository prescriptions = mock(PrescriptionRepository.class);
    final RegisteredUserRepository users = mock(RegisteredUserRepository.class);
    MockMvc mvc;
    @BeforeEach void setup() {
        var jwt = new JwtUtil(); ReflectionTestUtils.setField(jwt, "secret", secret); ReflectionTestUtils.setField(jwt, "users", users);
        var controller = new DoctorController(jwt); ReflectionTestUtils.setField(controller, "patientRepo", bookings); ReflectionTestUtils.setField(controller, "prescriptionRepo", prescriptions);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
        for (String id : List.of("d1", "d2")) {
            var user = new RegisteredUser(); user.setId(id); user.setEmail(id + "@example.test"); user.setRole("DOCTOR");
            when(users.findById(id)).thenReturn(Optional.of(user));
        }
    }
    String token(String id, String role) {
        // The current account, not this deliberately different email claim, identifies the doctor.
        return "Bearer " + Jwts.builder().subject(id).claim("role", role).claim("email", "different@example.test")
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }
    Patient appointment(String doctor) {
        var value = new Patient(); value.setId("booking-" + doctor); value.setDoctorEmail(doctor + "@example.test");
        value.setDoctorName("Registered " + doctor); value.setPatientEmail("patient@example.test");
        value.setpatientName("Booked Patient"); value.setTokenNumber("TOKEN-" + doctor); return value;
    }
    @Test void onlyTheCurrentRegisteredDoctorsBookingsAreReturned() throws Exception {
        for (String id : List.of("d1", "d2")) {
            var booking = appointment(id);
            when(bookings.findByDoctorEmailIgnoreCase(id + "@example.test")).thenReturn(List.of(booking));
            mvc.perform(get("/api/doctor/patients").header("Authorization", token(id, "DOCTOR")))
                .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0].doctorEmail").value(id + "@example.test"));
            verify(bookings).findByDoctorEmailIgnoreCase(id + "@example.test");
        }
        verify(bookings, never()).findAll();
    }
    @Test void missingPatientRevokedAndDeletedLoginsCannotReadDoctorRecords() throws Exception {
        mvc.perform(get("/api/doctor/patients")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/doctor/patients").header("Authorization", token("p", "PATIENT"))).andExpect(status().isForbidden());
        when(users.findById("d1")).thenReturn(Optional.empty());
        mvc.perform(get("/api/doctor/patients").header("Authorization", token("d1", "DOCTOR"))).andExpect(status().isUnauthorized());
        var changed = new RegisteredUser(); changed.setRole("PATIENT");
        when(users.findById("d2")).thenReturn(Optional.of(changed));
        mvc.perform(get("/api/doctor/patients").header("Authorization", token("d2", "DOCTOR"))).andExpect(status().isForbidden());
        verifyNoInteractions(bookings);
    }
    @Test void tokenLookupAndPrescriptionHistoryRequireDoctorOwnership() throws Exception {
        when(bookings.findByTokenNumberAndDoctorEmailIgnoreCase("TOKEN-d1", "d1@example.test")).thenReturn(Optional.of(appointment("d1")));
        mvc.perform(get("/api/doctor/patients/TOKEN-d1").header("Authorization", token("d1", "DOCTOR"))).andExpect(status().isOk());
        mvc.perform(get("/api/doctor/patients/TOKEN-d1").header("Authorization", token("d2", "DOCTOR"))).andExpect(status().isNotFound());
        when(bookings.findByPatientEmailIgnoreCaseAndDoctorEmailIgnoreCase("patient@example.test", "d1@example.test")).thenReturn(List.of(appointment("d1")));
        mvc.perform(get("/api/doctor/prescriptions/patient@example.test").header("Authorization", token("d1", "DOCTOR"))).andExpect(status().isOk());
        verify(prescriptions).findByPatientEmailAndDoctorEmailIgnoreCase("patient@example.test", "d1@example.test");
        mvc.perform(get("/api/doctor/prescriptions/patient@example.test").header("Authorization", token("d2", "DOCTOR"))).andExpect(status().isForbidden());
        verify(prescriptions, never()).findByPatientEmailAndDoctorEmailIgnoreCase("patient@example.test", "d2@example.test");
    }
    @Test void prescriptionCreationUsesOwnedBookingAndCannotOverwriteSuppliedId() throws Exception {
        when(bookings.findByTokenNumberAndDoctorEmailIgnoreCase("TOKEN-d1", "d1@example.test")).thenReturn(Optional.of(appointment("d1")));
        when(prescriptions.insert(any(Prescription.class))).thenAnswer(inv -> inv.getArgument(0));
        String body = """
            {"id":"someone-elses-prescription","tokenNumber":"TOKEN-d1","doctorEmail":"d2@example.test","patientEmail":"forged@example.test","patientAge":32,"prescriptionText":"Test"}
            """;
        mvc.perform(post("/api/doctor/prescriptions").header("Authorization", token("d1", "DOCTOR")).contentType("application/json").content(body))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.doctorEmail").value("d1@example.test"))
            .andExpect(jsonPath("$.patientEmail").value("patient@example.test")).andExpect(jsonPath("$.patientName").value("Booked Patient"));
        var captor = org.mockito.ArgumentCaptor.forClass(Prescription.class); verify(prescriptions).insert(captor.capture()); assertNull(captor.getValue().getId());
        verify(prescriptions, never()).save(any());
        mvc.perform(post("/api/doctor/prescriptions").header("Authorization", token("d2", "DOCTOR")).contentType("application/json").content(body)).andExpect(status().isNotFound());
    }
    @Test void prescriptionDeletionRequiresOwnerAndAnonymousCannotDelete() throws Exception {
        var prescription = new Prescription(); prescription.setId("rx1"); prescription.setDoctorEmail("d1@example.test");
        when(prescriptions.findById("rx1")).thenReturn(Optional.of(prescription));
        mvc.perform(delete("/api/doctor/prescriptions/delete/rx1")).andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/doctor/prescriptions/delete/rx1").header("Authorization", token("d2", "DOCTOR"))).andExpect(status().isNotFound());
        verify(prescriptions, never()).delete(any());
        mvc.perform(delete("/api/doctor/prescriptions/delete/rx1").header("Authorization", token("d1", "DOCTOR"))).andExpect(status().isOk());
        verify(prescriptions).delete(prescription);
    }
}
