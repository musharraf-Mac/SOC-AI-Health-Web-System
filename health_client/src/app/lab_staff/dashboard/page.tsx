"use client";

import { useCallback, useEffect, useRef, useState, type ReactNode } from "react";
import { useRouter } from "next/navigation";
import { apiFetch, reportFileUrl } from "@/lib/api";
import ExplanationDialog, { type ExplanationReport } from "./explanation-dialog";
import styles from "./dashboard.module.css";

interface LabReport extends ExplanationReport {
  patientEmail: string;
  uploadedAt: string;
}
const reportTypes = ["BLOOD_TEST", "URINE_TEST", "X_RAY", "MRI", "CT_SCAN", "ULTRASOUND", "PATHOLOGY", "OTHER"];
const label = (value: string) => value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, c => c.toUpperCase());
const isProcessing = (status: string) => ["PENDING", "PROCESSING", "PROCESSED"].includes(status);
const statusLabel = (status: string) => status === "COMPLETED" ? "Ready" : status === "FAILED" ? "Needs attention" : label(status);
const dateLabel = (value: string) => {
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Date unavailable" : date.toLocaleDateString(undefined, { day: "numeric", month: "short", year: "numeric" });
};

function Modal({ title, onClose, busy, children }: { title: string; onClose: () => void; busy?: boolean; children: ReactNode }) {
  const ref = useRef<HTMLDialogElement>(null);
  useEffect(() => {
    const dialog = ref.current;
    const opener = document.activeElement instanceof HTMLElement ? document.activeElement : null;
    const previous = document.body.style.overflow;
    dialog?.showModal();
    document.body.style.overflow = "hidden";
    return () => {
      dialog?.close();
      document.body.style.overflow = previous;
      if (opener?.isConnected) opener.focus();
    };
  }, []);
  const close = () => { if (!busy) { ref.current?.close(); onClose(); } };
  return <dialog ref={ref} className={styles.modal} aria-labelledby="action-dialog-title" onCancel={event => { event.preventDefault(); close(); }}>
    <div className={styles.modalHeader}>
      <h2 id="action-dialog-title">{title}</h2>
      <button type="button" className={styles.close} aria-label={`Close ${title.toLowerCase()}`} disabled={busy} onClick={close}>×</button>
    </div>
    {children}
  </dialog>;
}

export default function LabStaffDashboard() {
  const router = useRouter();
  const [reports, setReports] = useState<LabReport[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState("");
  const [filter, setFilter] = useState("ALL");
  const [explanationReport, setExplanationReport] = useState<LabReport | null>(null);
  const [openingPdf, setOpeningPdf] = useState<string | null>(null);
  const [deleteReport, setDeleteReport] = useState<LabReport | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [deleteError, setDeleteError] = useState("");
  const [showUpload, setShowUpload] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [uploadError, setUploadError] = useState("");
  const [notice, setNotice] = useState("");

  const loadReports = useCallback((signal?: AbortSignal) => {
    return apiFetch<LabReport[]>("/api/reports/view?includeExplanation=false", { signal, cache: "no-store" })
      .then(data => { if (!signal?.aborted) { setReports(data); setError(""); } })
      .catch(() => { if (!signal?.aborted) setError("Reports could not be loaded. Please refresh to try again."); })
      .finally(() => { if (!signal?.aborted) setLoading(false); });
  }, []);
  useEffect(() => {
    const controller = new AbortController();
    void loadReports(controller.signal);
    return () => controller.abort();
  }, [loadReports]);
  const refresh = () => { setLoading(true); void loadReports(); };

  const openPdf = async (report: LabReport) => {
    const tab = window.open("", "_blank");
    if (!tab) { setError("Please allow popups for this site to open the report PDF."); return; }
    tab.opener = null;
    tab.document.title = "Loading report PDF";
    tab.document.body.textContent = "Loading report PDF...";
    setOpeningPdf(report.id);
    setError("");
    try {
      const url = await reportFileUrl(report.id);
      if (tab.closed) { URL.revokeObjectURL(url); return; }
      tab.location.replace(url);
      window.setTimeout(() => URL.revokeObjectURL(url), 120000);
    } catch (failure) {
      tab.close();
      setError(failure instanceof Error ? failure.message : "Unable to open this PDF. Please try again.");
    } finally { setOpeningPdf(null); }
  };
  const removeReport = async () => {
    if (!deleteReport || deleting) return;
    setDeleting(true);
    setDeleteError("");
    try {
      await apiFetch(`/api/reports/delete/${encodeURIComponent(deleteReport.id)}`, { method: "DELETE" });
      setReports(current => current.filter(report => report.id !== deleteReport.id));
      setDeleteReport(null);
      setNotice("Report deleted successfully.");
    } catch { setDeleteError("Unable to delete this report. Please try again."); }
    finally { setDeleting(false); }
  };
  const uploadReport = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const file = form.get("file");
    if (!(file instanceof File) || !file.name.toLowerCase().endsWith(".pdf") || !file.size || file.size > 20 * 1024 * 1024) {
      setUploadError("Select a PDF file between 1 byte and 20 MB."); return;
    }
    setUploading(true);
    setUploadError("");
    try {
      const result = await apiFetch<{ status: string }>("/api/reports/upload", { method: "POST", body: form });
      setShowUpload(false);
      setNotice(result.status === "FAILED" ? "Report saved. Its explanation needs attention." : "Report uploaded successfully.");
      refresh();
    } catch (failure) { setUploadError(failure instanceof Error ? failure.message : "Upload failed. Please try again."); }
    finally { setUploading(false); }
  };
  const logout = async () => {
    try { await apiFetch("/api/auth/logout", { method: "POST" }); }
    finally { localStorage.removeItem("role"); localStorage.removeItem("token"); router.push("/login"); }
  };

  const query = search.trim().toLowerCase();
  const filtered = reports.filter(report => {
    const matchesStatus = filter === "ALL" || (filter === "PROCESSING" ? isProcessing(report.status) : report.status === filter);
    return matchesStatus && [report.patientName, report.patientEmail, report.fileName, report.id, report.reportType, label(report.reportType)]
      .some(value => value?.toLowerCase().includes(query));
  }).sort((a, b) => b.uploadedAt.localeCompare(a.uploadedAt));

  return <div className={styles.page}>
    <main className={styles.main}>
      <header className={styles.heading}>
        <div>
          <p className={styles.eyebrow}>Lab Staff Dashboard</p>
          <h1>Lab reports</h1>
          <p className={styles.subtitle}>View patient reports and their explanations.</p>
        </div>
        <div className={styles.account}>
          <button className={styles.primary} onClick={() => { setUploadError(""); setShowUpload(true); }}><span aria-hidden="true">📤</span> Upload report</button>
          <button className={styles.profileButton} title="Profile" aria-label="Profile" onClick={() => router.push("/profile_page")}><span aria-hidden="true">👤</span></button>
          <button className={styles.logoutButton} onClick={() => { void logout().catch(() => {}); }}><span aria-hidden="true">🚪</span> Logout</button>
        </div>
      </header>
      {notice && <div role="status" className={styles.notice}>{notice}<button aria-label="Dismiss notification" onClick={() => setNotice("")}>×</button></div>}
      {error && <div role="alert" className={styles.error}>{error}</div>}
      <section className={styles.panel} aria-label="Reports">
        <div className={styles.toolbar}>
          <label className={styles.search}><span className={styles.srOnly}>Search reports</span><svg aria-hidden="true" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="1.7"><circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/></svg><input type="search" placeholder="Search patient, email or report…" value={search} onChange={event => setSearch(event.target.value)} /></label>
          <label className={styles.filter}><span className={styles.srOnly}>Filter by status</span><select value={filter} onChange={event => setFilter(event.target.value)}><option value="ALL">All statuses</option><option value="COMPLETED">Ready</option><option value="PROCESSING">In progress</option><option value="FAILED">Needs attention</option></select></label>
          <button className={styles.secondary} disabled={loading} onClick={refresh}>{loading ? "Refreshing…" : "Refresh"}</button>
        </div>
        <div className={styles.listInfo} aria-live="polite">{loading ? "Loading reports…" : `${filtered.length} of ${reports.length} reports`}<span>Newest first</span></div>
        {loading && !reports.length ? <div role="status" className={styles.empty}>Loading your lab reports…</div> : !filtered.length ? <div className={styles.empty}><h2>{error ? "Reports unavailable" : reports.length ? "No matching reports" : "No reports yet"}</h2><p>{error ? "Use Refresh to try again." : reports.length ? "Try a different search or status filter." : "Upload a PDF report to get started."}</p>{reports.length > 0 && <button className={styles.secondary} onClick={() => { setSearch(""); setFilter("ALL"); }}>Clear filters</button>}</div> : <div className={styles.tableWrap}>
          <table className={styles.table}>
            <thead><tr><th>Report</th><th>Patient</th><th>Status</th><th>Uploaded</th><th className={styles.actionHeading}>Actions</th></tr></thead>
            <tbody>{filtered.map(report => <tr key={report.id}>
              <td data-label="Report"><div className={styles.reportCell}><span className={styles.fileIcon} aria-hidden="true">PDF</span><div><strong className={styles.fileName}>{report.fileName || `${label(report.reportType)} report`}</strong><small>{label(report.reportType)}</small></div></div></td>
              <td data-label="Patient"><strong className={styles.patientName}>{report.patientName}</strong><small className={styles.email}>{report.patientEmail}</small></td>
              <td data-label="Status"><span className={`${styles.status} ${report.status === "COMPLETED" ? styles.ready : report.status === "FAILED" ? styles.failed : styles.pending}`}><span aria-hidden="true" />{statusLabel(report.status)}</span></td>
              <td data-label="Uploaded"><time dateTime={report.uploadedAt} className={styles.date}>{dateLabel(report.uploadedAt)}</time></td>
              <td data-label="Actions"><div className={styles.actions}>
                <button className={styles.pdfButton} disabled={openingPdf !== null} onClick={() => openPdf(report)} title="Open PDF in a new tab">{openingPdf === report.id ? "Opening…" : "View PDF ↗"}</button>
                <button className={styles.explanationButton} onClick={() => setExplanationReport(report)}>View explanation</button>
                <button className={styles.deleteButton} onClick={() => { setDeleteError(""); setDeleteReport(report); }}>Delete</button>
              </div></td>
            </tr>)}</tbody>
          </table>
        </div>}
      </section>
      <p className={styles.footer}>PDF reports open in a new tab. Explanations open here in your workspace.</p>
    </main>
    {explanationReport && <ExplanationDialog key={explanationReport.id} report={explanationReport} onClose={() => setExplanationReport(null)} />}
    {showUpload && <Modal title="Upload lab report" onClose={() => setShowUpload(false)} busy={uploading}>
      <form onSubmit={uploadReport} className={styles.form}>
        <p className={styles.subtitle}>Add a report for a registered patient.</p>
        <label>Patient name<input name="patientName" required placeholder="Full name" disabled={uploading} /></label>
        <label>Patient email<input name="patientEmail" type="email" required placeholder="patient@example.com" disabled={uploading} /></label>
        <label>Report type<select name="reportType" required defaultValue="" disabled={uploading}><option value="" disabled>Select report type</option>{reportTypes.map(type => <option key={type} value={type}>{label(type)}</option>)}</select></label>
        <label className={styles.fileUpload}>Report PDF<input name="file" type="file" accept=".pdf,application/pdf" required disabled={uploading} /><small>Maximum 20 MB. The PDF must contain selectable text.</small></label>
        {uploadError && <p role="alert" className={styles.error}>{uploadError}</p>}
        {uploading && <p role="status" className={styles.subtitle}>Saving the report and preparing its explanation. This may take a few minutes.</p>}
        <div className={styles.modalActions}><button type="button" className={styles.secondary} disabled={uploading} onClick={() => setShowUpload(false)}>Cancel</button><button className={styles.primary} disabled={uploading}>{uploading ? "Uploading…" : "Upload report"}</button></div>
      </form>
    </Modal>}
    {deleteReport && <Modal title="Delete report?" onClose={() => setDeleteReport(null)} busy={deleting}>
      <div className={styles.form}><p>This will permanently delete <strong>{deleteReport.fileName || label(deleteReport.reportType)}</strong> and its explanation for <strong>{deleteReport.patientName}</strong>.</p><p className={styles.subtitle}>This action cannot be undone.</p>
        {deleteError && <p role="alert" className={styles.error}>{deleteError}</p>}
        <div className={styles.modalActions}><button className={styles.secondary} disabled={deleting} onClick={() => setDeleteReport(null)}>Cancel</button><button className={styles.danger} disabled={deleting} onClick={removeReport}>{deleting ? "Deleting…" : "Delete report"}</button></div>
      </div>
    </Modal>}
  </div>;
}
