package com.example.doc_booking.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.doc_booking.dto.BookingRequest;
import com.example.doc_booking.model.Booking;
import com.example.doc_booking.model.Doctor;
import com.example.doc_booking.service.BookingService;
import com.example.doc_booking.service.DoctorService;

@RestController
@RequestMapping("/api/doc_booking")
// @CrossOrigin(origins = "*")
public class BookingController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private BookingService bookingService;

    @GetMapping("/doctors")
    public ResponseEntity<List<Doctor>> getAllDoctors() {
        return ResponseEntity.ok(doctorService.getAllDoctors());
    }

    @GetMapping("/doctor/{email}")
    public ResponseEntity<Doctor> getDoctorByEmail(@PathVariable String email) {
        Doctor doctor = doctorService.getDoctorByEmail(email);
        if (doctor == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(doctor);
    }

    @PostMapping("/book")
    public ResponseEntity<?> bookAppointment(@RequestBody BookingRequest request) {
        try {
            Booking booking = bookingService.createBooking(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(booking);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping("/patient/{patientEmail}/bookings")
    public ResponseEntity<List<Booking>> getPatientBookings(@PathVariable String patientEmail) {
        return ResponseEntity.ok(bookingService.getPatientBookings(patientEmail));
    }

    @DeleteMapping("/delbooking/{bookingId}")
public ResponseEntity<?> deleteBooking(@PathVariable String bookingId) {  // ⭐ Changed Long to String
    try {
        if (!bookingService.existsById(bookingId)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of("error", "Booking not found with id: " + bookingId));
        }
        
        bookingService.deleteBooking(bookingId);
        return ResponseEntity.ok(Map.of(
            "message", "Booking deleted successfully",
            "id", bookingId
        ));
        
    } catch (Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(Map.of("error", "Failed to delete booking: " + e.getMessage()));
    }
}
}
