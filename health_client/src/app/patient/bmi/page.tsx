"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";

interface BmiResult {
  bmi: number;
  category: string;
  advice: string;
}

interface BmiHistoryEntry {
  id: string;
  bmi: number;
  category: string;
  heightCm: number;
  weightKg: number;
  recordedAt: string;
}

export default function BmiPatientPage() {
  const router = useRouter();
  const [height, setHeight] = useState("");
  const [weight, setWeight] = useState("");
  const [result, setResult] = useState<BmiResult | null>(null);
  const [history, setHistory] = useState<BmiHistoryEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");
    if (!token || role !== "PATIENT") {
      router.push("/login");
      return;
    }
    loadHistory(token);
  }, [router]);

  function loadHistory(token: string) {
    apiFetch<BmiHistoryEntry[]>("/api/bmi/history", {
      headers: { Authorization: `Bearer ${token}` },
    })
      .then(setHistory)
      .catch(() => setHistory([]));
  }

  async function handleCalculate(e: React.FormEvent) {
    e.preventDefault();
    setError("");
    setResult(null);

    if (!height || !weight) {
      setError("Enter both height and weight.");
      return;
    }

    setLoading(true);
    const token = localStorage.getItem("token")!;
    try {
      // Authenticated endpoint — backend saves this under the user's email/id
      // and updates their "latest BMI" field.
      const data = await apiFetch<BmiResult>("/api/bmi/calculate-and-save", {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
        body: JSON.stringify({
          heightCm: Number(height),
          weightKg: Number(weight),
        }),
      });
      setResult(data);
      loadHistory(token); // refresh history after saving
    } catch {
      setError("Couldn't calculate BMI. Try again.");
    } finally {
      setLoading(false);
    }
  }

  return (
    <div style={page}>
      <div style={wrap}>
        <p style={eyebrow}>Your BMI Tracker</p>
        <h1 style={heading}>Check &amp; track your BMI</h1>
        <p style={subtext}>This entry will be saved to your health profile.</p>

        <div style={grid}>
          {/* Calculator */}
          <div style={card}>
            <h2 style={cardTitle}>New Entry</h2>
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

              {error && <p style={errorText}>{error}</p>}

              <button type="submit" disabled={loading} style={submitBtn}>
                {loading ? "Calculating…" : "Calculate & Save"}
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

          {/* History */}
          <div style={card}>
            <h2 style={cardTitle}>History</h2>
            {history.length === 0 ? (
              <p style={{ color: "#8A8577", fontSize: 14 }}>
                No entries yet — your first calculation will appear here.
              </p>
            ) : (
              <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
                {history.map((h) => (
                  <div key={h.id} style={historyRow}>
                    <div>
                      <p style={{ margin: 0, fontWeight: 600, color: "#1B1B18", fontSize: 14 }}>
                        BMI {h.bmi.toFixed(1)} — {h.category}
                      </p>
                      <p style={{ margin: "2px 0 0", fontSize: 12.5, color: "#8A8577" }}>
                        {h.heightCm}cm · {h.weightKg}kg ·{" "}
                        {new Date(h.recordedAt).toLocaleDateString()}
                      </p>
                    </div>
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

const page: React.CSSProperties = {
  minHeight: "100vh",
  background: "#F6F3EC",
  padding: "40px 32px",
  fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
};

const wrap: React.CSSProperties = { maxWidth: 860, margin: "0 auto" };

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

const grid: React.CSSProperties = {
  display: "grid",
  gridTemplateColumns: "1fr 1fr",
  gap: 24,
};

const card: React.CSSProperties = {
  background: "#FFF",
  border: "1px solid #DCD6C8",
  borderRadius: 10,
  padding: 24,
};

const cardTitle: React.CSSProperties = {
  fontFamily: "'Fraunces', Georgia, serif",
  fontSize: 18,
  color: "#0B3D3A",
  margin: "0 0 16px",
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
  marginTop: 22,
  paddingTop: 22,
  borderTop: "1px solid #DCD6C8",
  textAlign: "center",
};

const resultBmi: React.CSSProperties = {
  fontFamily: "'Fraunces', Georgia, serif",
  fontSize: 40,
  color: "#0B3D3A",
  margin: 0,
  fontWeight: 500,
};

const resultCategory: React.CSSProperties = {
  fontSize: 13.5,
  fontWeight: 600,
  color: "#FF8A5B",
  textTransform: "uppercase",
  letterSpacing: "0.05em",
  margin: "4px 0 10px",
};

const resultAdvice: React.CSSProperties = {
  fontSize: 14,
  color: "#4A463E",
  lineHeight: 1.5,
  margin: 0,
};

const historyRow: React.CSSProperties = {
  padding: "10px 12px",
  border: "1px solid #DCD6C8",
  borderRadius: 7,
};