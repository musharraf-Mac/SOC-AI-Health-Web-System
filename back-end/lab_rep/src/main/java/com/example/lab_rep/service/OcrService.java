package com.example.lab_rep.service;

import org.springframework.stereotype.Service;

@Service
public class OCRService {

    /**
     * Placeholder - returns sample text for testing
     * Will be implemented with PDFBox in Step 2
     */
    public String extractTextFromPDF(String pdfPath) {
        // For now, return sample text for testing
        return """
            Sample Lab Report Data:
            Patient: John Doe
            Age: 35
            Test: Complete Blood Count
            
            Results:
            Hemoglobin: 14.2 g/dL (Normal: 13.0-17.0)
            White Blood Cells: 7.5 x10^3/uL (Normal: 4.5-11.0)
            Platelets: 250 x10^3/uL (Normal: 150-400)
            Glucose: 95 mg/dL (Normal: 70-100)
            Cholesterol: 190 mg/dL (Normal: <200)
            
            All values are within normal range.
            """;
    }
}