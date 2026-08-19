package com.example.bmi.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.bmi.dto.BmiRequest;
import com.example.bmi.dto.BmiResponse;
import com.example.bmi.model.BmiRecord;
import com.example.bmi.service.BmiService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bmi")
// @CrossOrigin(origins = "*")
public class BmiController {

    private final BmiService bmiService;

    public BmiController(BmiService bmiService) {
        this.bmiService = bmiService;
    }

    // -----------------------------------------
    // GUEST CALCULATION
    // -----------------------------------------

    @PostMapping("/calculate")
    public BmiResponse calculateBMI(@Valid @RequestBody BmiRequest request) {
        return bmiService.calculateGuestBmi(request);
    }

    // -----------------------------------------
    // LOGGED-IN CALCULATION
    // -----------------------------------------

    @PostMapping("/calculate/save")
    public BmiResponse calculateAndSave(@Valid @RequestBody BmiRequest request) {
        String userId = "test-user-001";
        return bmiService.calculateAndSave(request, userId);
    }

    // -----------------------------------------
    // GET USER BMI HISTORY
    // -----------------------------------------

    @GetMapping("/history")
    public List<BmiRecord> getHistory() {
        String userId = "test-user-001";
        return bmiService.getHistory(userId);
    }
}