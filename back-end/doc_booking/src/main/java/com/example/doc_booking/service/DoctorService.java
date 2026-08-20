package com.example.doc_booking.service;

import com.example.doc_booking.model.Doctor;
import com.example.doc_booking.repository.DoctorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class DoctorService {
    @Autowired
    private DoctorRepository doctorRepository;

    public List<Doctor> getAllDoctors() {
        return doctorRepository.findAll();
    }

    public Doctor getDoctorByEmail(String email) {
        return doctorRepository.findByEmail(email).orElse(null);
    }
}
