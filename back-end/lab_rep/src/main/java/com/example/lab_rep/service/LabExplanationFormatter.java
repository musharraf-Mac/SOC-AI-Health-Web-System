package com.example.lab_rep.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/** Patient text is rendered from evidence-checked measurements, not model clinical claims. */
final class LabExplanationFormatter {
    private static final String NUMBER = "[+-]?(?:\\d{1,3}(?:,\\d{3})+|\\d+)(?:\\.\\d+)?";
    private static final Pattern RANGE = Pattern.compile("^\\s*(" + NUMBER + ")\\s*[-–—]\\s*(" + NUMBER + ")\\s*$");
    private static final Pattern ANY_RANGE = Pattern.compile("(" + NUMBER + ")\\s*[-–—]\\s*(" + NUMBER + ")");
    private static final Pattern VALUE = Pattern.compile("^" + NUMBER + "$");
    private final ObjectMapper json = new ObjectMapper();

    String format(String content, String report) throws Exception {
        String raw = content.trim();
        if (raw.startsWith("```")) raw = raw.replaceFirst("^```(?:json)?\\s*", "").replaceFirst("\\s*```$", "");
        JsonNode rows = json.readTree(raw).path("measurements");
        if (!rows.isArray() || rows.size() == 0 || rows.size() > 300) throw new IllegalArgumentException("No structured measurements");
        String normalizedReport = normalize(report);
        List<String> findings = new ArrayList<>();
        int outside = 0;
        int skipped = 0;
        var seen = new java.util.HashSet<String>();
        for (JsonNode row : rows) {
            String name = row.path("name").asText("").trim();
            String value = row.path("value").asText("").trim();
            String unit = row.path("unit").asText("").trim();
            String range = row.path("referenceRange").asText("").trim();
            String source = normalize(row.path("source").asText(""));
            // The model must quote a contiguous fragment; no invented or repaired evidence.
            if (name.isEmpty() || name.length() > 120 || unit.length() > 60 || source.isEmpty() || source.length() > 600
                    || !normalizedReport.contains(source) || !source.contains(normalize(name))
                    || !containsNumber(source, value) || (!unit.isEmpty() && !source.contains(normalize(unit)))
                    || !source.contains(normalize(range)) || !VALUE.matcher(value).matches()) {
                skipped++;
                continue;
            }
            var bounds = RANGE.matcher(range);
            if (!bounds.matches()) { skipped++; continue; }
            // A range endpoint is not evidence of a measured result. Require the value outside the range.
            String sourceWithoutRange = source.replace(normalize(range), " ");
            var allRanges = ANY_RANGE.matcher(source);
            int rangeCount = 0;
            while (allRanges.find()) rangeCount++;
            if (rangeCount != 1 || !containsNumber(sourceWithoutRange, value)) { skipped++; continue; }
            BigDecimal measured = decimal(value), low = decimal(bounds.group(1)), high = decimal(bounds.group(2));
            if (low.compareTo(high) > 0) { skipped++; continue; }
            // Reject conflicts rather than allowing the model to choose a convenient reference interval.
            String identity = normalize(name);
            if (!seen.add(identity)) throw new IllegalArgumentException("Duplicate/ambiguous measurement");
            String flag;
            if (measured.compareTo(low) < 0) { flag = "Below the printed reference range"; outside++; }
            else if (measured.compareTo(high) > 0) { flag = "Above the printed reference range"; outside++; }
            else flag = "Within the printed reference range";
            findings.add("- " + name.replaceAll("[\\r\\n]", " ") + ": " + value + (unit.isBlank() ? "" : " " + unit.replaceAll("[\\r\\n]", " "))
                    + " (printed range: " + range + ") — " + flag + ".");
        }
        if (findings.isEmpty()) throw new IllegalArgumentException("No verifiable numeric reference ranges");
        String summary = outside == 0
                ? "None of the verified measurements listed below are outside their printed reference ranges. This does not establish that the entire report is normal."
                : outside + " of the verified measurements listed below " + (outside == 1 ? "is" : "are") + " outside the printed reference ranges.";
        return "Report explanation\n\n" + summary + "\n\nVerified measurements\n" + String.join("\n", findings)
                + "\n\nLimitations\nOnly measurements with a verifiable result and numeric interval from the extracted report are listed. "
                + (skipped > 0 ? skipped + " extracted entries could not be verified and were omitted. " : "")
                + "Missing, unclear, qualitative or age/sex-dependent ranges need review against the original PDF. Text extraction or model extraction may omit results."
                + "\n\nWhat this means\nOutside-range values alone do not diagnose a condition. Values within a range do not rule out illness. "
                + "Discuss these findings with your clinician, who can consider symptoms, medical history and the complete report. No treatment recommendation is made here.";
    }

    private boolean containsNumber(String text, String value) {
        return !value.isEmpty() && Pattern.compile("(?<![\\d.,])" + Pattern.quote(value) + "(?![\\d.,])").matcher(text).find();
    }
    private BigDecimal decimal(String text) { return new BigDecimal(text.replace(",", "")); }
    private String normalize(String text) { return text.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT); }
}
