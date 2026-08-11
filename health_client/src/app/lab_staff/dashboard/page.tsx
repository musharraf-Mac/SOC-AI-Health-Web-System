"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";

interface StaffInfo {
  fullName: string;
  email: string;
  role: string;
}

const CATEGORIES = ["Blood Test", "MRI", "CT Scan", "X-Ray", "Ultrasound", "ECG"];

export default function LabStaffDashboard() {
  const router = useRouter();
  const [staff, setStaff] = useState<StaffInfo | null>(null);
  const [patientId, setPatientId] = useState("");
  const [category, setCategory] = useState(CATEGORIES[0]);
  const [file, setFile] = useState<File | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState<{ text: string; ok: boolean } | null>(null);

  useEffect(() => {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");
    if (!token || role !== "LAB_STAFF") {
      router.push("/login");
      return;
    }
    apiFetch<StaffInfo>("/api/auth/me", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(setStaff)
      .catch(() => {
        localStorage.clear();
        router.push("/login");
      });
  }, [router]);

  async function handleUpload(e: React.FormEvent) {
    e.preventDefault();
    if (!file || !patientId.trim()) {
      setMessage({ text: "Patient ID and report file are required.", ok: false });
      return;
    }
    setSubmitting(true);
    setMessage(null);

    const token = localStorage.getItem("token");
    const formData = new FormData();
    formData.append("patientId", patientId);
    formData.append("category", category);
    formData.append("file", file);

    try {
      // Lab Report service endpoint — adjust path once teammate confirms it.
      // Note: don't set Content-Type manually for FormData, browser sets the boundary.
      const res = await fetch("http://localhost:8084/api/lab-reports/upload", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: formData,
      });
      if (!res.ok) throw new Error();

      setMessage({ text: "Report uploaded. OCR + AI explanation in progress.", ok: true });
      setPatientId("");
      setFile(null);
    } catch {
      setMessage({ text: "Upload failed. Check patient ID and try again.", ok: false });
    } finally {
      setSubmitting(false);
    }
  }

  if (!staff) return <div style={{ padding: 40, fontFamily: "'Inter', sans-serif" }}>Loading…</div>;

  return (
    <div style={page}>
      <div style={wrap}>
        <p style={eyebrow}>Lab Staff Dashboard</p>
        <h1 style={heading}>Welcome, {staff.fullName}</h1>
        <p style={subtext}>{staff.email}</p>

        <div style={card}>
          <h2 style={cardTitle}>Upload Lab Report</h2>
          <p style={{ fontSize: 13.5, color: "#8A8577", marginTop: -8, marginBottom: 20 }}>
            The report will be OCR-scanned and explained automatically, then sent to the
            patient&apos;s profile.
          </p>

          <form onSubmit={handleUpload} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <label style={fieldWrap}>
              <span style={fieldLabel}>Patient ID</span>
              <input
                type="text"
                value={patientId}
                onChange={(e) => setPatientId(e.target.value)}
                placeholder="e.g. 66a7928..."
                style={input}
                required
              />
            </label>

            <label style={fieldWrap}>
              <span style={fieldLabel}>Report Category</span>
              <select
                value={category}
                onChange={(e) => setCategory(e.target.value)}
                style={input}
              >
                {CATEGORIES.map((c) => (
                  <option key={c} value={c}>
                    {c}
                  </option>
                ))}
              </select>
            </label>

            <label style={fieldWrap}>
              <span style={fieldLabel}>Report File (image or PDF)</span>
              <input
                type="file"
                accept="image/*,application/pdf"
                onChange={(e) => setFile(e.target.files?.[0] ?? null)}
                style={{ fontSize: 14 }}
                required
              />
            </label>

            {message && (
              <p style={{ fontSize: 13.5, color: message.ok ? "#0B3D3A" : "#C0392B", margin: 0 }}>
                {message.text}
              </p>
            )}

            <button type="submit" disabled={submitting} style={submitBtn}>
              {submitting ? "Uploading…" : "Upload Report"}
            </button>
          </form>
        </div>
      </div>
    </div>
  );
}

const page: React.CSSProperties = {
  minHeight: "100vh",
  background: "#F6F3EC",
  padding: "40px 32px",
  fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
};

const wrap: React.CSSProperties = { maxWidth: 560, margin: "0 auto" };

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
  fontSize: 28,
  color: "#0B3D3A",
  margin: "8px 0 4px",
};

const subtext: React.CSSProperties = { color: "#8A8577", fontSize: 14, margin: "0 0 28px" };

const card: React.CSSProperties = {
  background: "#FFF",
  border: "1px solid #DCD6C8",
  borderRadius: 10,
  padding: 24,
};

const cardTitle: React.CSSProperties = {
  fontFamily: "'Fraunces', Georgia, serif",
  fontSize: 19,
  color: "#0B3D3A",
  margin: "0 0 4px",
};

const fieldWrap: React.CSSProperties = { display: "flex", flexDirection: "column", gap: 6 };

const fieldLabel: React.CSSProperties = { fontSize: 13, fontWeight: 500, color: "#1B1B18" };

const input: React.CSSProperties = {
  padding: "11px 13px",
  fontSize: 14.5,
  border: "1px solid #DCD6C8",
  borderRadius: 7,
  outline: "none",
  color: "#1B1B18",
  background: "#FEFDFB",
};

const submitBtn: React.CSSProperties = {
  marginTop: 4,
  padding: 13,
  fontSize: 15,
  fontWeight: 600,
  color: "#F6F3EC",
  background: "#FF8A5B",
  border: "none",
  borderRadius: 8,
  cursor: "pointer",
};