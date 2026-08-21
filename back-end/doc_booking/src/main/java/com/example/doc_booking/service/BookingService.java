package com.example.doc_booking.service;

import com.example.doc_booking.dto.BookingRequest;
import com.example.doc_booking.model.Booking;
import com.example.doc_booking.model.Doctor;
import com.example.doc_booking.repository.BookingRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {
    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private DoctorService doctorService;

    public Booking createBooking(BookingRequest request) {
        Doctor doctor = doctorService.getDoctorByEmail(request.getDoctorEmail());
        if (doctor == null) {
            throw new RuntimeException("Doctor not found");
        }

        List<Booking> existingBookings = bookingRepository
            .findByDoctorEmailAndBookingDate(request.getDoctorEmail(), request.getBookingDate());
        
        boolean slotTaken = existingBookings.stream()
            .anyMatch(b -> b.getBookingTime().equals(request.getBookingTime()));
        
        if (slotTaken) {
            throw new RuntimeException("Time slot already booked");
        }

        Booking booking = new Booking();
        booking.setPatientEmail(request.getPatientEmail());
        booking.setPatientName(request.getPatientName());
        booking.setDoctorEmail(request.getDoctorEmail());
        booking.setDoctorName(doctor.getName());
        booking.setBookingDate(request.getBookingDate());
        booking.setBookingTime(request.getBookingTime());
        booking.setStatus("CONFIRMED");
        booking.setCreatedAt(LocalDateTime.now());
        
        String token = generateToken(request.getBookingDate(), request.getDoctorEmail());
        booking.setTokenNumber(token);

        return bookingRepository.save(booking);
    }

    public List<Booking> getPatientBookings(String patientEmail) {
        return bookingRepository.findByPatientEmail(patientEmail);
    }

    private String generateToken(String date, String doctorEmail) {
        String datePrefix = date.replace("-", "");
        long count = bookingRepository.findByDoctorEmailAndBookingDate(doctorEmail, date).size() + 1;
        return "TKN-" + datePrefix + "-" + String.format("%03d", count);
    }
}
