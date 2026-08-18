"use client";

import { useState } from "react";
import { apiFetch } from "@/lib/api_bmi";

interface BmiResult {
  bmi: number;
  category: string;
  advice: string; // e.g. "Lose ~4.2 kg to reach a healthy BMI"
}

export default function BmiPublicPage() {
  const [height, setHeight] = useState(""); // cm
  const [weight, setWeight] = useState(""); // kg
  const [age, setAge] = useState(""); // years
  const [gender, setGender] = useState(""); // "male" | "female" | "other"
  const [result, setResult] = useState<BmiResult | null>(null);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  async function handleCalculate(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setResult(null);

    if (!height || !weight || !age || !gender) {
      setError("Enter height, weight, age, and gender.");
      return;
    }

    setLoading(true);
    try {
      // Public calculation-only endpoint — no auth, nothing saved to DB
      const data = await apiFetch<BmiResult>("/api/bmi/calculate", {
        method: "POST",
        body: JSON.stringify({
          height: Number(height),
          weight: Number(weight),
          age: Number(age),
          gender,
        }),
      });
      setResult(data);
    } catch {
      setError("Couldn't calculate BMI. Try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={page}>
      <div style={wrap}>
        <p style={eyebrow}>Free BMI Check</p>
        <h1 style={heading}>What&apos;s your BMI?</h1>
        <p style={subtext}>
          No account needed. Nothing is saved — just enter your details below.
        </p>

        <div style={card}>
          <form onSubmit={handleCalculate} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
            <label style={fieldWrap}>
              <span style={fieldLabel}>Height (cm)</span>
              <input
                type="number"
                value={height}
                onChange={(e) => setHeight(e.target.value)}
                placeholder="e.g. 170"
                style={input}
                min={50}
                max={250}
                required
              />
            </label>

            <label style={fieldWrap}>
              <span style={fieldLabel}>Weight (kg)</span>
              <input
                type="number"
                value={weight}
                onChange={(e) => setWeight(e.target.value)}
                placeholder="e.g. 65"
                style={input}
                min={10}
                max={400}
                required
              />
            </label>

            <label style={fieldWrap}>
              <span style={fieldLabel}>Age (years)</span>
              <input
                type="number"
                value={age}
                onChange={(e) => setAge(e.target.value)}
                placeholder="e.g. 28"
                style={input}
                min={1}
                max={120}
                required
              />
            </label>

            <label style={fieldWrap}>
              <span style={fieldLabel}>Gender</span>
              <select
                value={gender}
                onChange={(e) => setGender(e.target.value)}
                style={input}
                required
              >
                <option value="" disabled>
                  Select gender
                </option>
                <option value="male">Male</option>
                <option value="female">Female</option>
                <option value="other">Other</option>
              </select>
            </label>

            {error && <p style={errorText}>{error}</p>}

            <button type="submit" disabled={loading} style={submitBtn}>
              {loading ? "Calculating…" : "Calculate BMI"}
            </button>
          </form>

          {result && (
            <div style={resultBox}>
              <p style={resultBmi}>{result.bmi.toFixed(1)}</p>
              <p style={resultCategory}>{result.category}</p>
              <p style={resultAdvice}>{result.advice}</p>
            </div>
          )}
        </div>

        <p style={loginHint}>
          Want to track your BMI over time?{" "}
          <a href="/login" style={link}>
            Log in
          </a>{" "}
          to save your history.
        </p>
      </div>
    </div>
  );
}

const page: React.CSSProperties = {
  minHeight: "100vh",
  background: "#F6F3EC",
  padding: "56px 32px",
  fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
};

const wrap: React.CSSProperties = { maxWidth: 480, margin: "0 auto" };

const eyebrow: React.CSSProperties = {
  fontSize: 12,
  letterSpacing: "0.1em",
  textTransform: "uppercase",
  color: "#FF8A5B",
  fontWeight: 700,
  margin: 0,
  textAlign: "center",
};

const heading: React.CSSProperties = {
  fontFamily: "'Fraunces', Georgia, serif",
  fontSize: 30,
  color: "#0B3D3A",
  margin: "8px 0 4px",
  textAlign: "center",
};

const subtext: React.CSSProperties = {
  color: "#8A8577",
  fontSize: 14,
  textAlign: "center",
  margin: "0 0 28px",
};

const card: React.CSSProperties = {
  background: "#FFF",
  border: "1px solid #DCD6C8",
  borderRadius: 10,
  padding: 28,
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

const errorText: React.CSSProperties = { fontSize: 13, color: "#C0392B", margin: 0 };

const resultBox: React.CSSProperties = {
  marginTop: 24,
  paddingTop: 24,
  borderTop: "1px solid #DCD6C8",
  textAlign: "center",
};

const resultBmi: React.CSSProperties = {
  fontFamily: "'Fraunces', Georgia, serif",
  fontSize: 44,
  color: "#0B3D3A",
  margin: 0,
  fontWeight: 500,
};

const resultCategory: React.CSSProperties = {
  fontSize: 14,
  fontWeight: 600,
  color: "#FF8A5B",
  textTransform: "uppercase",
  letterSpacing: "0.05em",
  margin: "4px 0 12px",
};

const resultAdvice: React.CSSProperties = {
  fontSize: 14.5,
  color: "#4A463E",
  lineHeight: 1.5,
  margin: 0,
};

const loginHint: React.CSSProperties = {
  textAlign: "center",
  fontSize: 13.5,
  color: "#8A8577",
  marginTop: 20,
};

const link: React.CSSProperties = { color: "#0B3D3A", fontWeight: 600, textDecoration: "none" };