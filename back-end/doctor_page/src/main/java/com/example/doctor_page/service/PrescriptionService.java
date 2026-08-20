package com.example.doctor_page.service;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.nio.file.FileSystems;
import java.nio.file.Path;

import org.springframework.stereotype.Service;

import com.example.doctor_page.model.Prescription;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.itextpdf.text.Document;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

@Service
public class PrescriptionService {

    // ==========================================
    // 1. Generate PDF
    // ==========================================
    public String generatePdf(Prescription prescription) throws Exception {

        // Create folder if it doesn't exist
        File folder = new File("prescriptions");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String filePath =
                "prescriptions/" + prescription.getPatientEmail() + ".pdf";

        Document doc = new Document();

        PdfWriter.getInstance(
                doc,
                new FileOutputStream(filePath)
        );

        doc.open();

        doc.add(new Paragraph("DOCTOR PRESCRIPTION"));
        doc.add(new Paragraph("--------------------------------"));
        doc.add(new Paragraph(
                "Doctor: " + prescription.getDoctorName()
        ));

        doc.add(new Paragraph(
                "Doctor Email: " + prescription.getDoctorEmail()
        ));

        doc.add(new Paragraph(
                "Patient: " + prescription.getPatientName()
        ));

        doc.add(new Paragraph(
                "Patient Email: " + prescription.getPatientEmail()
        ));

        doc.add(new Paragraph(
                "Patient Age: " + prescription.getPatientAge()
        ));

        doc.add(new Paragraph(
                "Disease: " + prescription.getDisease()
        ));

        doc.add(new Paragraph(
                "Prescription: " +
                prescription.getPrescriptionText()
        ));

        doc.close();

        return filePath;
    }


    // ==========================================
    // 2. Generate QR Code
    // ==========================================
    public String generateQr(Prescription prescription)
            throws Exception {

        // Create folder if it doesn't exist
        File folder = new File("qr");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String qrPath =
                "qr/" + prescription.getPatientEmail() + ".png";

        QRCodeWriter qrCodeWriter = new QRCodeWriter();

        BitMatrix bitMatrix =
                qrCodeWriter.encode(
                        prescription.getUrl(),
                        BarcodeFormat.QR_CODE,
                        200,
                        200
                );

        Path path =
                FileSystems.getDefault().getPath(qrPath);

        MatrixToImageWriter.writeToPath(
                bitMatrix,
                "PNG",
                path
        );

        return qrPath;
    }


    // ==========================================
    // 3. Generate Webpage
    // ==========================================
    public String generateWebPage(Prescription prescription)
            throws Exception {

        // Create folder if it doesn't exist
        File folder = new File("webpages");

        if (!folder.exists()) {
            folder.mkdirs();
        }

        String fileName =
                prescription.getPatientEmail() + ".html";

        String filePath =
                "webpages/" + fileName;

        try (FileWriter writer =
                     new FileWriter(filePath)) {

            writer.write("<html>");
            writer.write("<head>");
            writer.write("<title>Prescription</title>");
            writer.write("</head>");

            writer.write("<body>");

            writer.write("<h1>Prescription</h1>");

            writer.write(
                    "<p><b>Doctor:</b> "
                    + prescription.getDoctorName()
                    + "</p>"
            );

            writer.write(
                    "<p><b>Doctor Email:</b> "
                    + prescription.getDoctorEmail()
                    + "</p>"
            );

            writer.write(
                    "<p><b>Patient:</b> "
                    + prescription.getPatientName()
                    + "</p>"
            );

            writer.write(
                    "<p><b>Patient Email:</b> "
                    + prescription.getPatientEmail()
                    + "</p>"
            );

            writer.write(
                    "<p><b>Age:</b> "
                    + prescription.getPatientAge()
                    + "</p>"
            );

            writer.write(
                    "<p><b>Disease:</b> "
                    + prescription.getDisease()
                    + "</p>"
            );

            writer.write(
                    "<p><b>Prescription:</b> "
                    + prescription.getPrescriptionText()
                    + "</p>"
            );

            writer.write("</body>");
            writer.write("</html>");
        }

        return "http://localhost:8086/doctor/webpage/"
                + fileName;
    }
}