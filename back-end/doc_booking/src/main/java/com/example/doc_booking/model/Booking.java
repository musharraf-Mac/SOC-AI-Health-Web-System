package com.example.doc_booking.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import java.time.LocalDateTime;

@Document(collection = "bookings")
public class Booking {
    @Id
    private String id;
    private String patientEmail;
    private String patientName;
    private String doctorEmail;
    private String doctorName;
    private String bookingDate;
    private String bookingTime;
    private String tokenNumber;
    private String status;
    private LocalDateTime createdAt;
    private String prescriptionQR;
    private String labReportUrl;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getPatientEmail() { return patientEmail; }
    public void setPatientEmail(String patientEmail) { this.patientEmail = patientEmail; }
    public String getPatientName() { return patientName; }
    public void setPatientName(String patientName) { this.patientName = patientName; }
    public String getDoctorEmail() { return doctorEmail; }
    public void setDoctorEmail(String doctorEmail) { this.doctorEmail = doctorEmail; }
    public String getDoctorName() { return doctorName; }
    public void setDoctorName(String doctorName) { this.doctorName = doctorName; }
    public String getBookingDate() { return bookingDate; }
    public void setBookingDate(String bookingDate) { this.bookingDate = bookingDate; }
    public String getBookingTime() { return bookingTime; }
    public void setBookingTime(String bookingTime) { this.bookingTime = bookingTime; }
    public String getTokenNumber() { return tokenNumber; }
    public void setTokenNumber(String tokenNumber) { this.tokenNumber = tokenNumber; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public String getPrescriptionQR() { return prescriptionQR; }
    public void setPrescriptionQR(String prescriptionQR) { this.prescriptionQR = prescriptionQR; }
    public String getLabReportUrl() { return labReportUrl; }
    public void setLabReportUrl(String labReportUrl) { this.labReportUrl = labReportUrl; }
}
