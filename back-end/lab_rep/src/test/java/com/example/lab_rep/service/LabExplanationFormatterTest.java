package com.example.lab_rep.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LabExplanationFormatterTest {
    private final LabExplanationFormatter formatter = new LabExplanationFormatter();
    @Test void arithmeticOverridesAnyModelInterpretation() throws Exception {
        String output = formatter.format(AiAnalysisServiceTest.EXTRACTION, AiAnalysisServiceTest.REPORT);
        assertTrue(output.contains("2 of the verified measurements"));
        assertTrue(output.contains("Below the printed reference range"));
        assertTrue(output.contains("Above the printed reference range"));
        assertFalse(output.contains("clinically normal"));
        assertFalse(output.contains("untrusted"));
    }
    @Test void inventedEvidenceCannotPass() {
        assertThrows(IllegalArgumentException.class, () -> formatter.format(AiAnalysisServiceTest.EXTRACTION, "Hemoglobin 14 g/dL 12 - 16"));
    }
    @Test void inventedResultCannotPass() {
        assertThrows(IllegalArgumentException.class, () -> formatter.format(
            AiAnalysisServiceTest.EXTRACTION.replace("\"80\"", "\"90\"").replace("\"37.5\"", "\"30\""), AiAnalysisServiceTest.REPORT));
    }
    @Test void rangeBoundariesAreInclusiveWithoutDeclaringWholeReportNormal() throws Exception {
        String extraction = """
          {"measurements":[{"name":"Hb","value":"12","unit":"g/dL","referenceRange":"12 - 16","source":"Hb 12 g/dL 12 - 16"}]}
          """;
        String result = formatter.format(extraction, "Hb 12 g/dL 12 - 16");
        assertTrue(result.contains("Within the printed reference range"));
        assertTrue(result.contains("does not establish that the entire report is normal"));
    }
}
