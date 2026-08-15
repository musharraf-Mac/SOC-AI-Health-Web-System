public interface PatientRepository extends MongoRepository<Patient, String> {
    Patient findByEmail(String email);
}

public interface PrescriptionRepository extends MongoRepository<Prescription, String> {
    List<Prescription> findByPatientEmail(String email);
}
