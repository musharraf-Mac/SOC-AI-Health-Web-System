package com.example.doc_booking.service;
import com.example.doc_booking.model.*;
import com.example.doc_booking.dto.AvailabilityRequest;
import com.example.doc_booking.repository.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
@Service
public class DoctorService {
    private final DoctorRepository doctors;
    private final RegisteredUserRepository users;
    private final BookingRepository bookings;
    private final AvailabilitySchedule schedule;
    @org.springframework.beans.factory.annotation.Autowired
    public DoctorService(DoctorRepository doctors, RegisteredUserRepository users, BookingRepository bookings,
            @Value("${booking.time-zone:Asia/Colombo}") String zone) {
        this(doctors, users, bookings, Clock.system(ZoneId.of(zone)));
    }
    public DoctorService(DoctorRepository doctors, RegisteredUserRepository users, BookingRepository bookings, Clock clock) {
        this.doctors = doctors; this.users = users; this.bookings = bookings; this.schedule = new AvailabilitySchedule(clock);
    }
    public boolean isFuture(AvailabilitySlot slot) {
        return schedule.isFuture(slot);
    }
    public int duration(Doctor doctor, AvailabilitySlot slot) { return schedule.duration(doctor, slot); }
    public boolean isReserved(Doctor doctor, AvailabilitySlot slot, List<Booking> reserved) {
        int start = LocalTime.parse(slot.time()).toSecondOfDay() / 60;
        int end = start + duration(doctor, slot);
        return reserved.stream().filter(b -> slot.date().equals(b.getBookingDate())).anyMatch(b -> {
            try {
                int bookedStart = LocalTime.parse(b.getBookingTime()).toSecondOfDay() / 60;
                int bookedEnd = bookedStart + (b.getDurationMinutes() > 0 ? b.getDurationMinutes() : 30);
                return start < bookedEnd && bookedStart < end;
            } catch (RuntimeException invalidLegacyTime) { return slot.time().equals(b.getBookingTime()); }
        });
    }
    public Doctor profile(RegisteredUser user) {
        return preview(storedProfile(user));
    }
    private Doctor storedProfile(RegisteredUser user) {
        var doctor = doctors.findByEmailIgnoreCase(user.getEmail()).orElseGet(Doctor::new);
        if (doctor.getId() == null) doctor.setId(user.getId());
        doctor.setEmail(user.getEmail()); doctor.setName(user.getFullname());
        if (doctor.getSpecialization() == null) doctor.setSpecialization("");
        if (doctor.getOneOffSlots() == null) doctor.setOneOffSlots(doctor.getAvailableSlots() == null ? List.of() : doctor.getAvailableSlots());
        if (doctor.getAvailabilityRules() == null) doctor.setAvailabilityRules(List.of());
        if (doctor.getExcludedDates() == null) doctor.setExcludedDates(List.of());
        return doctor;
    }
    private Doctor preview(Doctor doctor) {
        setSlots(doctor, schedule.expand(doctor.getAvailabilityRules(), doctor.getExcludedDates(), doctor.getOneOffSlots()));
        doctor.setWindowStart(schedule.today().toString()); doctor.setWindowEnd(schedule.lastDay().toString());
        return doctor;
    }
    private void setSlots(Doctor doctor, List<AvailabilitySlot> slots) {
        doctor.setAvailableSlots(slots);
        doctor.setAvailableDates(slots.stream().map(AvailabilitySlot::date).distinct().sorted().toList());
        doctor.setAvailableTimes(slots.stream().map(AvailabilitySlot::time).distinct().sorted().toList());
    }
    public Doctor saveAvailability(RegisteredUser user, AvailabilityRequest request) {
        if (request == null || request.specialization() == null || request.specialization().isBlank() || request.specialization().length() > 120) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Enter a specialization (maximum 120 characters).");
        }
        var doctor = storedProfile(user);
        doctor.setSpecialization(request.specialization().trim());
        if (request.availabilityRules() != null) {
            doctor.setAvailabilityRules(schedule.validateRules(request.availabilityRules()));
            doctor.setExcludedDates(schedule.validateExclusions(request.excludedDates()));
        } else {
            // Accept the previous individual-slot payload while clients are being redeployed.
            if (request.availableSlots() == null || request.availableSlots().size() > 500 || request.availableSlots().stream().anyMatch(slot -> !isFuture(slot)))
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Choose valid future slots (maximum 500).");
            doctor.setOneOffSlots(request.availableSlots().stream().distinct().toList());
        }
        doctor.setOneOffSlots(doctor.getOneOffSlots().stream().filter(this::isFuture).toList());
        // Save the rules, not an ever-growing list of future recurrence copies.
        setSlots(doctor, doctor.getOneOffSlots());
        return preview(doctors.save(doctor));
    }
    public Doctor getDoctorByEmail(String email) {
        if (email == null) return null;
        return users.findByEmailIgnoreCase(email).filter(u -> "DOCTOR".equals(u.getRole())).map(this::profile).orElse(null);
    }
    public List<Doctor> getAllDoctors() {
        return users.findByRole("DOCTOR").stream().map(this::profile).map(doctor -> {
            var reserved = bookings.findByDoctorEmailIgnoreCase(doctor.getEmail());
            setSlots(doctor, doctor.getAvailableSlots().stream().filter(slot -> !isReserved(doctor, slot, reserved)).toList());
            return doctor;
        }).filter(d -> !d.getAvailableSlots().isEmpty()).sorted(Comparator.comparing(Doctor::getName, String.CASE_INSENSITIVE_ORDER)).toList();
    }
}
