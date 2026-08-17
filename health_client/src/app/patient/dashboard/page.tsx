"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";

// ============ Types ============
interface UserInfo {
  fullName: string;
  email: string;
  role: string;
}

interface Doctor {
  id: string;
  name: string;
  email: string;
  specialization: string;
  availableDates: string[];
  availableTimes: string[];
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

interface FileItem {
  id: string;
  fileId: string;
  filename: string;
  fileCategory: "PRESCRIPTION" | "LAB_REPORT";
  description: string;
  uploadedAt: string;
  uploadedBy: string;
}

// @Abisha-2002 This page for modify it your port is 8082
export default function PatientDashboard() {
  const router = useRouter();
  const [user, setUser] = useState<UserInfo | null>(null);

  // ============ State ============
  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [bookings, setBookings] = useState<Booking[]>([]);
  const [files, setFiles] = useState<FileItem[]>([]);
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
  }, [router]);

  // ============ Fetch Data ============
  useEffect(() => {
    if (patientEmail) {
      fetchDoctors();
      fetchBookings();
      fetchFiles();
    }
  }, [patientEmail]);

  const fetchDoctors = async () => {
    try {
      const res = await fetch("http://localhost:8082/api/doc_booking/doctors");
      const data = await res.json();
      setDoctors(data);
    } catch (error) {
      console.error("Error fetching doctors:", error);
      setMessage("❌ Could not load doctors. Make sure backend is running.");
    }
  };

  const fetchBookings = async () => {
    try {
      const res = await fetch(
        `http://localhost:8082/api/doc_booking/patient/${patientEmail}/bookings`
      );
      const data = await res.json();
      setBookings(data);
    } catch (error) {
      console.error("Error fetching bookings:", error);
    }
  };

  const fetchFiles = async () => {
    try {
      const res = await fetch(
        `http://localhost:8082/api/doc_booking/files/patient/${patientEmail}`
      );
      const data = await res.json();
      setFiles(data);
    } catch (error) {
      console.error("Error fetching files:", error);
    }
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
      const res = await fetch("http://localhost:8082/api/doc_booking/book", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          patientEmail,
          patientName: user?.fullName || "Patient",
          doctorEmail: selectedDoctor.email,
          bookingDate,
          bookingTime,
        }),
      });

      if (res.ok) {
        const booking = await res.json();
        setMessage(`✅ Booking confirmed! Token: ${booking.tokenNumber}`);
        fetchBookings();
        setSelectedDoctor(null);
        setBookingDate("");
        setBookingTime("");
        setActiveTab("appointments");
      } else {
        const error = await res.text();
        setMessage(`❌ ${error}`);
      }
    } catch (error) {
      setMessage("❌ Booking failed. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // ============ View & Download PDF ============
  const viewFile = (fileId: string) => {
    window.open(
      `http://localhost:8082/api/doc_booking/files/${fileId}/view`,
      "_blank"
    );
  };

  const downloadFile = (fileId: string, filename: string) => {
    const link = document.createElement("a");
    link.href = `http://localhost:8082/api/doc_booking/files/${fileId}/view`;
    link.download = filename;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // ============ Loading State ============
  if (!user) return <div style={{ padding: 40, textAlign: "center" }}>Loading…</div>;

  // ============ Render ============
  return (
    <div
      style={{
        minHeight: "100vh",
        background: "#F6F3EC",
        padding: "40px 32px",
        fontFamily: "'Inter', sans-serif",
      }}
    >
      <div style={{ maxWidth: 900, margin: "0 auto" }}>
        {/* ===== Header ===== */}
        <p
          style={{
            fontSize: 12,
            letterSpacing: "0.1em",
            textTransform: "uppercase",
            color: "#FF8A5B",
            fontWeight: 700,
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
          Welcome, {user.fullName}
        </h1>
        <p style={{ color: "#8A8577", fontSize: 14, marginBottom: 24 }}>
          {user.email}
        </p>

        {/* ============================================================ */}
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
              onClick={() => setActiveTab(tab.key as any)}
              style={{
                padding: "8px 20px",
                background: activeTab === tab.key ? "#0B3D3A" : "#FFF",
                color: activeTab === tab.key ? "#FFF" : "#0B3D3A",
                border: "1px solid #DCD6C8",
                borderRadius: 20,
                cursor: "pointer",
                fontWeight: 500,
                fontSize: 13,
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

            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr 1fr",
                gap: 12,
              }}
            >
              <select
                style={{
                  padding: 10,
                  border: "1px solid #DCD6C8",
                  borderRadius: 6,
                  background: "#FFF",
                }}
                onChange={(e) => {
                  const doctor = doctors.find(
                    (d) => d.email === e.target.value
                  );
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
                }}
                onChange={(e) => setBookingDate(e.target.value)}
                value={bookingDate}
                disabled={!selectedDoctor}
              >
                <option value="">-- Select Date --</option>
                {selectedDoctor?.availableDates.map((date) => (
                  <option key={date} value={date}>
                    {date}
                  </option>
                ))}
              </select>

              <select
                style={{
                  padding: 10,
                  border: "1px solid #DCD6C8",
                  borderRadius: 6,
                  background: "#FFF",
                }}
                onChange={(e) => setBookingTime(e.target.value)}
                value={bookingTime}
                disabled={!bookingDate}
              >
                <option value="">-- Select Time --</option>
                {selectedDoctor?.availableTimes.map((time) => (
                  <option key={time} value={time}>
                    {time}
                  </option>
                ))}
              </select>
            </div>

            <button
              onClick={handleBooking}
              disabled={loading}
              style={{
                marginTop: 16,
                padding: "10px 24px",
                background: "#0B3D3A",
                color: "#FFF",
                border: "none",
                borderRadius: 6,
                fontWeight: 600,
                cursor: loading ? "not-allowed" : "pointer",
                opacity: loading ? 0.6 : 1,
              }}
            >
              {loading ? "⏳ Booking..." : "✅ Book Appointment"}
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

            {/* Doctor Availability Info */}
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
                  {selectedDoctor.availableDates.join(", ")}
                </p>
                <p style={{ margin: "4px 0 0 0", fontSize: 13, color: "#4A463E" }}>
                  <strong>Available Times:</strong>{" "}
                  {selectedDoctor.availableTimes.join(", ")}
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
            {bookings.length === 0 ? (
              <p style={{ color: "#8A8577" }}>
                No appointments yet. Book one above!
              </p>
            ) : (
              <div>
                {bookings.map((b) => (
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
                      <p style={{ margin: 0, fontSize: 13, color: "#8A8577" }}>
                        {b.bookingDate} at {b.bookingTime}
                      </p>
                    </div>
                    <div style={{ textAlign: "right" }}>
                      <div>
                        <span
                          style={{
                            fontWeight: "bold",
                            color: "#0B3D3A",
                            fontSize: 16,
                          }}
                        >
                          #{b.tokenNumber}
                        </span>
                        <span
                          style={{
                            marginLeft: 12,
                            fontSize: 12,
                            background:
                              b.status === "CONFIRMED"
                                ? "#E6F4EA"
                                : "#FDE8E8",
                            color:
                              b.status === "CONFIRMED" ? "#1E7E34" : "#C62828",
                            padding: "2px 12px",
                            borderRadius: 12,
                          }}
                        >
                          {b.status}
                        </span>
                      </div>
                      <p
                        style={{
                          margin: "4px 0 0 0",
                          fontSize: 11,
                          color: "#8A8577",
                        }}
                      >
                        {new Date(b.createdAt).toLocaleDateString()}
                      </p>
                    </div>
                  </div>
                ))}
              </div>
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
            {files.filter((f) => f.fileCategory === "PRESCRIPTION").length ===
            0 ? (
              <p style={{ color: "#8A8577" }}>
                No prescriptions available yet.
              </p>
            ) : (
              files
                .filter((f) => f.fileCategory === "PRESCRIPTION")
                .map((file) => (
                  <div
                    key={file.id}
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      padding: "12px 0",
                      borderBottom: "1px solid #EEE",
                    }}
                  >
                    <div>
                      <strong>{file.filename}</strong>
                      <p
                        style={{
                          margin: 0,
                          fontSize: 12,
                          color: "#8A8577",
                        }}
                      >
                        {file.description} • Uploaded:{" "}
                        {new Date(file.uploadedAt).toLocaleDateString()}
                      </p>
                      <p style={{ margin: 0, fontSize: 11, color: "#aaa" }}>
                        By: {file.uploadedBy}
                      </p>
                    </div>
                    <div style={{ display: "flex", gap: 8 }}>
                      <button
                        onClick={() => viewFile(file.fileId)}
                        style={{
                          padding: "6px 16px",
                          background: "#0B3D3A",
                          color: "#FFF",
                          border: "none",
                          borderRadius: 6,
                          cursor: "pointer",
                        }}
                      >
                        👁️ View
                      </button>
                      <button
                        onClick={() => downloadFile(file.fileId, file.filename)}
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
            {files.filter((f) => f.fileCategory === "LAB_REPORT").length === 0 ? (
              <p style={{ color: "#8A8577" }}>
                No lab reports available yet.
              </p>
            ) : (
              files
                .filter((f) => f.fileCategory === "LAB_REPORT")
                .map((file) => (
                  <div
                    key={file.id}
                    style={{
                      display: "flex",
                      justifyContent: "space-between",
                      alignItems: "center",
                      padding: "12px 0",
                      borderBottom: "1px solid #EEE",
                    }}
                  >
                    <div>
                      <strong>{file.filename}</strong>
                      <p
                        style={{
                          margin: 0,
                          fontSize: 12,
                          color: "#8A8577",
                        }}
                      >
                        {file.description} • Uploaded:{" "}
                        {new Date(file.uploadedAt).toLocaleDateString()}
                      </p>
                      <p style={{ margin: 0, fontSize: 11, color: "#aaa" }}>
                        By: {file.uploadedBy}
                      </p>
                    </div>
                    <div style={{ display: "flex", gap: 8 }}>
                      <button
                        onClick={() => viewFile(file.fileId)}
                        style={{
                          padding: "6px 16px",
                          background: "#0B3D3A",
                          color: "#FFF",
                          border: "none",
                          borderRadius: 6,
                          cursor: "pointer",
                        }}
                      >
                        👁️ View
                      </button>
                      <button
                        onClick={() => downloadFile(file.fileId, file.filename)}
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
    </div>
  );
}