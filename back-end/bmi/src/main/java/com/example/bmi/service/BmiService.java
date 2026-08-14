package com.example.bmi.service;

import com.example.bmi.dto.BmiRequest;
import com.example.bmi.dto.BmiResponse;
import org.springframework.stereotype.Service;

@Service
public class BmiService {

    public BmiResponse calculateGuestBmi(BmiRequest request) {

        double heightInMeters = request.getHeight() / 100;

        double bmi = request.getWeight()
                / (heightInMeters * heightInMeters);

        bmi = Math.round(bmi * 100.0) / 100.0;

        String category = getCategory(bmi);

        String message = getMessage(category);

        return new BmiResponse(
                bmi,
                category,
                message,
                false
        );
    }

    private String getCategory(double bmi) {

        if (bmi < 18.5) {
            return "UNDERWEIGHT";
        }

        if (bmi < 25) {
            return "NORMAL WEIGHT";
        }

        if (bmi < 30) {
            return "OVERWEIGHT";
        }

        return "OBESITY";
    }

    private String getMessage(String category) {

        return switch (category) {

            case "UNDERWEIGHT" ->
                    "Your BMI indicates that you are underweight.";

            case "NORMAL WEIGHT" ->
                    "Your BMI is in the healthy range.";

            case "OVERWEIGHT" ->
                    "Your BMI indicates that you are overweight.";

            case "OBESITY" ->
                    "Your BMI is in the obesity range.";

            default ->
                    "BMI calculation completed.";
        };
    }
}