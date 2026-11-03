package com.example.doc_booking.model;

import java.util.List;

/** For one-time rules, selected weekdays occur once in the seven days from startDate. */
public record AvailabilityRule(String id, String startDate, List<String> daysOfWeek,
        String startTime, String endTime, int slotMinutes, boolean repeatWeekly) {}
