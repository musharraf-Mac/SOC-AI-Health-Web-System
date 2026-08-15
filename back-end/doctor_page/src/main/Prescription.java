@Document(collection = "prescription")
public class Prescription {
    @Id
    private String id;
    private String doctorName;
    private String doctorEmail;
    private String patientName;
    private String patientEmail;
    private int patientAge;
    private String disease;
    private String prescriptionText;
    private String url; // webpage URL
    private String qrCode; // QR path or Base64
    private String pdfPath;
    // getters & setters
}

    
