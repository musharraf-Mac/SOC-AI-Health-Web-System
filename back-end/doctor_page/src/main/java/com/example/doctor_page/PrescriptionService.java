package com.example.doctor_page;

import org.springframework.stereotype.Service;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;

@Service
public class PrescriptionService {

    // Generate PDF
    public String generatePdf(Prescription prescription) throws Exception {
        String filePath = "prescriptions/" + prescription.getPatientEmail() + ".pdf";
        Document doc = new Document();
        PdfWriter.getInstance(doc, new FileOutputStream(filePath));
        doc.open();
        doc.add(new Paragraph("Doctor: " + prescription.getDoctorName()));
        doc.add(new Paragraph("Patient: " + prescription.getPatientName()));
        doc.add(new Paragraph("Disease: " + prescription.getDisease()));
        doc.add(new Paragraph("Prescription: " + prescription.getPrescriptionText()));
        doc.close();
        return filePath;
    }

    // Generate QR
    public String generateQr(Prescription prescription) throws Exception {
        String qrPath = "qr/" + prescription.getPatientEmail() + ".png";
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(prescription.getUrl(),
                BarcodeFormat.QR_CODE, 200, 200);
        Path path = FileSystems.getDefault().getPath(qrPath);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);
        return qrPath;
    }

    // Generate webpage (GET endpoint)
    public String generateWebPage(Prescription prescription) throws Exception {
        String fileName = prescription.getPatientEmail() + ".html";
        String filePath = "webpages/" + fileName;
        try (FileWriter writer = new FileWriter(filePath)) {
            writer.write("<html><body>");
            writer.write("<h1>Prescription</h1>");
            writer.write("<p>Doctor: " + prescription.getDoctorName() + "</p>");
            writer.write("<p>Patient: " + prescription.getPatientName() + "</p>");
            writer.write("<p>Disease: " + prescription.getDisease() + "</p>");
            writer.write("<p>Prescription: " + prescription.getPrescriptionText() + "</p>");
            writer.write("</body></html>");
        }
        return "http://localhost:8080/doctor/webpage/" + fileName;
    }
}
