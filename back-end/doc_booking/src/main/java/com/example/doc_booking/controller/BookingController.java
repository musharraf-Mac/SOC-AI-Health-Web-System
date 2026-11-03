package com.example.doc_booking.controller;
import com.example.doc_booking.dto.*;
import com.example.doc_booking.model.*;
import com.example.doc_booking.security.BookingAccess;
import com.example.doc_booking.service.*;
import java.util.*;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
@RestController
@RequestMapping("/api/doc_booking")
public class BookingController {
    @ModelAttribute public void privateResponse(jakarta.servlet.http.HttpServletResponse response) { response.setHeader("Cache-Control", "no-store"); }
    @ExceptionHandler(org.springframework.dao.DataAccessException.class)
    public ResponseEntity<?> storageError() { return ResponseEntity.status(503).header("Cache-Control", "no-store").body(Map.of("error", "Appointment storage is temporarily unavailable. Please try again.")); }
    private final DoctorService doctors;
    private final BookingService bookings;
    private final BookingAccess access;
    public BookingController(DoctorService doctors, BookingService bookings, BookingAccess access) { this.doctors = doctors; this.bookings = bookings; this.access = access; }
    @GetMapping("/doctors") public List<Doctor> doctors() { access.require("PATIENT"); return doctors.getAllDoctors(); }
    @GetMapping("/doctor/me/availability") public Doctor availability() { return doctors.profile(access.require("DOCTOR")); }
    @PutMapping("/doctor/me/availability") public Doctor availability(@RequestBody AvailabilityRequest request) { return doctors.saveAvailability(access.require("DOCTOR"), request); }
    @GetMapping("/doctor/{email}") public Doctor doctor(@PathVariable String email) {
        access.require("PATIENT");
        var doctor = doctors.getDoctorByEmail(email);
        if (doctor == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered doctor not found");
        return doctor;
    }
    @PostMapping("/book") public ResponseEntity<Booking> book(@RequestBody BookingRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(bookings.createBooking(request, access.require("PATIENT")));
    }
    @GetMapping("/patient/{email}/bookings") public List<Booking> bookings(@PathVariable String email) {
        var patient = access.require("PATIENT");
        if (!patient.getEmail().equalsIgnoreCase(email)) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking access denied");
        return bookings.getPatientBookings(patient.getEmail());
    }
    @DeleteMapping("/delbooking/{id}") public Map<String, String> delete(@PathVariable String id) {
        bookings.deleteBooking(id, access.require("PATIENT")); return Map.of("message", "Booking deleted successfully", "id", id);
    }
    @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> error(ResponseStatusException e) {
        return ResponseEntity.status(e.getStatusCode()).body(Map.of("error", Objects.requireNonNullElse(e.getReason(), "Request failed")));
    }
}
