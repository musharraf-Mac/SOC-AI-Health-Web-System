package com.example.doc_booking.repository;

import java.util.List;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.example.doc_booking.model.Booking;

@Repository
public interface BookingRepository extends MongoRepository<Booking, String> {
    List<Booking> findByPatientEmail(String patientEmail);
    List<Booking> findByDoctorEmailAndBookingDate(String doctorEmail, String bookingDate);
    List<Booking> findByDoctorEmail(String doctorEmail);
}
