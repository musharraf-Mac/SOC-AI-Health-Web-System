package com.example.doctor_page.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;


@Document(collection = "bookings")
public class Patient {
    @Id
    private String id;
    private String patientName;
    private String patientEmail;
    private String doctorName;
    private String doctorEmail;
    private String bookingDate;
    private String bookingTime;
    private String tokenNumber;
    private String status; // PENDING, CONFIRMED, CANCELLED
    private String createdAt;

    // Constructors
    public Patient() {}

    public Patient(String patientName, String patientEmail, String doctorName, String doctorEmail, String bookingDate, String bookingTime, String tokenNumber, String status, String createdAt) {
        this.patientName = patientName;
        this.patientEmail = patientEmail;
        this.doctorName = doctorName;
        this.doctorEmail = doctorEmail;
        this.bookingDate = bookingDate;
        this.bookingTime = bookingTime;
        this.tokenNumber = tokenNumber;
        this.status = status;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getpatientName() {
        return patientName;
    }

    public void setpatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getPatientEmail() {
        return patientEmail;
    }

    public void setPatientEmail(String patientEmail) {
        this.patientEmail = patientEmail;
    }

    public String getDoctorName() {
        return doctorName;
    }
    public void setDoctorName(String doctorName) {
        this.doctorName = doctorName;
    }
    public String getDoctorEmail() {
        return doctorEmail;
    }
    public void setDoctorEmail(String doctorEmail) {
        this.doctorEmail = doctorEmail;
    }
    public String getBookingDate() {
        return bookingDate;
    }
    public void setBookingDate(String bookingDate) {
        this.bookingDate = bookingDate;
    }
    public String getBookingTime() {
        return bookingTime;
    }
    public void setBookingTime(String bookingTime) {
        this.bookingTime = bookingTime;
    }
    public String getTokenNumber() {
        return tokenNumber;
    }
    public void setTokenNumber(String tokenNumber) {
        this.tokenNumber = tokenNumber;
    }
    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
    public String getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
    
}


