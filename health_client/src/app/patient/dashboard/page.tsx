"use client";
import { formatAppointmentDate } from "@/lib/availability";

import { useCallback, useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch, reportFileUrl } from "@/lib/api";
import ExplanationDialog from "@/components/explanation-dialog";

// ============ Types ============
interface UserInfo {
  fullname: string;
  email: string;
  role: string;
}

interface Doctor {
  id: string;
  name: string;
  email: string;
  specialization: string;
  availableSlots: { date: string; time: string }[];
  availableDates: string[];
  availableTimes: string[];
  windowStart: string;
  windowEnd: string;
}

interface Booking {
  id: string;
  patientEmail: string;
  patientName: string;
  doctorName: string;
  doctorEmail: string;
  bookingDate: string;
  bookingTime: string;
  tokenNumber: string;
  status: string;
  createdAt: string;
}

// Lab Report Type (from lab_rep service)
interface LabReport {
  id: string;
  patientName: string;
  patientEmail: string;
  reportType: string;
  fileId: string;
  fileName?: string;
  status: string;
  uploadedAt: string;
  analysisResult?: string;
}

// Prescription Type (from doctor service)
interface Prescription {
  id: string;
  tokenNumber: string;
  doctorName: string;
  doctorEmail: string;
  patientName: string;
  patientEmail: string;
  patientAge: number;
  disease: string;
  prescriptionText: string;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export default function PatientDashboard() {
  const router = useRouter();
  const [user, setUser] = useState<UserInfo | null>(null);

  // ============ State ============
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [labReports, setLabReports] = useState<LabReport[]>([]);
  const [explanationReport, setExplanationReport] = useState<LabReport | null>(null);
  const [prescriptions, setPrescriptions] = useState<Prescription[]>([]);
  const [selectedDoctor, setSelectedDoctor] = useState<Doctor | null>(null);
  const [bookingDate, setBookingDate] = useState("");
  const [bookingTime, setBookingTime] = useState("");
  const [message, setMessage] = useState("");
  const [loading, setLoading] = useState(false);
  const [activeTab, setActiveTab] = useState<"book" | "appointments" | "prescriptions" | "labreports">("book");

  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
  const patientEmail = user?.email || "";

  // ============ Fetch User Info ============
  useEffect(() => {
    const role = localStorage.getItem("role");
    if (!token || role !== "PATIENT") {
      router.push("/login");
      return;
    }

    apiFetch<UserInfo>("/api/auth/me", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(setUser)
      .catch(() => {
        localStorage.clear();
        router.push("/login");
      });
  }, [router, token]);

  // ============ Fetch Data ============

  const fetchDoctors = useCallback(() => apiFetch<Doctor[]>("/api/doc_booking/doctors", { cache: "no-store" })
    .then(data => {
      if (data.some(item => !Array.isArray(item.availableSlots) || !item.windowStart)) throw new Error("The appointment service needs updating before schedules can be booked.");
      setDoctors(data);
      setSelectedDoctor(current => current ? data.find(item => item.email === current.email) || null : null);
    }).catch(error => { setDoctors([]); setSelectedDoctor(null); setMessage(error instanceof Error ? error.message : "Could not load doctors. Please refresh availability."); }), []);

  const fetchBookings = useCallback(() => apiFetch<Booking[]>(`/api/doc_booking/patient/${encodeURIComponent(patientEmail)}/bookings`, { cache: "no-store" })
    .then(data => setBookings(data.filter(item => item.patientEmail?.toLowerCase() === patientEmail.toLowerCase())))
    .catch(() => { setBookings([]); setMessage("Could not load appointments. Please try again."); }), [patientEmail]);

  const fetchLabReports = useCallback(() => apiFetch<LabReport[]>(`/api/reports/patient/${encodeURIComponent(patientEmail)}`, { cache: "no-store" })
    .then(setLabReports).catch(() => setMessage("Could not load lab reports.")), [patientEmail]);

  const fetchPrescriptions = useCallback(() => apiFetch<Prescription[]>("/api/doctor/prescriptions/me", { cache: "no-store" })
    .then(setPrescriptions).catch(() => setMessage("Could not load prescriptions.")), []);

  useEffect(() => {
    if (patientEmail) {
      void fetchDoctors();
      void fetchBookings();
      void fetchLabReports();
      void fetchPrescriptions();
    }
  }, [patientEmail, fetchDoctors, fetchBookings, fetchLabReports, fetchPrescriptions]);

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

  // ============ Book Appointment ============
  const handleBooking = async () => {
    if (!selectedDoctor || !bookingDate || !bookingTime) {
      setMessage("⚠️ Please select doctor, date, and time");
      return;
    }

    setLoading(true);
    setMessage("");

    try {
      const booking = await apiFetch("/api/doc_booking/book", {
        method: "POST",
        body: JSON.stringify({
          patientEmail,
          patientName: user?.fullname || "Patient",
          doctorEmail: selectedDoctor.email,
          bookingDate,
          bookingTime,
        }),
      });

      if (booking.tokenNumber) {
        setMessage(`✅ Booking confirmed! Token: ${booking.tokenNumber}`);
        fetchBookings();
        fetchDoctors();
        setSelectedDoctor(null);
        setBookingDate("");
        setBookingTime("");
        setActiveTab("appointments");
      } else if (booking.error) {
        setMessage(`❌ ${booking.error}`);
      } else {
        setMessage("❌ Booking failed. Please try again.");
      }
    } catch (error) {
      console.error("Booking error:", error);
      setMessage(error instanceof Error ? `❌ ${error.message}` : "❌ Booking failed. Please try again.");
      fetchDoctors();
      setBookingDate("");
      setBookingTime("");
    } finally {
      setLoading(false);
    }
  };

  // ============ View & Download PDF ============
  const viewFile = async (reportId: string) => {
    const tab = window.open("", "_blank");
    try {
      const url = await reportFileUrl(reportId);
      if (tab) tab.location.href = url;
      setTimeout(() => URL.revokeObjectURL(url), 120000);
    } catch {
      tab?.close();
      setMessage("Unable to open report. Please sign in and try again.");
    }
  };

  const downloadFile = async (reportId: string, filename: string) => {
    try {
      const url = await reportFileUrl(reportId);
      const link = document.createElement("a");
      link.href = url;
      link.download = filename;
      link.click();
      setTimeout(() => URL.revokeObjectURL(url), 60000);
    } catch { setMessage("Unable to download report. Please try again."); }
  };

  const handleDeleteBooking = async (bookingId: string) => {
    if (!confirm("Are you sure you want to cancel this appointment?")) return;
    try {
      await apiFetch(`/api/doc_booking/delbooking/${encodeURIComponent(bookingId)}`, { method: "DELETE" });
      await fetchBookings();
      await fetchDoctors();
      setMessage("✅ Appointment cancelled.");
    } catch (error) {
      setMessage(error instanceof Error ? error.message : "Unable to cancel appointment.");
    }
  };

  // ============ Loading State ============
  if (!user) return <div style={{ padding: 40, textAlign: "center" }}>Loading…</div>;

  // ============ Render ============
  return (
    <div
      style={{
        minHeight: "100vh",
        background: "#F6F3EC",
        color: "#000",
        padding: "40px 32px",
        fontFamily: "'Inter', sans-serif",
      }}
    >
      <div style={{ maxWidth: 900, margin: "0 auto" }}>
        {/* ===== Header ===== */}
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: 16,
          }}
        >
          <div>
            <p
              style={{
                fontSize: 12,
                letterSpacing: "0.1em",
                textTransform: "uppercase",
                color: "#FF8A5B",
                fontWeight: 700,
                margin: 0,
              }}
            >
              Patient Dashboard
            </p>
            <h1
              style={{
                fontFamily: "'Fraunces', Georgia, serif",
                fontSize: 32,
                color: "#0B3D3A",
                margin: "8px 0 4px",
              }}
            >
              Welcome, {user.fullname}
            </h1>
            <p style={{ color: "#000", fontSize: 14, margin: 0 }}>
              {user.email}
            </p>
          </div>

          <div style={{ display: "flex", alignItems: "center", gap: 12 }}>

            <button
                onClick={() => router.push("../patient/bmi")}
                style={bmiBtn}
                onMouseEnter={(e) => {
                e.currentTarget.style.background = "#0B3D3A";
                e.currentTarget.style.color = "#FFF";
             }}
                onMouseLeave={(e) => {
                e.currentTarget.style.background = "#FFF";
                e.currentTarget.style.color = "#0B3D3A";
    }}
    title="BMI Calculator"
  >
    <span>📊</span> BMI
  </button>
            <button
              onClick={goToProfile}
              style={{
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
              }}
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

            <button
              onClick={handleLogout}
              style={{
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
              }}
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

        {/* ===== Tabs ===== */}
        <div
          style={{
            display: "flex",
            gap: 8,
            marginBottom: 16,
            flexWrap: "wrap",
          }}
        >
          {[
            { key: "book", label: "📅 Book Doctor" },
            { key: "appointments", label: "📋 My Appointments" },
            { key: "prescriptions", label: "📄 Prescriptions" },
            { key: "labreports", label: "📊 Lab Reports" },
          ].map((tab) => (
            <button
              key={tab.key}
              onClick={() => {
                setActiveTab(tab.key as typeof activeTab);
                if (tab.key === "book") { fetchDoctors(); setBookingDate(""); setBookingTime(""); }
                if (tab.key === "appointments") fetchBookings();
              }}
              style={{
                padding: "8px 20px",
                background: activeTab === tab.key ? "#0B3D3A" : "#FFF",
                color: activeTab === tab.key ? "#FFF" : "#0B3D3A",
                border: "1px solid #DCD6C8",
                borderRadius: 20,
                cursor: "pointer",
                fontWeight: 500,
                fontSize: 13,
                transition: "all 0.2s",
              }}
              onMouseEnter={(e) => {
                if (activeTab !== tab.key) {
                  e.currentTarget.style.background = "#F6F3EC";
                }
              }}
              onMouseLeave={(e) => {
                if (activeTab !== tab.key) {
                  e.currentTarget.style.background = "#FFF";
                }
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {/* ============================================================ */}
        {/* ===== TAB: Book Doctor ===== */}
        {activeTab === "book" && (
          <div
            style={{
              background: "#FFF",
              border: "1px solid #DCD6C8",
              borderRadius: 10,
              padding: 24,
            }}
          >
            <h3 style={{ margin: "0 0 16px 0", color: "#0B3D3A" }}>
              Book a Doctor Appointment
            </h3>
            <p style={{ color: "#000", fontSize: 13, marginBottom: 12 }}>Choose an available appointment in the next two weeks, published by a registered doctor. Dates include the weekday; all times are in Sri Lanka time (Asia/Colombo).</p>
            <button type="button" onClick={() => { fetchDoctors(); setBookingDate(""); setBookingTime(""); }} style={{ color: "#0B3D3A", marginBottom: 16, cursor: "pointer" }}>Refresh availability</button>
            {!doctors.length && <p style={{ color: "#000", marginBottom: 16 }}>No doctors have available appointments yet. Please check again later.</p>}

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "repeat(auto-fit, minmax(180px, 1fr))",
                gap: 12,
              }}
            >
              <select
                style={{
                  padding: 10,
                  border: "1px solid #DCD6C8",
                  borderRadius: 6,
                  background: "#FFF",
                  color: "#1B1B18",
                }}
                onChange={(e) => {
                  const doctor = doctors.find((d) => d.email === e.target.value);
                  setSelectedDoctor(doctor || null);
                  setBookingDate("");
                  setBookingTime("");
                }}
                value={selectedDoctor?.email || ""}
              >
                <option value="">-- Select Doctor --</option>
                {doctors.map((doctor) => (
                  <option key={doctor.id} value={doctor.email}>
                    {doctor.name} - {doctor.specialization}
                  </option>
                ))}
              </select>

              <select
                style={{
                  padding: 10,
                  border: "1px solid #DCD6C8",
                  borderRadius: 6,
                  background: "#FFF",
                  color: "#1B1B18",
                }}
                onChange={(e) => { setBookingDate(e.target.value); setBookingTime(""); }}
                value={bookingDate}
                disabled={!selectedDoctor}
              >
                <option value="">-- Select Date --</option>
                {selectedDoctor?.availableDates.map((date) => (
                  <option key={date} value={date}>
                    {formatAppointmentDate(date)}
                  </option>
                ))}
              </select>

              <select
                style={{
                  padding: 10,
                  border: "1px solid #DCD6C8",
                  borderRadius: 6,
                  background: "#FFF",
                  color: "#1B1B18",
                }}
                onChange={(e) => setBookingTime(e.target.value)}
                value={bookingTime}
                disabled={!bookingDate}
              >
                <option value="">-- Select Time --</option>
                {selectedDoctor?.availableSlots?.filter(slot => slot.date === bookingDate).map(({ time }) => (
                  <option key={time} value={time}>
                    {time}
                  </option>
                ))}
              </select>
            </div>

            <button
              onClick={handleBooking}
              disabled={loading || !selectedDoctor || !bookingDate || !bookingTime}
              style={{
                marginTop: 16,
                padding: "10px 24px",
                background: "#0B3D3A",
                color: "#FFFFFF",
                border: "none",
                borderRadius: 6,
                fontWeight: 600,
                cursor: loading ? "not-allowed" : "pointer",
                opacity: loading ? 0.6 : 1,
              }}
            >
              {loading ? "⏳ Booking..." : "Book Appointment"}
            </button>

            {message && (
              <div
                style={{
                  marginTop: 12,
                  padding: 10,
                  background: message.includes("✅") ? "#E6F4EA" : "#FDE8E8",
                  color: message.includes("✅") ? "#1E7E34" : "#C62828",
                  borderRadius: 6,
                  fontSize: 14,
                }}
              >
                {message}
              </div>
            )}

            {selectedDoctor && (
              <div
                style={{
                  marginTop: 16,
                  padding: 12,
                  background: "#F6F3EC",
                  borderRadius: 6,
                }}
              >
                <p style={{ margin: 0, fontSize: 13, color: "#4A463E" }}>
                  <strong>Available Dates:</strong>{" "}
                  {selectedDoctor.availableDates.map(formatAppointmentDate).join(", ")}
                </p>
                <p style={{ margin: "4px 0 0 0", fontSize: 13, color: "#4A463E" }}>
                  <strong>Times for selected date:</strong>{" "}
                  {bookingDate ? selectedDoctor.availableSlots.filter(slot => slot.date === bookingDate).map(slot => slot.time).join(", ") : "Select a date above"}
                </p>
              </div>
            )}
          </div>
        )}

        {/* ============================================================ */}
        {/* ===== TAB: Appointments ===== */}
        {activeTab === "appointments" && (
          <div
            style={{
              background: "#FFF",
              border: "1px solid #DCD6C8",
              borderRadius: 10,
              padding: 24,
            }}
          >
            <h3 style={{ margin: "0 0 16px 0", color: "#0B3D3A" }}>
              My Appointments
            </h3>
            {message && <p role="status" style={{ color: "#000", marginBottom: 12 }}>{message}</p>}
            {bookings.length === 0 ? (
              <p style={{ color: "#000" }}>No appointments yet. Book one above!</p>
            ) : (
              bookings.map((b) => (
                <div
                  key={b.id}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    padding: "12px 0",
                    borderBottom: "1px solid #EEE",
                  }}
                >
                  <div>
                    <strong>{b.doctorName}</strong>
                    <p style={{ margin: 0, fontSize: 13, color: "#000" }}>
                      {formatAppointmentDate(b.bookingDate)} at {b.bookingTime}
                    </p>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <div>
                      <span style={{ fontWeight: "bold", color: "#0B3D3A", fontSize: 16 }}>
                        #{b.tokenNumber}
                      </span>
                      <div style={{ display: "flex", alignItems: "center", gap: 10 }}>
                      <span
                        style={{
                          marginLeft: 12,
                          fontSize: 12,
                          background: b.status === "CONFIRMED" ? "#E6F4EA" : "#FDE8E8",
                          color: b.status === "CONFIRMED" ? "#1E7E34" : "#C62828",
                          padding: "2px 12px",
                          borderRadius: 12,
                        }}
                      >
                        {b.status}
                      </span>
                      {/* Delete button for each booking */}
                      <button
                onClick={() => handleDeleteBooking(b.id)}
                style={{
                  padding: "4px 10px",
                  borderRadius: 6,
                  border: "none",
                  background: "#FDE8E8",
                  color: "#C62828",
                  cursor: "pointer",
                  fontSize: 14,
                  transition: "background 0.2s",
                }}
                onMouseEnter={(e) => {
                  e.currentTarget.style.background = "#F5C6C6";
                }}
                onMouseLeave={(e) => {
                  e.currentTarget.style.background = "#FDE8E8";
                }}
                title="Cancel appointment"
              >
                🗑️
              </button>
                      </div>
                    </div>
                    <p style={{ margin: "4px 0 0 0", fontSize: 11, color: "#000" }}>
                      {new Date(b.createdAt).toLocaleDateString()}
                    </p>
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* ============================================================ */}
        {/* ===== TAB: Prescriptions ===== */}
        {activeTab === "prescriptions" && (
          <div
            style={{
              background: "#FFF",
              border: "1px solid #DCD6C8",
              borderRadius: 10,
              padding: 24,
            }}
          >
            <h3 style={{ margin: "0 0 16px 0", color: "#0B3D3A" }}>
              📄 My Prescriptions
            </h3>
            {prescriptions.length === 0 ? (
              <p style={{ color: "#000" }}>No prescriptions available yet.</p>
            ) : (
              prescriptions.map((p) => (
                <div
                  key={p.id}
                  style={{
                    padding: "12px 0",
                    borderBottom: "1px solid #EEE",
                  }}
                >
                  <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center" }}>
                    <div>
                      <strong style={{ fontSize: 15, color: "#0B3D3A" }}>
                        Dr. {p.doctorName}
                      </strong>
                      <p style={{ margin: "2px 0 0", fontSize: 13, color: "#000" }}>
                        {p.doctorEmail}
                      </p>
                    </div>
                    
                    <span
                      style={{
                        padding: "2px 12px",
                        borderRadius: 12,
                        fontSize: 11,
                        background: p.status === "ACTIVE" ? "#E6F4EA" : p.status === "EXPIRED" ? "#FDE8E8" : "#FFF3E0",
                        color: p.status === "ACTIVE" ? "#1E7E34" : p.status === "EXPIRED" ? "#C62828" : "#E65100",
                      }}
                    >
                      {p.status || "ACTIVE"}
                    </span>                   
                  
                  </div>

                  <p style={{ margin: "6px 0 0 0", fontSize: 14, color: "#1B1B18" }}>
                    <strong>Token:</strong> #{p.tokenNumber}
                  </p>

                  <div style={{ marginTop: 8, padding: 10, background: "#F6F3EC", borderRadius: 6 }}>
                    <p style={{ margin: 0, fontSize: 14, color: "#1B1B18", whiteSpace: "pre-wrap" }}>
                      {p.prescriptionText}
                    </p>
                  </div>

                  <div style={{ marginTop: 8, display: "flex", gap: 16, fontSize: 12, color: "#000" }}>
                    <span>📅 Created: {new Date(p.createdAt).toLocaleDateString()}</span>
                    {p.updatedAt && (
                      <span>🔄 Updated: {new Date(p.updatedAt).toLocaleDateString()}</span>
                    )}
                  </div>
                </div>
              ))
            )}
          </div>
        )}

        {/* ============================================================ */}
        {/* ===== TAB: Lab Reports ===== */}
        {activeTab === "labreports" && (
          <div
            style={{
              background: "#FFF",
              border: "1px solid #DCD6C8",
              borderRadius: 10,
              padding: 24,
            }}
          >
            <h3 style={{ margin: "0 0 16px 0", color: "#0B3D3A" }}>
              📊 My Lab Reports
            </h3>
            {labReports.length === 0 ? (
              <p style={{ color: "#000" }}>No lab reports available yet.</p>
            ) : (
              labReports.map((report) => (
                <div
                  key={report.id}
                  style={{
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                    flexWrap: "wrap",
                    gap: 12,
                    padding: "12px 0",
                    borderBottom: "1px solid #EEE",
                  }}
                >
                  <div>
                    <strong>{report.reportType}</strong>
                    <p style={{ margin: 0, fontSize: 12, color: "#000" }}>
                      Uploaded: {new Date(report.uploadedAt).toLocaleDateString()}
                    </p>
                    <span
                      style={{
                        padding: "2px 12px",
                        borderRadius: 12,
                        fontSize: 11,
                        background: report.status === "COMPLETED" ? "#E6F4EA" : "#FFF3E0",
                        color: report.status === "COMPLETED" ? "#1E7E34" : "#E65100",
                      }}
                    >
                      {report.status}
                    </span>
                  </div>
                  <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
                    <button
                      onClick={() => viewFile(report.id)}
                      style={{
                        padding: "6px 16px",
                        background: "#0B3D3A",
                        color: "#FFF",
                        border: "none",
                        borderRadius: 6,
                        cursor: "pointer",
                      }}
                    >
                      👁️ View PDF
                    </button>
                    <button
                      type="button"
                      onClick={() => setExplanationReport(report)}
                      style={{
                        padding: "6px 16px",
                        background: "#E8F5E9",
                        color: "#1B5E20",
                        border: "1px solid #DCD6C8",
                        borderRadius: 6,
                        cursor: "pointer",
                      }}
                    >
                      View explanation
                    </button>
                    <button
                      onClick={() => downloadFile(report.id, `${report.reportType}.pdf`)}
                      style={{
                        padding: "6px 16px",
                        background: "#FF8A5B",
                        color: "#FFF",
                        border: "none",
                        borderRadius: 6,
                        cursor: "pointer",
                      }}
                    >
                      ⬇️ Download PDF
                    </button>
                  </div>
                </div>
              ))
            )}
          </div>
        )}
        
      </div>
      {explanationReport && (
        <ExplanationDialog
          key={explanationReport.id}
          report={explanationReport}
          onClose={() => setExplanationReport(null)}
        />
      )}
    </div>
  );
}

const bmiBtn: React.CSSProperties = {
  padding: "10px 18px",
  borderRadius: 8,
  background: "#FFF",
  color: "#0B3D3A",
  border: "2px solid #0B3D3A",
  cursor: "pointer",
  fontSize: 14,
  fontWeight: 600,
  display: "flex",
  alignItems: "center",
  gap: 6,
  transition: "all 0.2s",
};
