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
  age: number;
  gender: string;
  recordedAt: string;
}

export default function BmiPatientPage() {
  const router = useRouter();

  const [height, setHeight] = useState("");
  const [weight, setWeight] = useState("");
  const [age, setAge] = useState("");
  const [gender, setGender] = useState("");
  const [result, setResult] = useState<BmiResult | null>(null);
  const [history, setHistory] = useState<BmiHistoryEntry[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");
  const [deletingId, setDeletingId] = useState<string | null>(null);
  const [clearingAll, setClearingAll] = useState(false);

  // ============ Check Auth ============
  useEffect(() => {
    const token = localStorage.getItem("token");
    const role = localStorage.getItem("role");
    if (!token || role !== "PATIENT") {
      router.push("/login");
      return;
    }
    loadHistory();
  }, [router]);

  function authHeaders() {
    const token = localStorage.getItem("token");
    return { Authorization: `Bearer ${token}` };
  }

  // ============ Load History ============
  const loadHistory = async () => {
    try {
      const data = await apiFetch<BmiHistoryEntry[]>("/api/bmi/history", {
        headers: authHeaders(),
      });
      setHistory(data);
    } catch (err) {
      console.error("Error loading BMI history:", err);
      setHistory([]);
    }
  };

  // ============ Calculate BMI ============
  const handleCalculate = async (e: React.FormEvent) => {
    e.preventDefault();
    setError("");
    setResult(null);

    if (!height || !weight || !age || !gender) {
      setError("Please fill in all fields.");
      return;
    }

    setLoading(true);
    try {
      const data = await apiFetch<BmiResult>("/api/bmi/calculate/save", {
        method: "POST",
        headers: authHeaders(),
        body: JSON.stringify({
          height: Number(height),
          weight: Number(weight),
          age: Number(age),
          gender: gender,
        }),
      });

      setResult(data);
      await loadHistory();

      setHeight("");
      setWeight("");
      setAge("");
      setGender("");
    } catch (err) {
      console.error("BMI calculation error:", err);
      setError("Couldn't calculate BMI. Please try again.");
    } finally {
      setLoading(false);
    }
  };

  // ============ Delete one entry ============
  const handleDeleteEntry = async (id: string) => {
    setDeletingId(id);
    try {
      await apiFetch(`/api/bmi/history/${id}`, {
        method: "DELETE",
        headers: authHeaders(),
      });
      setHistory((prev) => prev.filter((h) => h.id !== id));
    } catch (err) {
      console.error("Failed to delete entry:", err);
    } finally {
      setDeletingId(null);
    }
  };

  // ============ Delete all history ============
  const handleClearAll = async () => {
    setClearingAll(true);
    try {
      await apiFetch("/api/bmi/history", {
        method: "DELETE",
        headers: authHeaders(),
      });
      setHistory([]);
    } catch (err) {
      console.error("Failed to clear history:", err);
    } finally {
      setClearingAll(false);
    }
  };

  const getBmiColor = (category: string) => {
    const colors: Record<string, string> = {
      "Underweight": "#FF8A5B",
      "Normal weight": "#1E7E34",
      "Overweight": "#FFC107",
      "Obese": "#C0392B",
    };
    return colors[category] || "#0B3D3A";
  };

  return (
    <div style={page}>
      <button onClick={() => router.push("/patient/dashboard")} style={backBtn}>
        ← Back to Dashboard
      </button>

      <div style={wrap}>
        <p style={eyebrow}>Your BMI Tracker</p>
        <h1 style={heading}>Check &amp; track your BMI</h1>
        <p style={subtext}>This entry will be saved to your health profile.</p>

        <div style={grid}>
          {/* ===== Calculator ===== */}
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
                  min={2}
                  max={300}
                  required
                />
              </label>

              <label style={fieldWrap}>
                <span style={fieldLabel}>Age (Years)</span>
                <input
                  type="number"
                  value={age}
                  onChange={(e) => setAge(e.target.value)}
                  placeholder="e.g. 30"
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
                  <option value="">Select</option>
                  <option value="male">Male</option>
                  <option value="female">Female</option>
                  <option value="other">Other</option>
                </select>
              </label>

              {error && <p style={errorText}>{error}</p>}

              <button type="submit" disabled={loading} style={submitBtn}>
                {loading ? "Calculating…" : "Calculate & Save"}
              </button>
            </form>

            {result && (
              <div style={resultBox}>
                <p style={{ ...resultBmi, color: getBmiColor(result.category) }}>
                  {result.bmi.toFixed(1)}
                </p>
                <p style={{ ...resultCategory, color: getBmiColor(result.category) }}>
                  {result.category}
                </p>
                <p style={resultAdvice}>{result.advice}</p>
              </div>
            )}
          </div>

          {/* ===== History ===== */}
          <div style={card}>
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 16 }}>
              <h2 style={{ ...cardTitle, margin: 0 }}>History</h2>
              {history.length > 0 && (
                <button onClick={handleClearAll} disabled={clearingAll} style={clearAllBtn}>
                  {clearingAll ? "Clearing…" : "Clear all"}
                </button>
              )}
            </div>

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
                        {h.heightCm}cm · {h.weightKg}kg · Age {h.age} · {h.gender}
                      </p>
                      <p style={{ margin: "2px 0 0", fontSize: 12, color: "#aaa" }}>
                        {new Date(h.recordedAt).toLocaleDateString()} at{" "}
                        {new Date(h.recordedAt).toLocaleTimeString()}
                      </p>
                    </div>
                    <button
                      onClick={() => handleDeleteEntry(h.id)}
                      disabled={deletingId === h.id}
                      style={deleteEntryBtn}
                      title="Delete this entry"
                    >
                      {deletingId === h.id ? "…" : "✕"}
                    </button>
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

// ============ Styles ============
const page: React.CSSProperties = {
  minHeight: "100vh",
  background: "#F6F3EC",
  padding: "40px 32px",
  fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
};

const backBtn: React.CSSProperties = {
  padding: "10px 20px",
  borderRadius: 8,
  border: "1px solid #DCD6C8",
  background: "#FFF",
  cursor: "pointer",
  fontSize: 14,
  fontWeight: 500,
  color: "#0B3D3A",
  marginBottom: 24,
  maxWidth: "200px",
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
  margin: 0,
  fontWeight: 500,
};

const resultCategory: React.CSSProperties = {
  fontSize: 13.5,
  fontWeight: 600,
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
  display: "flex",
  justifyContent: "space-between",
  alignItems: "flex-start",
  padding: "10px 12px",
  border: "1px solid #DCD6C8",
  borderRadius: 7,
};

const deleteEntryBtn: React.CSSProperties = {
  fontSize: 13,
  color: "#C0392B",
  background: "transparent",
  border: "1px solid #EAD4D0",
  borderRadius: 6,
  width: 26,
  height: 26,
  cursor: "pointer",
  lineHeight: 1,
};

const clearAllBtn: React.CSSProperties = {
  fontSize: 12.5,
  color: "#C0392B",
  background: "transparent",
  border: "1px solid #EAD4D0",
  borderRadius: 6,
  padding: "5px 10px",
  cursor: "pointer",
};