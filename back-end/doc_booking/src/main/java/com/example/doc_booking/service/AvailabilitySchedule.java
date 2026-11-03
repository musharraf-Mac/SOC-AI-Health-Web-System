package com.example.doc_booking.service;

import com.example.doc_booking.model.*;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Expands stored rules on read, so weekly recurrence needs no cron job or new collection. */
public class AvailabilitySchedule {
    private final Clock clock;
    public AvailabilitySchedule(Clock clock) { this.clock = clock; }
    public LocalDate today() { return LocalDate.now(clock); }
    public LocalDate lastDay() { return today().plusDays(13); }
    public boolean isFuture(AvailabilitySlot slot) {
        try { return slot != null && LocalDateTime.of(date(slot.date()), time(slot.time())).isAfter(LocalDateTime.now(clock)); }
        catch (RuntimeException e) { return false; }
    }
    private LocalDate date(String value) {
        if (value == null || !value.matches("\\d{4}-\\d{2}-\\d{2}")) throw bad("Use dates in YYYY-MM-DD format.");
        try { return LocalDate.parse(value); }
        catch (DateTimeParseException e) { throw bad("Choose a valid calendar date."); }
    }
    private LocalTime time(String value) {
        if (value == null || !value.matches("\\d{2}:\\d{2}")) throw bad("Use times in HH:mm format.");
        try { return LocalTime.parse(value); }
        catch (DateTimeParseException e) { throw bad("Choose a valid time."); }
    }
    public List<AvailabilityRule> validateRules(List<AvailabilityRule> rules) {
        if (rules == null || rules.size() > 50) throw bad("A maximum of 50 availability rules is allowed.");
        Set<String> ids = new HashSet<>();
        for (var rule : rules) {
            if (rule == null || rule.id() == null || rule.id().isBlank() || rule.id().length() > 80 || !ids.add(rule.id()))
                throw bad("Availability rules must have unique IDs.");
            date(rule.startDate());
            if (rule.daysOfWeek() == null || rule.daysOfWeek().isEmpty() || rule.daysOfWeek().size() > 7)
                throw bad("Choose at least one weekday.");
            for (String day : rule.daysOfWeek()) {
                try { DayOfWeek.valueOf(day); }
                catch (RuntimeException e) { throw bad("Choose valid weekdays."); }
            }
            var start = time(rule.startTime()); var end = time(rule.endTime());
            long minutes = Duration.between(start, end).toMinutes();
            if (rule.slotMinutes() < 5 || rule.slotMinutes() > 240 || minutes < rule.slotMinutes()
                    || minutes % rule.slotMinutes() != 0)
                throw bad("The end time must be after the start time, and the range must contain whole appointments of 5–240 minutes.");
        }
        // Reject overlapping rules even beyond today's preview, not just duplicate start times.
        for (int i = 0; i < rules.size(); i++) for (int j = i + 1; j < rules.size(); j++) {
            var a = rules.get(i); var b = rules.get(j);
            if (!time(a.startTime()).isBefore(time(b.endTime())) || !time(b.startTime()).isBefore(time(a.endTime()))) continue;
            LocalDate first = date(a.startDate()).isAfter(date(b.startDate())) ? date(a.startDate()) : date(b.startDate());
            for (int day = 0; day < 7; day++) {
                LocalDate candidate = first.plusDays(day);
                if (applies(a, candidate) && applies(b, candidate)) throw bad("Time ranges overlap on a selected day. Edit or remove the existing range first.");
            }
        }
        return List.copyOf(rules);
    }
    public List<String> validateExclusions(List<String> dates) {
        if (dates == null || dates.size() > 366) throw bad("A maximum of 366 excluded dates is allowed.");
        return dates.stream().map(this::date).filter(d -> !d.isBefore(today())).distinct().sorted().map(LocalDate::toString).toList();
    }
    private boolean applies(AvailabilityRule rule, LocalDate day) {
        LocalDate first = date(rule.startDate());
        return !day.isBefore(first) && (rule.repeatWeekly() || day.isBefore(first.plusDays(7)))
            && rule.daysOfWeek().contains(day.getDayOfWeek().name());
    }
    public List<AvailabilitySlot> expand(List<AvailabilityRule> rules, List<String> excluded, List<AvailabilitySlot> manual) {
        Set<AvailabilitySlot> result = new HashSet<>();
        for (var slot : manual) if (isFuture(slot) && !date(slot.date()).isAfter(lastDay()) && !excluded.contains(slot.date())) result.add(slot);
        for (LocalDate day = today(); !day.isAfter(lastDay()); day = day.plusDays(1)) {
            if (excluded.contains(day.toString())) continue;
            for (var rule : rules) if (applies(rule, day)) {
                int start = time(rule.startTime()).toSecondOfDay() / 60;
                int end = time(rule.endTime()).toSecondOfDay() / 60;
                for (int minute = start; minute + rule.slotMinutes() <= end; minute += rule.slotMinutes()) {
                    var slot = new AvailabilitySlot(day.toString(), String.format(Locale.ROOT, "%02d:%02d", minute / 60, minute % 60));
                    if (isFuture(slot)) result.add(slot);
                }
            }
        }
        return result.stream().sorted(Comparator.comparing(AvailabilitySlot::date).thenComparing(AvailabilitySlot::time)).toList();
    }
    public int duration(Doctor doctor, AvailabilitySlot slot) {
        for (var rule : doctor.getAvailabilityRules()) {
            if (applies(rule, date(slot.date())) && !time(slot.time()).isBefore(time(rule.startTime())) && time(slot.time()).isBefore(time(rule.endTime()))) return rule.slotMinutes();
        }
        return 30; // Legacy individual slots did not carry a duration.
    }
    private ResponseStatusException bad(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
