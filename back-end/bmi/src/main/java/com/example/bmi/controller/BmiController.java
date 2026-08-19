package com.example.bmi.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.bmi.dto.BmiRequest;
import com.example.bmi.dto.BmiResponse;
import com.example.bmi.model.BmiRecord;
import com.example.bmi.security.JwtUtil;
import com.example.bmi.service.BmiService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/bmi")
public class BmiController {

    private final BmiService bmiService;
    private final JwtUtil jwtUtil;

    public BmiController(BmiService bmiService, JwtUtil jwtUtil) {
        this.bmiService = bmiService;
        this.jwtUtil = jwtUtil;
    }

    // -----------------------------------------
    // GUEST CALCULATION — no auth, nothing saved
    // -----------------------------------------

    @PostMapping("/calculate")
    public BmiResponse calculateBMI(@Valid @RequestBody BmiRequest request) {
        return bmiService.calculateGuestBmi(request);
    }

    // -----------------------------------------
    // LOGGED-IN CALCULATION — saved under real user ID
    // -----------------------------------------

    @PostMapping("/calculate/save")
    public ResponseEntity<?> calculateAndSave(
            @Valid @RequestBody BmiRequest request,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or missing token"));
        }

        BmiResponse response = bmiService.calculateAndSave(request, userId);
        return ResponseEntity.ok(response);
    }

    // -----------------------------------------
    // GET USER BMI HISTORY — only this user's records
    // -----------------------------------------

    @GetMapping("/history")
    public ResponseEntity<?> getHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or missing token"));
        }

        List<BmiRecord> history = bmiService.getHistory(userId);
        return ResponseEntity.ok(history);
    }

    // -----------------------------------------
    // DELETE ONE HISTORY ENTRY — only if it belongs to this user
    // -----------------------------------------

    @DeleteMapping("/history/{recordId}")
    public ResponseEntity<?> deleteHistoryEntry(
            @PathVariable String recordId,
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or missing token"));
        }

        boolean deleted = bmiService.deleteHistoryEntry(recordId, userId);
        if (!deleted) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "Record not found or doesn't belong to this user"
            ));
        }
        return ResponseEntity.ok(Map.of("message", "History entry deleted"));
    }

    // -----------------------------------------
    // DELETE ALL HISTORY FOR THIS USER
    // -----------------------------------------

    @DeleteMapping("/history")
    public ResponseEntity<?> deleteAllHistory(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        String userId = extractUserId(authHeader);
        if (userId == null) {
            return ResponseEntity.status(401).body(Map.of("error", "Invalid or missing token"));
        }

        bmiService.deleteAllHistory(userId);
        return ResponseEntity.ok(Map.of("message", "All history deleted"));
    }

    // -----------------------------------------
    // Helper — validates token, returns userId or null
    // -----------------------------------------
    private String extractUserId(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return null;
        }
        String token = authHeader.substring(7);
        if (!jwtUtil.validateToken(token)) {
            return null;
        }
        return jwtUtil.getUserIdFromToken(token);
    }
}