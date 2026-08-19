"use client";

import { useState, useEffect } from "react";
import { useRouter, useParams } from "next/navigation";
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

export default function ReportDetailPage() {
  const router = useRouter();
  const params = useParams();
  const reportId = params.id as string;
  const [report, setReport] = useState<LabReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    const fetchReport = async () => {
      try {
        const data = await apiFetch(`/api/reports/${reportId}`);
        setReport(data);
      } catch (err) {
        setError("Failed to load report");
        console.error(err);
      } finally {
        setLoading(false);
      }
    };
    fetchReport();
  }, [reportId]);

  if (loading) return <div style={loadingStyle}>Loading...</div>;
  if (error) return <div style={errorStyle}>{error}</div>;
  if (!report) return <div style={errorStyle}>Report not found</div>;

  return (
    <div style={pageWrap}>
      <button onClick={() => router.back()} style={backBtn}>
        ← Back
      </button>
      <div style={card}>
        <h2>{report.reportType} Report</h2>
        <div style={detailRow}><strong>Patient:</strong> {report.patientName}</div>
        <div style={detailRow}><strong>Email:</strong> {report.patientEmail}</div>
        <div style={detailRow}><strong>Status:</strong> {report.status}</div>
        <div style={detailRow}><strong>Uploaded:</strong> {new Date(report.uploadedAt).toLocaleString()}</div>
        {report.analysisResult && (
          <div style={analysisSection}>
            <h3>Analysis Result</h3>
            <p>{report.analysisResult}</p>
          </div>
        )}
      </div>
    </div>
  );
}

const pageWrap: React.CSSProperties = {
  padding: "32px",
  maxWidth: "800px",
  margin: "0 auto",
  minHeight: "100vh",
  background: "#F6F3EC",
};

const backBtn: React.CSSProperties = {
  padding: "8px 16px",
  borderRadius: "6px",
  border: "1px solid #DCD6C8",
  background: "white",
  cursor: "pointer",
  marginBottom: "20px",
};

const card: React.CSSProperties = {
  background: "white",
  padding: "32px",
  borderRadius: "12px",
  border: "1px solid #DCD6C8",
};

const detailRow: React.CSSProperties = {
  padding: "8px 0",
  borderBottom: "1px solid #eee",
};

const analysisSection: React.CSSProperties = {
  marginTop: "20px",
  padding: "16px",
  background: "#F6F3EC",
  borderRadius: "8px",
};

const loadingStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  minHeight: "100vh",
  fontSize: "18px",
  color: "#8A8577",
};

const errorStyle: React.CSSProperties = {
  display: "flex",
  justifyContent: "center",
  alignItems: "center",
  minHeight: "100vh",
  fontSize: "18px",
  color: "#C0392B",
};