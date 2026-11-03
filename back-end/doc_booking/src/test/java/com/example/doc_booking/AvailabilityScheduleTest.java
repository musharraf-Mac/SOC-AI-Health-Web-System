package com.example.doc_booking;

import com.example.doc_booking.model.*;
import com.example.doc_booking.service.AvailabilitySchedule;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

class AvailabilityScheduleTest {
    AvailabilitySchedule at(String instant) { return new AvailabilitySchedule(Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Colombo"))); }
    AvailabilitySchedule schedule = at("2026-11-02T00:00:00Z"); // Monday 05:30 in Colombo
    AvailabilityRule rule(boolean repeat) { return new AvailabilityRule("r1", "2026-11-02", List.of("MONDAY", "WEDNESDAY"), "09:00", "10:00", 30, repeat); }
    @Test void recurringDaysHaveOnlyWholeAppointmentsWithinFourteenDays() {
        var slots = schedule.expand(schedule.validateRules(List.of(rule(true))), List.of(), List.of());
        assertEquals(List.of("2026-11-02", "2026-11-04", "2026-11-09", "2026-11-11"), slots.stream().map(AvailabilitySlot::date).distinct().toList());
        assertEquals(8, slots.size()); assertTrue(slots.stream().allMatch(s -> Set.of("09:00", "09:30").contains(s.time())));
    }
    @Test void recurrenceRollsForwardWithoutWritingCopies() {
        var later = at("2026-12-07T00:00:00Z");
        var slots = later.expand(List.of(rule(true)), List.of(), List.of());
        assertEquals(8, slots.size()); assertEquals("2026-12-07", slots.getFirst().date());
    }
    @Test void oneTimeRunsSelectedWeekdaysOnlyInItsFirstSevenDays() {
        assertEquals(4, schedule.expand(List.of(rule(false)), List.of(), List.of()).size());
        assertTrue(at("2026-11-09T00:00:00Z").expand(List.of(rule(false)), List.of(), List.of()).isEmpty());
    }
    @Test void excludingOneDateDoesNotRemoveFollowingWeekAndCanBeRestored() {
        var slots = schedule.expand(List.of(rule(true)), List.of("2026-11-02"), List.of());
        assertTrue(slots.stream().noneMatch(s -> s.date().equals("2026-11-02")));
        assertTrue(slots.stream().anyMatch(s -> s.date().equals("2026-11-09")));
        assertEquals(8, schedule.expand(List.of(rule(true)), List.of(), List.of()).size());
    }
    @Test void colomboDayAndPastTimeAreCorrectAcrossUtcMidnight() {
        var eveningUtc = at("2026-11-01T20:00:00Z");
        assertEquals(LocalDate.parse("2026-11-02"), eveningUtc.today());
        assertFalse(eveningUtc.isFuture(new AvailabilitySlot("2026-11-02", "01:00")));
        assertTrue(eveningUtc.isFuture(new AvailabilitySlot("2026-11-02", "02:00")));
    }
    @Test void rejectsBadRangesAndOverlapBeyondCurrentPreview() {
        for (var rule : List.of(
            new AvailabilityRule("bad", "2026-11-02", List.of("MONDAY"), "10:00", "09:00", 30, true),
            new AvailabilityRule("bad", "2026-11-02", List.of("MONDAY"), "09:00", "10:10", 30, true),
            new AvailabilityRule("bad", "2026-11-02", List.of("MONDAY"), "09:00", "10:00", 0, true),
            new AvailabilityRule("bad", "2026-02-30", List.of("MONDAY"), "09:00", "10:00", 30, true),
            new AvailabilityRule("bad", "2026-11-02", List.of("FUNDAY"), "09:00", "10:00", 30, true)))
            assertThrows(ResponseStatusException.class, () -> schedule.validateRules(List.of(rule)));
        var overlap = new AvailabilityRule("r2", "2027-01-01", List.of("MONDAY"), "09:15", "10:15", 30, true);
        assertThrows(ResponseStatusException.class, () -> schedule.validateRules(List.of(rule(true), overlap)));
    }
    @Test void legacySlotsRespectWindowAndExclusions() {
        var manual = List.of(new AvailabilitySlot("2026-11-02", "09:00"), new AvailabilitySlot("2026-11-16", "09:00"));
        assertEquals(1, schedule.expand(List.of(), List.of(), manual).size());
        assertTrue(schedule.expand(List.of(), List.of("2026-11-02"), manual).isEmpty());
    }
}
