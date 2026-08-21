package com.example.doc_booking.repository;

import com.example.doc_booking.model.Booking;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByPatientEmail(String patientEmail);
    List<Booking> findByDoctorEmailAndBookingDate(String doctorEmail, String bookingDate);
}
