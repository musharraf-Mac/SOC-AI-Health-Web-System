package com.example.lab_rep.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class OcrService {

    public String extractTextFromPDF(byte[] pdfContent) {
        try (PDDocument document = Loader.loadPDF(pdfContent)) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);

            if (text == null || text.trim().isEmpty()) {
                throw new IllegalArgumentException("Scanned PDFs require OCR; upload a PDF containing selectable text.");
            }
            return text.trim();

        } catch (IOException e) {
            throw new IllegalArgumentException("The file is not a readable PDF.", e);
        }
    }
}