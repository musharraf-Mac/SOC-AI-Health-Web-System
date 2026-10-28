"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "@/lib/api";

interface LabReport {
  id: string;
  patientName: string;
  patientEmail: string;
  reportType: string;
  fileId: string;
  status: string;
  uploadedAt: string;
  analysisResult?: string;
}

export default function LabStaffDashboard() {
  const router = useRouter();
  const [reports, setReports] = useState<LabReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [deleteLoading, setDeleteLoading] = useState<string | null>(null);
  const [showDeleteModal, setShowDeleteModal] = useState(false);
  const [selectedReport, setSelectedReport] = useState<LabReport | null>(null);
  
  // Upload state
  const [showUploadModal, setShowUploadModal] = useState(false);
  const [uploadLoading, setUploadLoading] = useState(false);
  const [uploadForm, setUploadForm] = useState({
    patientName: "",
    patientEmail: "",
    reportType: "",
    file: null as File | null,
  });

  const fetchReports = async () => {
    setLoading(true);
    setError("");
    try {
      const data = await apiFetch<LabReport[]>("/api/reports/view");
      setReports(data);
    } catch (err) {
      setError("Failed to load reports. Please try again.");
      console.error("Error fetching reports:", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchReports();
  }, []);
  

  // Handle Upload
  const handleUpload = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!uploadForm.file) {
      setError("Please select a file");
      return;
    }

    setUploadLoading(true);
    setError("");

    const formData = new FormData();
    formData.append("patientName", uploadForm.patientName);
    formData.append("patientEmail", uploadForm.patientEmail);
    formData.append("reportType", uploadForm.reportType);
    formData.append("file", uploadForm.file);

    try {
      const response = await apiFetch("/api/reports/upload", {
        method: "POST",
        body: formData
        
      });
      console.log("Upload response:", response);
      //close modal and reset form
      setShowUploadModal(false);
      setUploadForm({ patientName: "", patientEmail: "", reportType: "", file: null });
      fetchReports(); // Refresh the reports list
    } catch (err) {
      setError("Failed to upload report. Please try again.");
      console.error("Upload error:", err);
    } finally {
      setUploadLoading(false);
    }
  };

  const handleDelete = async (reportId: string) => {
    setDeleteLoading(reportId);
    try {
      await apiFetch(`/api/reports/delete/${reportId}`, {
        method: "DELETE",
      });
      setReports(reports.filter((r) => r.id !== reportId));
      setShowDeleteModal(false);
      setSelectedReport(null);
    } catch (err) {
      setError("Failed to delete report. Please try again.");
      console.error("Error deleting report:", err);
    } finally {
      setDeleteLoading(null);
    }
  };

  const handleLogout = async () => {
    try {
      await apiFetch("/api/auth/logout", {
        method: "POST",
      });
    } catch (err) {
      console.error("Logout error:", err);
    } finally {
      localStorage.removeItem("role");
      localStorage.removeItem("token");
      router.push("/login");
    }
  };

  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleDateString("en-US", {
      year: "numeric",
      month: "short",
      day: "numeric",
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  const getStatusBadge = (status: string) => {
    const styles: Record<string, { bg: string; color: string }> = {
      PENDING: { bg: "#FFF3E0", color: "#E65100" },
      PROCESSED: { bg: "#E3F2FD", color: "#0D47A1" },
      COMPLETED: { bg: "#E8F5E9", color: "#1B5E20" },
    };
    const style = styles[status] || { bg: "#F5F5F5", color: "#616161" };
    return (
      <span
        style={{
          padding: "4px 12px",
          borderRadius: "12px",
          background: style.bg,
          color: style.color,
          fontSize: "12px",
          fontWeight: 600,
        }}
      >
        {status}
      </span>
    );
  };

  return (
    <div style={pageWrap}>
      {/* Header */}
      <header style={header}>
        <div style={headerLeft}>
          <h1 style={headerTitle}>Lab Reports Dashboard</h1>
          <span style={reportCount}>{reports.length} reports</span>
        </div>
        <div style={headerRight}>
          {/* UPLOAD BUTTON */}
          <button
            onClick={() => setShowUploadModal(true)}
            style={uploadBtn}
          >
            <span>📤</span> Upload Report
          </button>

          {/* Profile Button */}
          <button
            onClick={() => router.push("../../profile_page")}
            style={profileBtn}
            title="Profile"
          >
            <span style={profileIcon}>👤</span>
          </button>
          
          {/* Logout Button */}
          <button onClick={handleLogout} style={logoutBtn}>
            <span>🚪</span> Logout
          </button>
        </div>
      </header>

      {/* Main Content */}
      <div style={content}>
        {/* Toolbar */}
        <div style={toolbar}>
          <button onClick={fetchReports} style={refreshBtn}>
            🔄 Refresh
          </button>
        </div>

        {/* Error Message */}
        {error && (
          <div style={errorBanner}>
            <span>⚠️</span> {error}
            <button onClick={() => setError("")} style={closeBtn}>
              ×
            </button>
          </div>
        )}

        {/* Reports Table */}
        {loading ? (
          <div style={loadingContainer}>
            <div style={spinner}></div>
            <p>Loading reports...</p>
          </div>
        ) : reports.length === 0 ? (
          <div style={emptyState}>
            <span style={emptyIcon}>📄</span>
            <h3>No reports found</h3>
            <p>Uploaded lab reports will appear here.</p>
          </div>
        ) : (
          <div style={tableContainer}>
            <table style={table}>
              <thead style={{ color: "black" }}>
                <tr>
                  <th className="font-bold text-black">Patient</th>
                  <th className="text-black font-bold">Email</th>
                  <th className="text-black font-bold">Report Type</th>
                  <th className="text-black font-bold">Status</th>
                  <th className="text-black font-bold">Uploaded</th>
                  <th className="text-black font-bold">Actions</th>
                </tr>
              </thead>
              <tbody>
                {reports.map((report) => (
                  <tr key={report.id}>
                    <td style={tdName}>{report.patientName}</td>
                    <td style={tdEmail}>{report.patientEmail}</td>
                    <td>
                      <span style={reportTypeBadge}>{report.reportType}</span>
                    </td>
                    <td>{getStatusBadge(report.status)}</td>
                    <td>{formatDate(report.uploadedAt)}</td>
                    <td>
                      <div style={actionButtons}>
                        <button
                          onClick={() => router.push(`/lab_staff/reports/${report.fileId}`)}
                          style={viewBtn}
                          title="View Report"
                        >
                          👁️
                        </button>
                        <button
                          onClick={() => {
                            setSelectedReport(report);
                            setShowDeleteModal(true);
                          }}
                          style={deleteBtn}
                          title="Delete Report"
                          disabled={deleteLoading === report.id}
                        >
                          {deleteLoading === report.id ? "..." : "🗑️"}
                        </button>
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* UPLOAD MODAL */}
      {showUploadModal && (
        <div style={modalOverlay}>
          <div style={modal}>
            <h3 style={modalTitle}>📤 Upload Lab Report</h3>
            <form onSubmit={handleUpload}>
              <div style={formGroup}>
                <label style={formLabel}>Patient Name *</label>
                <input
                  type="text"
                  value={uploadForm.patientName}
                  onChange={(e) => setUploadForm({ ...uploadForm, patientName: e.target.value })}
                  style={formInput}
                  placeholder="Enter patient full name"
                  required
                />
              </div>

              <div style={formGroup}>
                <label style={formLabel}>Patient Email *</label>
                <input
                  type="email"
                  value={uploadForm.patientEmail}
                  onChange={(e) => setUploadForm({ ...uploadForm, patientEmail: e.target.value })}
                  style={formInput}
                  placeholder="patient@example.com"
                  required
                />
              </div>

              <div style={formGroup}>
                <label style={formLabel}>Report Type *</label>
                <select
                  value={uploadForm.reportType}
                  onChange={(e) => setUploadForm({ ...uploadForm, reportType: e.target.value })}
                  style={formInput}
                  required
                >
                  <option value="">Select report type</option>
                  <option value="BLOOD_TEST">Blood Test</option>
                  <option value="URINE_TEST">Urine Test</option>
                  <option value="X_RAY">X-Ray</option>
                  <option value="MRI">MRI</option>
                  <option value="CT_SCAN">CT Scan</option>
                  <option value="ULTRASOUND">Ultrasound</option>
                  <option value="PATHOLOGY">Pathology</option>
                  <option value="OTHER">Other</option>
                </select>
              </div>

              <div style={formGroup}>
                <label style={formLabel}>Upload PDF File *</label>
                <input
                  type="file"
                  accept=".pdf,application/pdf"
                  onChange={(e) => {
                    const file = e.target.files?.[0] || null;
                    setUploadForm({ ...uploadForm, file });
                  }}
                  style={fileInput}
                  required
                />
                {uploadForm.file && (
                  <span style={fileName}>{uploadForm.file.name}</span>
                )}
              </div>

              {error && <p style={modalError}>{error}</p>}

              <div style={modalActions}>
                <button
                  type="button"
                  onClick={() => {
                    setShowUploadModal(false);
                    setUploadForm({ patientName: "", patientEmail: "", reportType: "", file: null });
                    setError("");
                  }}
                  style={modalCancelBtn}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={uploadLoading}
                  style={modalUploadBtn}
                >
                  {uploadLoading ? "Uploading..." : "📤 Upload Report"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Delete Confirmation Modal */}
      {showDeleteModal && selectedReport && (
        <div style={modalOverlay}>
          <div style={modal}>
            <h3>Confirm Delete</h3>
            <p>
              Are you sure you want to delete the report for{" "}
              <strong>{selectedReport.patientName}</strong>?
            </p>
            <p style={modalSubtext}>This action cannot be undone.</p>
            <div style={modalActions}>
              <button
                onClick={() => {
                  setShowDeleteModal(false);
                  setSelectedReport(null);
                }}
                style={modalCancelBtn}
              >
                Cancel
              </button>
              <button
                onClick={() => handleDelete(selectedReport.id)}
                style={modalDeleteBtn}
              >
                Delete Report
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}

// ============ STYLES ============

const pageWrap: React.CSSProperties = {
  minHeight: "100vh",
  background: "#F6F3EC",
  fontFamily: "'Inter', -apple-system, BlinkMacSystemFont, sans-serif",
};

const header: React.CSSProperties = {
  background: "#FFFFFF",
  padding: "16px 32px",
  borderBottom: "1px solid #DCD6C8",
  display: "flex",
  justifyContent: "space-between",
  alignItems: "center",
  position: "sticky",
  top: 0,
  zIndex: 100,
};

const headerLeft: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "16px",
};

const headerTitle: React.CSSProperties = {
  fontFamily: "'Fraunces', Georgia, serif",
  fontSize: "24px",
  fontWeight: 600,
  color: "#0B3D3A",
  margin: 0,
};

const reportCount: React.CSSProperties = {
  fontSize: "14px",
  color: "#8A8577",
  background: "#F6F3EC",
  padding: "4px 12px",
  borderRadius: "12px",
};

const headerRight: React.CSSProperties = {
  display: "flex",
  alignItems: "center",
  gap: "12px",
};

const uploadBtn: React.CSSProperties = {
  padding: "10px 20px",
  borderRadius: "8px",
  background: "#0B3D3A",
  color: "#FFFFFF",
  border: "none",
  cursor: "pointer",
  fontSize: "14px",
  fontWeight: 600,
  display: "flex",
  alignItems: "center",
  gap: "8px",
  transition: "background 0.2s",
};

const profileBtn: React.CSSProperties = {
  width: "44px",
  height: "44px",
  borderRadius: "50%",
  background: "#F6F3EC",
  border: "2px solid #DCD6C8",
  cursor: "pointer",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  fontSize: "20px",
  transition: "all 0.2s",
  padding: 0,
};

const profileIcon: React.CSSProperties = {
  fontSize: "20px",
  lineHeight: 1,
};

const logoutBtn: React.CSSProperties = {
  padding: "10px 20px",
  borderRadius: "8px",
  background: "#FF8A5B",
  color: "#FFFFFF",
  border: "none",
  cursor: "pointer",
  fontSize: "14px",
  fontWeight: 600,
  display: "flex",
  alignItems: "center",
  gap: "8px",
  transition: "background 0.2s",
};

const content: React.CSSProperties = {
  padding: "32px",
  maxWidth: "1400px",
  margin: "0 auto",
};

const toolbar: React.CSSProperties = {
  display: "flex",
  justifyContent: "flex-end",
  marginBottom: "20px",
};

const refreshBtn: React.CSSProperties = {
  padding: "8px 16px",
  borderRadius: "6px",
  background: "transparent",
  border: "1px solid #DCD6C8",
  cursor: "pointer",
  fontSize: "14px",
  color: "#4A463E",
  transition: "background 0.2s",
};

const errorBanner: React.CSSProperties = {
  background: "#FDE8E8",
  color: "#C0392B",
  padding: "12px 20px",
  borderRadius: "8px",
  marginBottom: "20px",
  display: "flex",
  alignItems: "center",
  gap: "12px",
  fontSize: "14px",
};

const closeBtn: React.CSSProperties = {
  background: "none",
  border: "none",
  fontSize: "20px",
  cursor: "pointer",
  color: "#C0392B",
  marginLeft: "auto",
};

const loadingContainer: React.CSSProperties = {
  display: "flex",
  flexDirection: "column",
  alignItems: "center",
  justifyContent: "center",
  padding: "60px",
  color: "#8A8577",
};

const spinner: React.CSSProperties = {
  width: "40px",
  height: "40px",
  border: "4px solid #DCD6C8",
  borderTop: "4px solid #FF8A5B",
  borderRadius: "50%",
  animation: "spin 1s linear infinite",
};

const emptyState: React.CSSProperties = {
  textAlign: "center",
  padding: "80px 20px",
  color: "#8A8577",
};

const emptyIcon: React.CSSProperties = {
  fontSize: "48px",
  display: "block",
  marginBottom: "16px",
};

const tableContainer: React.CSSProperties = {
  background: "#FFFFFF",
  borderRadius: "12px",
  border: "1px solid #DCD6C8",
  overflow: "hidden",
  color:"black"
};

const table: React.CSSProperties = {
  width: "100%",
  borderCollapse: "collapse",
  color:"black"
};

const tdName: React.CSSProperties = {
  fontWeight: 500,
  color: "#0B3D3A",
};

const tdEmail: React.CSSProperties = {
  color: "#8A8577",
  fontSize: "14px",
};

const reportTypeBadge: React.CSSProperties = {
  padding: "4px 12px",
  borderRadius: "12px",
  background: "#E8F0FE",
  color: "#1A56DB",
  fontSize: "12px",
  fontWeight: 600,
};

const actionButtons: React.CSSProperties = {
  display: "flex",
  gap: "8px",
};

const viewBtn: React.CSSProperties = {
  padding: "6px 10px",
  borderRadius: "6px",
  border: "none",
  background: "#E3F2FD",
  cursor: "pointer",
  fontSize: "16px",
  transition: "background 0.2s",
};

const deleteBtn: React.CSSProperties = {
  padding: "6px 10px",
  borderRadius: "6px",
  border: "none",
  background: "#FDE8E8",
  cursor: "pointer",
  fontSize: "16px",
  transition: "background 0.2s",
};

// Modal Styles
const modalOverlay: React.CSSProperties = {
  position: "fixed",
  top: 0,
  left: 0,
  right: 0,
  bottom: 0,
  background: "rgba(0, 0, 0, 0.5)",
  display: "flex",
  alignItems: "center",
  justifyContent: "center",
  zIndex: 1000,
};

const modal: React.CSSProperties = {
  background: "#FFFFFF",
  borderRadius: "12px",
  padding: "32px",
  maxWidth: "480px",
  width: "100%",
  boxShadow: "0 20px 60px rgba(0,0,0,0.2)",
  maxHeight: "90vh",
  overflowY: "auto",
};

const modalTitle: React.CSSProperties = {
  fontSize: "20px",
  fontWeight: 600,
  color: "#0B3D3A",
  marginTop: 0,
  marginBottom: "20px",
};

const formGroup: React.CSSProperties = {
  marginBottom: "16px",
};

const formLabel: React.CSSProperties = {
  display: "block",
  fontSize: "14px",
  fontWeight: 500,
  color: "#1B1B18",
  marginBottom: "4px",
};

const formInput: React.CSSProperties = {
  width: "100%",
  padding: "10px 12px",
  fontSize: "14px",
  border: "1px solid #DCD6C8",
  borderRadius: "8px",
  outline: "none",
  background: "#FEFDFB",
  color: "#1B1B18",
  fontFamily: "inherit",
};

const fileInput: React.CSSProperties = {
  width: "100%",
  padding: "10px 12px",
  fontSize: "14px",
  border: "1px solid #DCD6C8",
  borderRadius: "8px",
  background: "#FEFDFB",
  cursor: "pointer",
};

const fileName: React.CSSProperties = {
  display: "block",
  fontSize: "13px",
  color: "#0B3D3A",
  marginTop: "4px",
  fontWeight: 500,
};

const modalError: React.CSSProperties = {
  color: "#C0392B",
  fontSize: "14px",
  marginBottom: "12px",
};

const modalSubtext: React.CSSProperties = {
  fontSize: "14px",
  color: "#8A8577",
  marginTop: "4px",
};

const modalActions: React.CSSProperties = {
  display: "flex",
  gap: "12px",
  marginTop: "24px",
  justifyContent: "flex-end",
};

const modalCancelBtn: React.CSSProperties = {
  padding: "10px 24px",
  borderRadius: "8px",
  border: "1px solid #DCD6C8",
  background: "transparent",
  cursor: "pointer",
  fontSize: "14px",
  fontWeight: 500,
  color: "#4A463E",
};

const modalDeleteBtn: React.CSSProperties = {
  padding: "10px 24px",
  borderRadius: "8px",
  border: "none",
  background: "#C0392B",
  color: "#FFFFFF",
  cursor: "pointer",
  fontSize: "14px",
  fontWeight: 600,
};

const modalUploadBtn: React.CSSProperties = {
  padding: "10px 24px",
  borderRadius: "8px",
  border: "none",
  background: "#0B3D3A",
  color: "#FFFFFF",
  cursor: "pointer",
  fontSize: "14px",
  fontWeight: 600,
};