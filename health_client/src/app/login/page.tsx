"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState("");
  const [loading, setLoading] = useState(false);

  async function handleSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setLoading(true);

    try {
      const data = await apiFetch("/auth/login", {
        method: "POST",
        body: JSON.stringify({ email, password }),
      });

      // Save token + role for route protection across the app
      localStorage.setItem("token", data.token);
      localStorage.setItem("role", data.role);

      if (data.role === "DOCTOR") {
        router.push("/doctor/dashboard");
      } else {
        router.push("/patient/dashboard");
      }
    } catch (err) {
      setError("Invalid email or password.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <>    
    <div style={pageWrap}>    
      <button
        onClick={() => router.push("/")}
        className="absolute top-8 left-8 flex items-center gap-2 rounded-full border border-[#E8E3D9] bg-transparent px-5 py-2.5 text-[15px] font-medium text-[#FF8A5B] transition-all duration-200 hover:border-[#FF8A5B] hover:bg-[#FFF5F0] font-sans">
        <span className="text-base leading-none">←</span> Back
      </button>
      <div style={card}>
        <p style={eyebrow}>Meridian Clinic</p>
        <h1 style={heading}>Welcome back</h1>
        <p style={subtext}>Log in to view your appointments and records.</p>

        <form onSubmit={handleSubmit} style={{ display: "flex", flexDirection: "column", gap: "14px" }}>
          <label style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            <span style={fieldLabel}>Email</span>
            <input
              type="email"
              value={email}
              onChange={(e) => setEmail(e.target.value)}
              placeholder="you@example.com"
              required
              style={input}
            />
          </label>
          <label style={{ display: "flex", flexDirection: "column", gap: "6px" }}>
            <span style={fieldLabel}>Password</span>
            <input
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="Your password"
              required
              style={input}
            />
          </label>

          {error && <p style={errorText}>{error}</p>}

          <button type="submit" disabled={loading} style={submitBtn}>
            {loading ? "Logging in…" : "Log in"}
          </button>
        </form>

        <p style={footerText}>
          Don&apos;t have an account?{" "}
          <a href="/register" style={link}>
            Register
          </a>
        </p>
      </div>
    </div>
    </>
  );
}

// ---- styles (same token set as register page) ----
const pageWrap: React.CSSProperties = {
  minHeight: "100vh",
  background: "#F6F3EC",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  padding: "32px",
  fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
  position: "relative",
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