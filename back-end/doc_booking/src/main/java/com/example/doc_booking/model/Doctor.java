package com.example.doc_booking.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "doctor")
public class Doctor {
    @Id
    private String id;
    private String name;
    private String email;
    private String specialization;
    private List<AvailabilityRule> availabilityRules;
    private List<String> excludedDates;
    // Preserve existing manually published slots separately from generated recurring slots.
    private List<AvailabilitySlot> oneOffSlots;
    @org.springframework.data.annotation.Transient
    private String windowStart;
    @org.springframework.data.annotation.Transient
    private String windowEnd;
    public List<AvailabilityRule> getAvailabilityRules() { return availabilityRules; }
    public void setAvailabilityRules(List<AvailabilityRule> rules) { availabilityRules = rules; }
    public List<String> getExcludedDates() { return excludedDates; }
    public void setExcludedDates(List<String> dates) { excludedDates = dates; }
    public List<AvailabilitySlot> getOneOffSlots() { return oneOffSlots; }
    public void setOneOffSlots(List<AvailabilitySlot> slots) { oneOffSlots = slots; }
    public String getWindowStart() { return windowStart; }
    public void setWindowStart(String date) { windowStart = date; }
    public String getWindowEnd() { return windowEnd; }
    public void setWindowEnd(String date) { windowEnd = date; }
    private List<AvailabilitySlot> availableSlots;
    public List<AvailabilitySlot> getAvailableSlots() { return availableSlots; }
    public void setAvailableSlots(List<AvailabilitySlot> slots) { availableSlots = slots; }
    private List<String> availableDates;
    private List<String> availableTimes;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }
    public List<String> getAvailableDates() { return availableDates; }
    public void setAvailableDates(List<String> availableDates) { this.availableDates = availableDates; }
    public List<String> getAvailableTimes() { return availableTimes; }
    public void setAvailableTimes(List<String> availableTimes) { this.availableTimes = availableTimes; }
}
