package com.example.bmi.service;

import com.example.bmi.dto.BmiRequest;
import com.example.bmi.dto.BmiResponse;
import com.example.bmi.model.BmiRecord;
import com.example.bmi.repository.BmiRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class BmiService {

    private final BmiRepository bmiRepository;

    public BmiService(BmiRepository bmiRepository) {
        this.bmiRepository = bmiRepository;
    }

    // -----------------------------------------
    // 1. GUEST BMI
    // -----------------------------------------

    public BmiResponse calculateGuestBmi(
            BmiRequest request
    ) {

        double bmi = calculateBmi(
                request.getHeight(),
                request.getWeight()
        );

        String category = getCategory(bmi);
        String message = getMessage(category);

        return new BmiResponse(
                bmi,
                category,
                message,
                false
        );
    }

    // -----------------------------------------
    // 2. LOGGED-IN BMI
    // -----------------------------------------

    public BmiResponse calculateAndSave(
            BmiRequest request,
            String userId
    ) {

        double bmi = calculateBmi(
                request.getHeight(),
                request.getWeight()
        );

        String category = getCategory(bmi);
        String message = getMessage(category);

        BmiRecord record = new BmiRecord();

        record.setUserId(userId);
        record.setHeight(request.getHeight());
        record.setWeight(request.getWeight());
        record.setAge(request.getAge());
        record.setGender(request.getGender());
        record.setBmi(bmi);
        record.setCategory(category);
        record.setCalculatedAt(LocalDateTime.now());

        bmiRepository.save(record);

        return new BmiResponse(
                bmi,
                category,
                message,
                true
        );
    }

    // -----------------------------------------
    // 3. GET BMI HISTORY
    // -----------------------------------------

    public List<BmiRecord> getHistory(String userId) {

        return bmiRepository
                .findByUserIdOrderByCalculatedAtDesc(userId);
    }

    // -----------------------------------------
    // BMI CALCULATION
    // -----------------------------------------

    private double calculateBmi(
            double heightCm,
            double weightKg
    ) {

        double heightMeters = heightCm / 100.0;

        double bmi = weightKg /
                (heightMeters * heightMeters);

        return Math.round(bmi * 100.0) / 100.0;
    }

    // -----------------------------------------
    // BMI CATEGORY
    // -----------------------------------------

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

    // -----------------------------------------
    // BMI MESSAGE
    // -----------------------------------------

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