package com.example.doc_booking.service;
import com.example.doc_booking.dto.BookingRequest;
import com.example.doc_booking.model.*;
import com.example.doc_booking.repository.BookingRepository;
import java.time.LocalDateTime;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class BookingService {
    private final BookingRepository bookings;
    private final DoctorService doctors;
    public BookingService(BookingRepository bookings, DoctorService doctors) { this.bookings = bookings; this.doctors = doctors; }
    public Booking createBooking(BookingRequest request, RegisteredUser patient) {
        Doctor doctor = doctors.getDoctorByEmail(request.getDoctorEmail());
        if (doctor == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Registered doctor not found");
        var slot = new AvailabilitySlot(request.getBookingDate(), request.getBookingTime());
        if (!doctors.isFuture(slot) || !doctor.getAvailableSlots().contains(slot))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This date/time is not available. Refresh the doctor's availability.");
        if (doctors.isReserved(doctor, slot, bookings.findByDoctorEmailIgnoreCase(doctor.getEmail())))
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This time slot has already been booked.");
        Booking booking = new Booking();
        // A deterministic _id and insert (not save/upsert) make simultaneous claims atomic in the existing collection.
        String slotKey = doctor.getEmail().toLowerCase(Locale.ROOT) + "|" + slot.date() + "|" + slot.time();
        booking.setId(UUID.nameUUIDFromBytes(slotKey.getBytes(StandardCharsets.UTF_8)).toString());
        booking.setPatientEmail(patient.getEmail()); booking.setPatientName(patient.getFullname());
        booking.setDoctorEmail(doctor.getEmail()); booking.setDoctorName(doctor.getName());
        booking.setBookingDate(slot.date()); booking.setBookingTime(slot.time());
        booking.setDurationMinutes(doctors.duration(doctor, slot));
        booking.setStatus("CONFIRMED"); booking.setCreatedAt(LocalDateTime.now());
        booking.setTokenNumber("TKN-" + slot.date().replace("-", "") + "-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
        try { return bookings.insert(booking); }
        catch (DuplicateKeyException e) { throw new ResponseStatusException(HttpStatus.CONFLICT, "This time slot has already been booked."); }
    }
    public List<Booking> getPatientBookings(String email) { return bookings.findByPatientEmailIgnoreCase(email); }
    public void deleteBooking(String id, RegisteredUser patient) {
        var booking = bookings.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Booking not found"));
        if (!patient.getEmail().equalsIgnoreCase(booking.getPatientEmail())) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Booking access denied");
        bookings.deleteById(id);
    }
}
