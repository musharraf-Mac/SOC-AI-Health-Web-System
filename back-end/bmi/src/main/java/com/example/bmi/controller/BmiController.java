package com.example.bmi.controller;

import com.example.bmi.dto.BmiRequest;
import com.example.bmi.dto.BmiResponse;
import com.example.bmi.service.BmiService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/bmi")
@CrossOrigin(origins = "http://localhost:3000")
public class BmiController {

    private final BmiService bmiService;

    public BmiController(BmiService bmiService) {
        this.bmiService = bmiService;
    }

    @PostMapping("/calculate")
    public BmiResponse calculateBMI(
            @Valid @RequestBody BmiRequest request
    ) {

        return bmiService.calculateGuestBmi(request);
    }
}
