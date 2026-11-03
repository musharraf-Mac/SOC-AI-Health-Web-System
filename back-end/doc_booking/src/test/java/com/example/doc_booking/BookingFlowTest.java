package com.example.doc_booking;
import com.example.doc_booking.model.*;
import com.example.doc_booking.dto.*;
import com.example.doc_booking.repository.*;
import com.example.doc_booking.service.*;
import com.example.doc_booking.security.BookingAccess;
import com.example.doc_booking.controller.BookingController;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
class BookingFlowTest {
    final DoctorRepository profiles = mock(DoctorRepository.class);
    final RegisteredUserRepository users = mock(RegisteredUserRepository.class);
    final BookingRepository appointments = mock(BookingRepository.class);
    final DoctorService doctors = new DoctorService(profiles, users, appointments, java.time.Clock.fixed(java.time.Instant.parse("2098-12-31T18:30:00Z"), java.time.ZoneId.of("Asia/Colombo")));
    final BookingService bookings = new BookingService(appointments, doctors);
    final String secret = "test-only-key-at-least-thirty-two-characters";
    RegisteredUser doctor, patient;
    Doctor profile;
    MockMvc mvc;
    @BeforeEach void setup() {
        doctor = user("doctor-id", "doctor@example.test", "DOCTOR", "Registered Doctor");
        patient = user("patient-id", "patient@example.test", "PATIENT", "Registered Patient");
        when(users.findByRole("DOCTOR")).thenReturn(List.of(doctor));
        when(users.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(users.findById(patient.getId())).thenReturn(Optional.of(patient));
        when(users.findByEmailIgnoreCase(doctor.getEmail())).thenReturn(Optional.of(doctor));
        profile = new Doctor(); profile.setId("existing-doctor-id"); profile.setEmail(doctor.getEmail()); profile.setName("Old name");
        profile.setAvailableSlots(List.of(new AvailabilitySlot("2099-01-01", "09:00"), new AvailabilitySlot("2099-01-02", "11:00")));
        when(profiles.findByEmailIgnoreCase(doctor.getEmail())).thenReturn(Optional.of(profile));
        when(profiles.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(appointments.insert(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));
        mvc = MockMvcBuilders.standaloneSetup(new BookingController(doctors, bookings, new BookingAccess(secret, users))).build();
    }
    RegisteredUser user(String id, String email, String role, String name) {
        var user = new RegisteredUser(); user.setId(id); user.setEmail(email); user.setRole(role); user.setFullname(name); return user;
    }
    String token(RegisteredUser user) {
        return "Bearer " + Jwts.builder().subject(user.getId()).claim("role", user.getRole()).claim("email", user.getEmail())
            .signWith(Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8))).compact();
    }
    BookingRequest request() {
        var r = new BookingRequest(); r.setDoctorEmail(doctor.getEmail()); r.setBookingDate("2099-01-01"); r.setBookingTime("09:00");
        r.setPatientEmail("forged@example.test"); r.setPatientName("Forged name"); return r;
    }
    @Test void directoryOnlyUsesRegisteredDoctorsAndExcludesBookedSlots() {
        var taken = new Booking(); taken.setBookingDate("2099-01-01"); taken.setBookingTime("09:00");
        when(appointments.findByDoctorEmailIgnoreCase(doctor.getEmail())).thenReturn(List.of(taken));
        var result = doctors.getAllDoctors();
        assertEquals(1, result.size()); assertEquals("Registered Doctor", result.get(0).getName());
        assertEquals(List.of(new AvailabilitySlot("2099-01-02", "11:00")), result.get(0).getAvailableSlots());
        verify(profiles, never()).findAll(); assertNull(doctors.getDoctorByEmail("fake@example.test"));
    }
    @Test void registeredDoctorWithoutAvailabilityIsNotBookable() {
        profile.setAvailableSlots(null);
        assertTrue(doctors.getAllDoctors().isEmpty());
    }
    @Test void saveUsesExistingProfileAndDateSpecificSlots() {
        var result = doctors.saveAvailability(doctor, new AvailabilityRequest("Cardiology", List.of(new AvailabilitySlot("2099-01-03", "10:00"))));
        assertEquals("existing-doctor-id", result.getId()); assertEquals(doctor.getEmail(), result.getEmail());
        assertEquals(List.of("2099-01-03"), result.getAvailableDates());
        assertEquals(List.of("10:00"), result.getAvailableTimes()); verify(profiles).save(result);
    }
    @Test void rejectsInvalidAndPastSlots() {
        for (var slot : List.of(new AvailabilitySlot("2000-01-01", "10:00"), new AvailabilitySlot("2099-02-30", "10:00"), new AvailabilitySlot("2099-01-01", "25:00"))) {
            assertThrows(ResponseStatusException.class, () -> doctors.saveAvailability(doctor, new AvailabilityRequest("General", List.of(slot))));
        }
        verify(profiles, never()).save(any());
    }
    @Test void createsBookingFromAuthenticatedPatientAndRegisteredDoctor() {
        var result = bookings.createBooking(request(), patient);
        assertEquals(patient.getEmail(), result.getPatientEmail()); assertEquals(patient.getFullname(), result.getPatientName());
        assertEquals(doctor.getFullname(), result.getDoctorName()); assertEquals("CONFIRMED", result.getStatus());
        verify(appointments).insert(result); verify(appointments, never()).save(any());
    }
    @Test void rejectsUnpublishedTimeAndUnregisteredDoctor() {
        var r = request(); r.setBookingTime("11:00");
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> bookings.createBooking(r, patient)).getStatusCode().value());
        r.setDoctorEmail("fake@example.test");
        assertEquals(404, assertThrows(ResponseStatusException.class, () -> bookings.createBooking(r, patient)).getStatusCode().value());
        verify(appointments, never()).insert(any(Booking.class));
    }
    @Test void simultaneousSlotClaimsShareIdAndDuplicateInsertReturnsConflict() {
        var first = bookings.createBooking(request(), patient);
        var second = bookings.createBooking(request(), user("p2", "p2@example.test", "PATIENT", "Second"));
        assertEquals(first.getId(), second.getId());
        when(appointments.insert(any(Booking.class))).thenThrow(new DuplicateKeyException("duplicate _id"));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> bookings.createBooking(request(), patient)).getStatusCode().value());
    }
    @Test void existingLegacyBookingBlocksSlot() {
        var existing = new Booking(); existing.setBookingDate("2099-01-01"); existing.setBookingTime("09:00");
        when(appointments.findByDoctorEmailIgnoreCase(doctor.getEmail())).thenReturn(List.of(existing));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> bookings.createBooking(request(), patient)).getStatusCode().value());
    }
    @Test void existingAppointmentBlocksOverlappingNewTimeRange() {
        var existing = new Booking(); existing.setBookingDate("2099-01-01"); existing.setBookingTime("08:45"); existing.setDurationMinutes(30);
        when(appointments.findByDoctorEmailIgnoreCase(doctor.getEmail())).thenReturn(List.of(existing));
        assertEquals(409, assertThrows(ResponseStatusException.class, () -> bookings.createBooking(request(), patient)).getStatusCode().value());
        assertTrue(doctors.getAllDoctors().getFirst().getAvailableSlots().stream().noneMatch(s -> s.date().equals("2099-01-01")));
    }
    @Test void rulePayloadPersistsInExistingDoctorAndExclusionBlocksBooking() throws Exception {
        profile.setOneOffSlots(List.of());
        String day = java.time.LocalDate.parse("2099-01-01").getDayOfWeek().name();
        String json = """
            {"specialization":"General","availabilityRules":[{"id":"r1","startDate":"2099-01-01","daysOfWeek":["%s"],"startTime":"09:00","endTime":"10:00","slotMinutes":30,"repeatWeekly":true}],"excludedDates":["2099-01-01"]}
            """.formatted(day);
        mvc.perform(put("/api/doc_booking/doctor/me/availability").header("Authorization", token(patient)).contentType("application/json").content(json)).andExpect(status().isForbidden());
        mvc.perform(put("/api/doc_booking/doctor/me/availability").header("Authorization", token(doctor)).contentType("application/json").content(json))
            .andExpect(status().isOk()).andExpect(header().string("Cache-Control", "no-store"))
            .andExpect(jsonPath("$.id").value("existing-doctor-id")).andExpect(jsonPath("$.windowEnd").value("2099-01-14"))
            .andExpect(jsonPath("$.availableSlots[0].date").value("2099-01-08"));
        assertEquals(1, profile.getAvailabilityRules().size());
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> bookings.createBooking(request(), patient)).getStatusCode().value());
        verify(appointments, never()).deleteAll(); verify(appointments, never()).deleteById(any());
        var nextWeek = request(); nextWeek.setBookingDate("2099-01-08");
        assertEquals(30, bookings.createBooking(nextWeek, patient).getDurationMinutes());
        nextWeek.setBookingDate("2099-01-15");
        assertEquals(400, assertThrows(ResponseStatusException.class, () -> bookings.createBooking(nextWeek, patient)).getStatusCode().value());
    }
    @Test void deleteRequiresOwnership() {
        var booking = new Booking(); booking.setPatientEmail("other@example.test");
        when(appointments.findById("id")).thenReturn(Optional.of(booking));
        assertEquals(403, assertThrows(ResponseStatusException.class, () -> bookings.deleteBooking("id", patient)).getStatusCode().value());
        verify(appointments, never()).deleteById(any());
        booking.setPatientEmail(patient.getEmail()); bookings.deleteBooking("id", patient); verify(appointments).deleteById("id");
    }
    @Test void endpointsRequireCorrectAccountAndPatientOwnership() throws Exception {
        mvc.perform(get("/api/doc_booking/doctors")).andExpect(status().isUnauthorized());
        mvc.perform(get("/api/doc_booking/doctor/me/availability").header("Authorization", token(patient))).andExpect(status().isForbidden());
        mvc.perform(get("/api/doc_booking/doctor/me/availability").header("Authorization", token(doctor))).andExpect(status().isOk()).andExpect(jsonPath("$.name").value("Registered Doctor"));
        mvc.perform(get("/api/doc_booking/patient/other@example.test/bookings").header("Authorization", token(patient))).andExpect(status().isForbidden());
    }
}
