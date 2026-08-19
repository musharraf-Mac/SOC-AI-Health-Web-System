"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";

// ============ Types ============
interface DoctorInfo {
  fullName: string;
  email: string;
  role: string;
}

interface Patient {
  id: string;
  patientName: string;
  patientEmail: string;
  doctorName: string;
  doctorEmail: string;
  bookingDate: string;
  bookingTime: string;
  tokenNumber: string;
  status: string;
  createdAt: string;
}

interface Prescription {
  id: string;
  doctorName: string;
  doctorEmail: string;
  patientName: string;
  patientEmail: string;
  patientAge: number;
  disease: string;
  prescriptionText: string;
  url: string;
  qrCode: string;
  pdfPath: string;
}

export default function DoctorDashboard() {
  const router = useRouter();
  const [doctor, setDoctor] = useState<DoctorInfo | null>(null);
  const [patients, setPatients] = useState<Patient[]>([]);
  const [selectedPatient, setSelectedPatient] = useState<Patient | null>(null);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [prescriptionForm, setPrescriptionForm] = useState({
    patientName: "",
    patientEmail: "",
    patientAge: "",
    disease: "",
    prescriptionText: "",
  });
  const [submitting, setSubmitting] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");
  const [errorMsg, setErrorMsg] = useState("");
  const [loading, setLoading] = useState(true);

  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;

  // ============ Fetch Doctor Info ============
  useEffect(() => {
    const role = localStorage.getItem("role");
    if (!token || role !== "DOCTOR") {
      router.push("/login");
      return;
    }

    apiFetch<DoctorInfo>("/api/auth/me", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(setDoctor)
      .catch(() => {
        localStorage.clear();
        router.push("/login");
      });

    // Fetch patients (booked patients)
    fetchPatients();
  }, [router]);

  // ============ Fetch Patients ============
  const fetchPatients = async () => {
    try {
      setLoading(true);
      const data = await apiFetch<Patient[]>("/api/doctor/patients");
      setPatients(data);
    } catch (error) {
      console.error("Error fetching patients:", error);
      setErrorMsg("Failed to load patients.");
    } finally {
      setLoading(false);
    }
  };

  // ============ Fetch Prescriptions for Selected Patient ============
  const fetchPrescriptions = async (email: string) => {
    try {
      const data = await apiFetch<Prescription[]>(`/api/doctor/prescriptions/${email}`);
      setPrescriptions(data);
    } catch (error) {
      console.error("Error fetching prescriptions:", error);
    }
  };

  // ============ Select Patient ============
  const handleSelectPatient = (patient: Patient) => {
    setSelectedPatient(patient);
    setPrescriptionForm({
      patientName: patient.patientName,
      patientEmail: patient.patientEmail,
      patientAge: "",
      disease: "",
      prescriptionText: "",
    });
    fetchPrescriptions(patient.patientEmail);
    setSuccessMsg("");
    setErrorMsg("");
  };

  // ============ Submit Prescription ============
  const submitPrescription = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedPatient) return;

    setSubmitting(true);
    setSuccessMsg("");
    setErrorMsg("");

    try {
      const prescriptionData = {
        doctorName: doctor?.fullName || "Doctor",
        doctorEmail: doctor?.email || "",
        patientName: prescriptionForm.patientName,
        patientEmail: prescriptionForm.patientEmail,
        patientAge: parseInt(prescriptionForm.patientAge) || 0,
        disease: prescriptionForm.disease,
        prescriptionText: prescriptionForm.prescriptionText,
      };

      const result = await apiFetch<Prescription>("/api/doctor/prescriptions", {
        method: "POST",
        body: JSON.stringify(prescriptionData),
      });

      setSuccessMsg("✅ Prescription saved successfully!");
      setPrescriptionForm({
        patientName: selectedPatient.patientName,
        patientEmail: selectedPatient.patientEmail,
        patientAge: "",
        disease: "",
        prescriptionText: "",
      });
      // Refresh prescriptions list
      fetchPrescriptions(selectedPatient.patientEmail);
    } catch (error) {
      console.error("Error saving prescription:", error);
      setErrorMsg("❌ Failed to save prescription. Please try again.");
    } finally {
      setSubmitting(false);
    }
  };

  // ============ Logout Handler ============
  const handleLogout = async () => {
    try {
      await apiFetch("/api/auth/logout", {
        method: "POST",
      });
    } catch (err) {
      console.error("Logout error:", err);
    } finally {
      localStorage.removeItem("token");
      localStorage.removeItem("role");
      router.push("/login");
    }
  };

  // ============ Profile Handler ============
  const goToProfile = () => {
    router.push("../../profile_page");
  };

  // ============ Loading State ============
  if (!doctor) return <div style={{ padding: 40, textAlign: "center" }}>Loading…</div>;

  return (
    <div style={page}>
      <div style={wrap}>
        {/* ===== Header ===== */}
        <div style={headerFlex}>
          <div>
            <p style={eyebrow}>Doctor Dashboard</p>
            <h1 style={heading}>Welcome, Dr. {doctor.fullName}</h1>
            <p style={subtext}>{doctor.email}</p>
          </div>
          <div style={headerButtons}>
            {/* Profile Button */}
            <button
              onClick={goToProfile}
              style={profileBtn}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = "#F6F3EC";
                e.currentTarget.style.borderColor = "#0B3D3A";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = "#FFF";
                e.currentTarget.style.borderColor = "#DCD6C8";
              }}
              title="Profile"
            >
              👤
            </button>

            {/* Logout Button */}
            <button
              onClick={handleLogout}
              style={logoutBtn}
              onMouseEnter={(e) => {
                e.currentTarget.style.background = "#E67A4A";
              }}
              onMouseLeave={(e) => {
                e.currentTarget.style.background = "#FF8A5B";
              }}
            >
              <span>🚪</span> Logout
            </button>
          </div>
        </div>

        {/* ===== Main Content ===== */}
        <div style={grid}>
          {/* ===== Left: Patient List ===== */}
          <div style={card}>
            <h2 style={cardTitle}>Today&apos;s Patients</h2>
            {loading ? (
              <p style={{ color: "#8A8577", fontSize: 14 }}>Loading patients...</p>
            ) : patients.length === 0 ? (
              <p style={{ color: "#8A8577", fontSize: 14 }}>No patients yet.</p>
            ) : (
              patients.map((p) => (
                <div
                  key={p.id}
                  onClick={() => handleSelectPatient(p)}
                  style={{
                    ...queueItem,
                    borderColor: selectedPatient?.id === p.id ? "#0B3D3A" : "#DCD6C8",
                    background: selectedPatient?.id === p.id ? "#F6F3EC" : "#FFF",
                  }}
                >
                  <div>
                    <p style={{ margin: 0, fontWeight: 600, color: "#1B1B18" }}>
                      Token #{p.tokenNumber} — {p.patientName}
                    </p>
                    <p style={{ margin: "2px 0 0", fontSize: 13, color: "#8A8577" }}>
                      {p.bookingDate} at {p.bookingTime}
                    </p>
                    <p style={{ margin: "2px 0 0", fontSize: 12, color: "#aaa" }}>
                      {p.patientEmail}
                    </p>
                  </div>
                  <span style={statusBadge(p.status)}>{p.status}</span>
                </div>
              ))
            )}
          </div>

          {/* ===== Right: Prescription Panel ===== */}
          <div style={card}>
            <h2 style={cardTitle}>Write Prescription</h2>
            {!selectedPatient ? (
              <p style={{ color: "#8A8577", fontSize: 14 }}>
                Select a patient from the list to write a prescription.
              </p>
            ) : (
              <form onSubmit={submitPrescription}>
                <p style={{ fontSize: 14, marginBottom: 12 }}>
                  For <strong>{selectedPatient.patientName}</strong> (Token #{selectedPatient.tokenNumber})
                </p>

                <div style={formRow}>
                  <label style={label}>Patient Name</label>
                  <input
                    type="text"
                    value={prescriptionForm.patientName}
                    onChange={(e) => setPrescriptionForm({ ...prescriptionForm, patientName: e.target.value })}
                    style={input}
                    required
                  />
                </div>

                <div style={formRow}>
                  <label style={label}>Patient Email</label>
                  <input
                    type="email"
                    value={prescriptionForm.patientEmail}
                    onChange={(e) => setPrescriptionForm({ ...prescriptionForm, patientEmail: e.target.value })}
                    style={input}
                    required
                  />
                </div>

                <div style={formRow}>
                  <label style={label}>Patient Age</label>
                  <input
                    type="number"
                    value={prescriptionForm.patientAge}
                    onChange={(e) => setPrescriptionForm({ ...prescriptionForm, patientAge: e.target.value })}
                    style={input}
                    placeholder="e.g., 30"
                  />
                </div>

                <div style={formRow}>
                  <label style={label}>Disease / Condition</label>
                  <input
                    type="text"
                    value={prescriptionForm.disease}
                    onChange={(e) => setPrescriptionForm({ ...prescriptionForm, disease: e.target.value })}
                    style={input}
                    placeholder="e.g., Hypertension"
                  />
                </div>

                <div style={formRow}>
                  <label style={label}>Prescription Details</label>
                  <textarea
                    value={prescriptionForm.prescriptionText}
                    onChange={(e) => setPrescriptionForm({ ...prescriptionForm, prescriptionText: e.target.value })}
                    placeholder="Medication, dosage, frequency, duration, instructions…"
                    rows={6}
                    style={textarea}
                    required
                  />
                </div>

                {successMsg && <p style={successStyle}>{successMsg}</p>}
                {errorMsg && <p style={errorStyle}>{errorMsg}</p>}

                <button
                  type="submit"
                  disabled={submitting}
                  style={submitBtn}
                >
                  {submitting ? "Saving…" : "💊 Save & Send to Patient"}
                </button>
              </form>
            )}

            {/* ===== Previous Prescriptions ===== */}
            {selectedPatient && prescriptions.length > 0 && (
              <div style={{ marginTop: 24, borderTop: "1px solid #DCD6C8", paddingTop: 16 }}>
                <h3 style={{ fontSize: 15, color: "#0B3D3A", marginBottom: 12 }}>
                  Previous Prescriptions
                </h3>
                {prescriptions.map((p) => (
                  <div key={p.id} style={{ ...queueItem, borderColor: "#E8E3D9", padding: "10px 12px" }}>
                    <div>
                      <p style={{ margin: 0, fontWeight: 500, fontSize: 13, color: "#1B1B18" }}>
                        {p.disease}
                      </p>
                      <p style={{ margin: "2px 0 0", fontSize: 12, color: "#8A8577" }}>
                        {p.prescriptionText.substring(0, 60)}...
                      </p>
                    </div>
                    {p.url && (
                      <a href={p.url} target="_blank" rel="noopener noreferrer" style={viewLink}>
                        View
                      </a>
                    )}
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ============ Helper Functions ============
function statusBadge(status: string): React.CSSProperties {
  const colors: Record<string, string> = {
    BOOKED: "#8A8577",
    CONFIRMED: "#0B3D3A",
    ARRIVED: "#FF8A5B",
    COMPLETED: "#1E7E34",
    CANCELLED: "#C0392B",
  };
  return {
    fontSize: 10,
    fontWeight: 700,
    letterSpacing: "0.05em",
    textTransform: "uppercase",
    color: "#FFF",
    background: colors[status] || "#8A8577",
    padding: "4px 10px",
    borderRadius: 4,
  };
}

// ============ Styles ============
const page: React.CSSProperties = {
  minHeight: "100vh",
  background: "#F6F3EC",
  padding: "40px 32px",
  fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
};

const wrap: React.CSSProperties = { maxWidth: 960, margin: "0 auto" };

const headerFlex: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  marginBottom: 24,
};

const headerButtons: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: 12,
};

const profileBtn: React.CSSProperties = {
  width: 44,
  height: 44,
  borderRadius: "50%",
  background: "#FFF",
  border: "2px solid #DCD6C8",
  cursor: "pointer",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  fontSize: 20,
  transition: "all 0.2s",
  padding: 0,
};

const logoutBtn: React.CSSProperties = {
  padding: "10px 20px",
  borderRadius: 8,
  background: "#FF8A5B",
  color: "#FFF",
  border: "none",
  cursor: "pointer",
  fontSize: 14,
  fontWeight: 600,
  display: "flex",
  alignItems: "center",
  gap: 8,
  transition: "background 0.2s",
};

const eyebrow: React.CSSProperties = {
  fontSize: 12,
  letterSpacing: "0.1em",
  textTransform: "uppercase",
  color: "#FF8A5B",
  fontWeight: 700,
  margin: 0,
};

const heading: React.CSSProperties = {
  fontFamily: "'Fraunces', Georgia, serif",
  fontSize: 30,
  color: "#0B3D3A",
  margin: "8px 0 4px",
};

const subtext: React.CSSProperties = { color: "#8A8577", fontSize: 14, margin: 0 };

const grid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "1fr 1.2fr",
  gap: 24,
};

const card: React.CSSProperties = {
  background: "#FFF",
  border: "1px solid #DCD6C8",
  borderRadius: 10,
  padding: 22,
};

const cardTitle: React.CSSProperties = {
  fontFamily: "'Fraunces', Georgia, serif",
  fontSize: 18,
  color: "#0B3D3A",
  margin: "0 0 16px",
};

const queueItem: React.CSSProperties = {
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  padding: "12px 14px",
  border: "1px solid #DCD6C8",
  borderRadius: 8,
  marginBottom: 10,
  cursor: "pointer",
  transition: "all 0.2s",
};

const formRow: React.CSSProperties = {
  marginBottom: 12,
};

const label: React.CSSProperties = {
  display: "block",
  fontSize: 13,
  fontWeight: 500,
  color: "#1B1B18",
  marginBottom: 4,
};

const input: React.CSSProperties = {
  width: "100%",
  padding: "10px 12px",
  fontSize: 14,
  border: "1px solid #DCD6C8",
  borderRadius: 6,
  background: "#FEFDFB",
  fontFamily: "inherit",
};

const textarea: React.CSSProperties = {
  width: "100%",
  padding: "10px 12px",
  fontSize: 14,
  border: "1px solid #DCD6C8",
  borderRadius: 6,
  resize: "vertical",
  fontFamily: "inherit",
  background: "#FEFDFB",
};

const submitBtn: React.CSSProperties = {
  width: "100%",
  padding: "12px 20px",
  fontSize: 14.5,
  fontWeight: 600,
  color: "#F6F3EC",
  background: "#0B3D3A",
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
  transition: "background 0.2s",
};

const viewLink: React.CSSProperties = {
  padding: "4px 12px",
  fontSize: 12,
  color: "#FF8A5B",
  textDecoration: "none",
  border: "1px solid #FF8A5B",
  borderRadius: 4,
};

const successStyle: React.CSSProperties = {
  color: "#1E7E34",
  fontSize: 13,
  marginTop: 8,
};

const errorStyle: React.CSSProperties = {
  color: "#C0392B",
  fontSize: 13,
  marginTop: 8,
};