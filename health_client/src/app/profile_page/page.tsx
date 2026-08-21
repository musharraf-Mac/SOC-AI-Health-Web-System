"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";

interface UserInfo {
  id: string;
  fullName: string;
  email: string;
  role: string;
}

export default function ProfilePage() {
  const router = useRouter();
  const [user, setUser] = useState<UserInfo | null>(null);
  const [showConfirm, setShowConfirm] = useState(false);
  const [deleting, setDeleting] = useState(false);

  useEffect(() => {
    const token = localStorage.getItem("token");
    if (!token) {
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

  function handleLogout() {
    localStorage.clear();
    router.push("/login");
  }

  async function handleDeleteAccount() {
    setDeleting(true);
    const token = localStorage.getItem("token");
    try {
      await apiFetch("/api/auth/delete-account", {
        method: "DELETE",
        headers: { Authorization: `Bearer ${token}` },
      });
      localStorage.clear();
      router.push("/");
    } catch {
      setDeleting(false);
      setShowConfirm(false);
    }
  }

  if (!user) return <div style={{ padding: 40, fontFamily: "'Inter', sans-serif" }}>Loading…</div>;

  return (
    <div style={page}>
      <div style={wrap}>
        <p style={eyebrow}>My Profile</p>
        <h1 style={heading}>{user.fullName}</h1>

        <div style={card}>
          <Row label="Full name" value={user.fullName} />
          <Row label="Email" value={user.email} />
          <Row label="Role" value={user.role} />
        </div>

        <div style={{ display: "flex", flexDirection: "column", gap: 12, marginTop: 24 }}>
          <button onClick={handleLogout} style={logoutBtn}>
            Log out
          </button>

          {!showConfirm ? (
            <button onClick={() => setShowConfirm(true)} style={deleteBtn}>
              Delete account
            </button>
          ) : (
            <div style={confirmBox}>
              <p style={{ fontSize: 14, color: "#C0392B", margin: "0 0 12px" }}>
                This permanently deletes your account. This can&apos;t be undone.
              </p>
              <div style={{ display: "flex", gap: 10 }}>
                <button onClick={handleDeleteAccount} disabled={deleting} style={deleteBtn}>
                  {deleting ? "Deleting…" : "Yes, delete permanently"}
                </button>
                <button onClick={() => setShowConfirm(false)} style={cancelBtn}>
                  Cancel
                </button>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

function Row({ label, value }: { label: string; value: string }) {
  return (
    <div style={{ display: "flex", justifyContent: "space-between", padding: "12px 0", borderBottom: "1px solid #EDE8DC" }}>
      <span style={{ fontSize: 13.5, color: "#8A8577" }}>{label}</span>
      <span style={{ fontSize: 14.5, color: "#1B1B18", fontWeight: 500 }}>{value}</span>
    </div>
  );
}

const page: React.CSSProperties = {
  minHeight: "100vh",
  background: "#F6F3EC",
  padding: "48px 32px",
  fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif",
};

const wrap: React.CSSProperties = { maxWidth: 480, margin: "0 auto" };

const eyebrow: React.CSSProperties = {
  fontSize: 12, letterSpacing: "0.1em", textTransform: "uppercase",
  color: "#FF8A5B", fontWeight: 700, margin: 0,
};

const heading: React.CSSProperties = {
  fontFamily: "'Fraunces', Georgia, serif", fontSize: 28, color: "#0B3D3A", margin: "8px 0 24px",
};

const card: React.CSSProperties = {
  background: "#FFF", border: "1px solid #DCD6C8", borderRadius: 10, padding: "8px 20px",
};

const logoutBtn: React.CSSProperties = {
  padding: 13, fontSize: 14.5, fontWeight: 600, color: "#0B3D3A",
  background: "transparent", border: "1.5px solid #0B3D3A", borderRadius: 8, cursor: "pointer",
};

const deleteBtn: React.CSSProperties = {
  padding: 13, fontSize: 14.5, fontWeight: 600, color: "#FFF",
  background: "#C0392B", border: "none", borderRadius: 8, cursor: "pointer",
};

const cancelBtn: React.CSSProperties = {
  padding: 13, fontSize: 14.5, fontWeight: 600, color: "#4A463E",
  background: "transparent", border: "1px solid #DCD6C8", borderRadius: 8, cursor: "pointer", flex: 1,
};

const confirmBox: React.CSSProperties = {
  padding: 16, background: "#FDF2F0", border: "1px solid #F0C4BC", borderRadius: 8,
};