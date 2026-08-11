"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";

interface DoctorInfo {
  fullName: string;
  email: string;
  role: string;
}

interface Appointment {
  id: string;
  patientId: string;
  patientName: string;
  tokenNumber: number;
  scheduledTime: string;
  status: string; // BOOKED | ARRIVED | COMPLETED
}

export default function DoctorDashboard() {
  const router = useRouter();
  const [doctor, setDoctor] = useState<DoctorInfo | null>(null);
  const [appointments, setAppointments] = useState<Appointment[]>([]);
  const [selected, setSelected] = useState<Appointment | null>(null);
  const [prescriptionText, setPrescriptionText] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [successMsg, setSuccessMsg] = useState("");

  useEffect(() => {
    const token = localStorage.getItem("token");
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

    // Appointment service endpoint — adjust path once teammate confirms it
    apiFetch<Appointment[]>("/api/appointments/doctor-view", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(setAppointments)
      .catch(() => setAppointments([]));
  }, [router]);

  function markArrived(id: string) {
    const token = localStorage.getItem("token");
    apiFetch(`/api/appointments/${id}/arrived`, {
      method: "PATCH",
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(() => {
        setAppointments((prev) =>
          prev.map((a) => (a.id === id ? { ...a, status: "ARRIVED" } : a))
        );
      })
      .catch(() => {});
  }

  async function submitPrescription() {
    if (!selected || !prescriptionText.trim()) return;
    setSubmitting(true);
    setSuccessMsg("");
    const token = localStorage.getItem("token");
    try {
      // Prescription service endpoint — adjust path once teammate confirms it
      await apiFetch("/api/prescriptions", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          patientId: selected.patientId,
          appointmentId: selected.id,
          content: prescriptionText,
        }),
      });
      setSuccessMsg("Prescription saved and sent to patient.");
      setAppointments((prev) =>
        prev.map((a) => (a.id === selected.id ? { ...a, status: "COMPLETED" } : a))
      );
      setPrescriptionText("");
      setSelected(null);
    } catch {
      setSuccessMsg("Failed to save prescription. Try again.");
    } finally {
      setSubmitting(false);
    }
  }

  if (!doctor) return <div style={{ padding: 40, fontFamily: "'Inter', sans-serif" }}>Loading…</div>;

  return (
    <div style={page}>
      <div style={wrap}>
        <p style={eyebrow}>Doctor Dashboard</p>
        <h1 style={heading}>Welcome, Dr. {doctor.fullName}</h1>
        <p style={subtext}>{doctor.email}</p>

        <div style={{ display: "grid", gridTemplateColumns: "1fr 1.2fr", gap: 24, marginTop: 32 }}>
          {/* Patient queue */}
          <div style={card}>
            <h2 style={cardTitle}>Today&apos;s Patients</h2>
            {appointments.length === 0 && (
              <p style={{ color: "#8A8577", fontSize: 14 }}>No appointments yet.</p>
            )}
            {appointments.map((a) => (
              <div
                key={a.id}
                onClick={() => setSelected(a)}
                style={{
                  ...queueItem,
                  borderColor: selected?.id === a.id ? "#0B3D3A" : "#DCD6C8",
                }}
              >
                <div>
                  <p style={{ margin: 0, fontWeight: 600, color: "#1B1B18" }}>
                    Token #{a.tokenNumber} — {a.patientName}
                  </p>
                  <p style={{ margin: "2px 0 0", fontSize: 13, color: "#8A8577" }}>
                    {a.scheduledTime}
                  </p>
                </div>
                <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                  <span style={statusBadge(a.status)}>{a.status}</span>
                  {a.status === "BOOKED" && (
                    <button
                      onClick={(e) => {
                        e.stopPropagation();
                        markArrived(a.id);
                      }}
                      style={smallBtn}
                    >
                      Mark Arrived
                    </button>
                  )}
                </div>
              </div>
            ))}
          </div>

          {/* Prescription panel */}
          <div style={card}>
            <h2 style={cardTitle}>Write Prescription</h2>
            {!selected ? (
              <p style={{ color: "#8A8577", fontSize: 14 }}>
                Select a patient from the list to write a prescription.
              </p>
            ) : (
              <>
                <p style={{ fontSize: 14, marginBottom: 12 }}>
                  For <strong>{selected.patientName}</strong> (Token #{selected.tokenNumber})
                </p>
                <textarea
                  value={prescriptionText}
                  onChange={(e) => setPrescriptionText(e.target.value)}
                  placeholder="Diagnosis, medication, dosage, instructions…"
                  rows={8}
                  style={textarea}
                />
                {successMsg && (
                  <p style={{ fontSize: 13, color: successMsg.startsWith("Failed") ? "#C0392B" : "#0B3D3A", marginTop: 8 }}>
                    {successMsg}
                  </p>
                )}
                <button
                  onClick={submitPrescription}
                  disabled={submitting || !prescriptionText.trim()}
                  style={submitBtn}
                >
                  {submitting ? "Saving…" : "Save & Send to Patient"}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

function statusBadge(status: string): React.CSSProperties {
  const colors: Record<string, string> = {
    BOOKED: "#8A8577",
    ARRIVED: "#FF8A5B",
    COMPLETED: "#0B3D3A",
  };
  return {
    fontSize: 11,
    fontWeight: 700,
    letterSpacing: "0.05em",
    textTransform: "uppercase",
    color: "#FFF",
    background: colors[status] || "#8A8577",
    padding: "3px 8px",
    borderRadius: 4,
  };
}

const page: React.CSSProperties = {
  minHeight: "100vh",
  background: "#F6F3EC",
  padding: "40px 32px",
  fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
};

const wrap: React.CSSProperties = { maxWidth: 960, margin: "0 auto" };

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
};

const smallBtn: React.CSSProperties = {
  fontSize: 12,
  padding: "5px 10px",
  border: "1px solid #0B3D3A",
  background: "transparent",
  color: "#0B3D3A",
  borderRadius: 5,
  cursor: "pointer",
};

const textarea: React.CSSProperties = {
  width: "100%",
  padding: 12,
  fontSize: 14,
  border: "1px solid #DCD6C8",
  borderRadius: 7,
  resize: "vertical",
  fontFamily: "inherit",
};

const submitBtn: React.CSSProperties = {
  marginTop: 14,
  padding: "12px 20px",
  fontSize: 14.5,
  fontWeight: 600,
  color: "#F6F3EC",
  background: "#FF8A5B",
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
};