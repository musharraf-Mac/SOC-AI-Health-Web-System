"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";

interface UserInfo {
  fullName: string;
  email: string;
  role: string;
}

export default function PatientDashboard() {
  const router = useRouter();
  const [user, setUser] = useState<UserInfo | null>(null);
  const [error, setError] = useState("");

  useEffect(() => {
    const token = localStorage.getItem("token");
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

  if (!user) return <div style={{ padding: 40 }}>Loading…</div>;

  return (
    <div style={{ minHeight: "100vh", background: "#F6F3EC", padding: "40px 32px", fontFamily: "'Inter', sans-serif" }}>
      <div style={{ maxWidth: 640, margin: "0 auto" }}>
        <p style={{ fontSize: 12, letterSpacing: "0.1em", textTransform: "uppercase", color: "#FF8A5B", fontWeight: 700 }}>
          Patient Dashboard
        </p>
        <h1 style={{ fontFamily: "'Fraunces', Georgia, serif", fontSize: 32, color: "#0B3D3A", margin: "8px 0 4px" }}>
          Welcome, {user.fullName}
        </h1>
        <p style={{ color: "#8A8577", fontSize: 14 }}>{user.email}</p>

        <div style={{ marginTop: 32, background: "#FFF", border: "1px solid #DCD6C8", borderRadius: 10, padding: 24 }}>
          <p style={{ margin: 0, color: "#4A463E" }}>Your appointments, records, and lab reports will appear here.</p>
        </div>
      </div>
    </div>
  );
}