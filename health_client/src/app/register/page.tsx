"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";


export default function RegisterPage() {
  const SERVER_URL = process.env.NEXT_PUBLIC_SERVER_URL || "http://localhost:8081";
  const router = useRouter();
  const [role, setRole] = useState<"PATIENT" | "DOCTOR" | "LAB_STAFF">("PATIENT");
  const [form, setForm] = useState({
    fullName: "",
    email: "",
    phoneNumber:"",
    password: "",
    confirmPassword: "",
  });
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  function update(field: string, value: string) {
    setForm((prev) => ({ ...prev, [field]: value }));
  }

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");

    if (form.password !== form.confirmPassword) {
      setError("Passwords don't match.");
      return;
    }
    if (form.password.length < 6) {
      setError("Password must be at least 6 characters.");
      return;
    }

    setLoading(true);
    try {
      await apiFetch(`${SERVER_URL}/api/auth/register`, {
        method: "POST",
        body: JSON.stringify({
          fullName: form.fullName,
          email: form.email,
          password: form.password,
          role,
        }),
      });
      router.push("/login?registered=true");
    } catch (err) {
      setError("Registration failed. That email may already be in use.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={pageWrap}>
      <div style={card}>
        <p style={eyebrow}>Meridian Clinic</p>
        <h1 style={heading}>Create your account</h1>
        <p style={subtext}>Takes less than a minute.</p>

        {/* Role toggle */}
        <div style={roleToggle}>
          <button
            type="button"
            onClick={() => setRole("PATIENT")}
            style={role === "PATIENT" ? roleBtnActive : roleBtn}
          >
            I&apos;m a Patient
          </button>
          <button
            type="button"
            onClick={() => setRole("DOCTOR")}
            style={role === "DOCTOR" ? roleBtnActive : roleBtn}
          >
            I&apos;m a Doctor
          </button>
          <button
            type="button"
            onClick={() => setRole("LAB_STAFF")}
            style={role === "LAB_STAFF" ? roleBtnActive : roleBtn}
          >
            I&apos;m a Lab Staff
          </button>
        </div>

        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          <Field
            label="Full name"
            type="text"
            value={form.fullName}
            onChange={(v) => update("fullName", v)}
            placeholder="Dr. Kamal Perera"
            required
          />
          <Field
            label="Email"
            type="email"
            value={form.email}
            onChange={(v) => update("email", v)}
            placeholder="you@example.com"
            required
          />
          <Field
            label="Phone_Number"
            type="number"
            value={form.phoneNumber}
            onChange={(v) => update("phoneNumber", v)}
            placeholder="+94 77 123 4567"
            required
          />
          <Field
            label="Password"
            type="password"
            value={form.password}
            onChange={(v) => update("password", v)}
            placeholder="At least 6 characters"
            required
          />
          <Field
            label="Confirm password"
            type="password"
            value={form.confirmPassword}
            onChange={(v) => update("confirmPassword", v)}
            placeholder="Re-enter password"
            required
          />

          {error && <p style={errorText}>{error}</p>}

          <button type="submit" disabled={loading} style={submitBtn}>
            {loading ? "Creating account…" : "Create account"}
          </button>
        </form>

        <p style={footerText}>
          Already have an account?{" "}
          <a href="/login" style={link}>
            Log in
          </a>
        </p>
      </div>
    </div>
  );
}

function Field({
  label,
  type,
  value,
  onChange,
  placeholder,
  required,
}: {
  label: string;
  type: string;
  value: string;
  onChange: (v: string) => void;
  placeholder?: string;
  required?: boolean;
}) {
  return (
    <label style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
      <span style={fieldLabel}>{label}</span>
      <input
        type={type}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        placeholder={placeholder}
        required={required}
        style={input}
      />
    </label>
  );
}

// ---- styles (shared token set with homepage) ----
const pageWrap: React.CSSProperties = {
  minHeight: "100vh",
  background: "#F6F3EC",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "32px",
  fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
};

const card: React.CSSProperties = {
  width: "100%",
  maxWidth: "420px",
  background: "#FFFFFF",
  border: "1px solid #DCD6C8",
  borderRadius: "12px",
  padding: "40px 36px",
};

const eyebrow: React.CSSProperties = {
  fontSize: "12px",
  letterSpacing: "0.1em",
  textTransform: "uppercase",
  color: "#FF8A5B",
  fontWeight: 700,
  margin: 0,
};

const heading: React.CSSProperties = {
  fontFamily: "'Fraunces', Georgia, serif",
  fontSize: "26px",
  fontWeight: 500,
  color: "#0B3D3A",
  margin: "8px 0 4px",
};

const subtext: React.CSSProperties = {
  fontSize: "14px",
  color: "#8A8577",
  margin: "0 0 24px",
};

const roleToggle: React.CSSProperties = {
  display: "flex",
  background: "#F6F3EC",
  border: "1px solid #DCD6C8",
  borderRadius: "8px",
  padding: "4px",
  marginBottom: "22px",
};

const roleBtn: React.CSSProperties = {
  flex: 1,
  padding: "9px 12px",
  fontSize: "13.5px",
  fontWeight: 500,
  color: "#4A463E",
  background: "transparent",
  border: "none",
  borderRadius: "6px",
  cursor: "pointer",
};

const roleBtnActive: React.CSSProperties = {
  ...roleBtn,
  background: "#0B3D3A",
  color: "#F6F3EC",
  fontWeight: 600,
};

const fieldLabel: React.CSSProperties = {
  fontSize: "13px",
  fontWeight: 500,
  color: "#1B1B18",
};

const input: React.CSSProperties = {
  padding: "11px 13px",
  fontSize: "14.5px",
  border: "1px solid #DCD6C8",
  borderRadius: "7px",
  outline: "none",
  color: "#1B1B18",
  background: "#FEFDFB",
};

const submitBtn: React.CSSProperties = {
  marginTop: "6px",
  padding: "13px",
  fontSize: "15px",
  fontWeight: 600,
  color: "#F6F3EC",
  background: "#FF8A5B",
  border: "none",
  borderRadius: "8px",
  cursor: "pointer",
};

const errorText: React.CSSProperties = {
  fontSize: "13px",
  color: "#C0392B",
  margin: 0,
};

const footerText: React.CSSProperties = {
  fontSize: "13.5px",
  color: "#8A8577",
  textAlign: "center",
  marginTop: "22px",
};

const link: React.CSSProperties = {
  color: "#0B3D3A",
  fontWeight: 600,
  textDecoration: "none",
};