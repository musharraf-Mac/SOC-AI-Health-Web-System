package com.example.lab_rep.dto;

/** The saved explanation for one report; storage object paths remain server-side. */
public record ReportExplanation(String reportId, String fileName, String status, String explanation,
                                String provider, String model, String generatedAt, String error) {}
