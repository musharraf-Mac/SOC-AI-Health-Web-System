package com.example.bmi.controller;

import com.example.bmi.dto.BmiRequest;
import com.example.bmi.dto.BmiResponse;
import com.example.bmi.model.BmiRecord;
import com.example.bmi.service.BmiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/bmi")
@CrossOrigin(origins = "http://localhost:3000")
public class BmiController {

    private final BmiService bmiService;

    public BmiController(BmiService bmiService) {
        this.bmiService = bmiService;
    }

    // -----------------------------------------
    // GUEST CALCULATION
    // -----------------------------------------

    @PostMapping("/calculate")
    public BmiResponse calculateBMI(
            @Valid @RequestBody BmiRequest request
    ) {

        return bmiService.calculateGuestBmi(request);
    }

    // -----------------------------------------
    // LOGGED-IN CALCULATION
    // -----------------------------------------

    @PostMapping("/history")
    public BmiResponse calculateAndSave(
            @Valid @RequestBody BmiRequest request
    ) {

        // Temporary user ID.
        // We will replace this with JWT later.
        String userId = "test-user-001";

        return bmiService.calculateAndSave(
                request,
                userId
        );
    }

    // -----------------------------------------
    // GET USER BMI HISTORY
    // -----------------------------------------

    @GetMapping("/history")
    public List<BmiRecord> getHistory() {

        // Temporary user ID.
        // We will replace this with JWT later.
        String userId = "test-user-001";

        return bmiService.getHistory(userId);
    }
}