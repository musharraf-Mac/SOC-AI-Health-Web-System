package com.example.doc_booking.dto;
import com.example.doc_booking.model.AvailabilitySlot;
import com.example.doc_booking.model.AvailabilityRule;
import java.util.List;
public record AvailabilityRequest(String specialization, List<AvailabilitySlot> availableSlots,
        List<AvailabilityRule> availabilityRules, List<String> excludedDates) {
    public AvailabilityRequest(String specialization, List<AvailabilitySlot> availableSlots) {
        this(specialization, availableSlots, null, null);
    }
}
