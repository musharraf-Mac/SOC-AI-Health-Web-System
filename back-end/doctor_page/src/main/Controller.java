@RestController
@RequestMapping("/doctor")
public class DoctorController {

    @Autowired
    private PatientRepository patientRepo;

    @Autowired
    private PrescriptionRepository prescriptionRepo;

    @Autowired
    private PrescriptionService prescriptionService;

    // 1. Get all booked patients
    @GetMapping("/patients")
    public List<Patient> getAllPatients() {
        return patientRepo.findAll();
    }

    // 2. Get patient by email
    @GetMapping("/patients/{email}")
    public Patient getPatient(@PathVariable String email) {
        return patientRepo.findByEmail(email);
    }

    // 3. Save prescription + generate webpage + save URL
    @PostMapping("/prescriptions")
    public Prescription savePrescription(@RequestBody Prescription prescription) throws Exception {
        // Generate webpage + PDF + QR
        String url = prescriptionService.generateWebPage(prescription);
        String pdfPath = prescriptionService.generatePdf(prescription);
        String qrPath = prescriptionService.generateQr(prescription);

        prescription.setUrl(url);
        prescription.setPdfPath(pdfPath);
        prescription.setQrCode(qrPath);

        return prescriptionRepo.save(prescription);
    }

    // 4. Retrieve prescriptions by patient email
    @GetMapping("/prescriptions/{email}")
    public List<Prescription> getPrescriptions(@PathVariable String email) {
        return prescriptionRepo.findByPatientEmail(email);
    }
}

