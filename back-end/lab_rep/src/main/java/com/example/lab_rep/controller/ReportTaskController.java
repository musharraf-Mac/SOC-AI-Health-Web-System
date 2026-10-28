package com.example.lab_rep.controller;

import com.example.lab_rep.service.LabReportService;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ReportTaskController {
    private final LabReportService reports;
    public ReportTaskController(LabReportService reports) { this.reports = reports; }

    // Not routed by the public API gateway. ApiKeyFilter checks the task's private API key.
    @PostMapping("/internal/reports/{id}/process")
    public void process(@PathVariable String id) throws java.io.IOException {
        var report = reports.processQueued(id);
        if ("FAILED".equals(report.getStatus())) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Analysis unavailable; retry task");
    }
}
