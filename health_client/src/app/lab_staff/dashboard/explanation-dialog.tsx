"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { apiFetch } from "@/lib/api";

export interface ExplanationReport {
  id: string;
  patientName: string;
  reportType: string;
  fileName?: string;
  status: string;
  analysisResult?: string;
  analysisError?: string;
}

export default function ExplanationDialog({ report, onClose }: {
  report: ExplanationReport;
  onClose: () => void;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);
  const [details, setDetails] = useState<ExplanationReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  const loadExplanation = useCallback((signal?: AbortSignal) => {
    return apiFetch<ExplanationReport>(`/api/reports/${encodeURIComponent(report.id)}`, {
      signal,
      cache: "no-store",
    }).then((data) => {
      if (!signal?.aborted) {
        setDetails(data);
        setError("");
      }
    }).catch(() => {
      if (!signal?.aborted) setError("Unable to load this explanation. Please try again.");
    }).finally(() => {
      if (!signal?.aborted) setLoading(false);
    });
  }, [report.id]);

  useEffect(() => {
    const dialog = dialogRef.current;
    const controller = new AbortController();
    const previousOverflow = document.body.style.overflow;
    dialog?.showModal();
    document.body.style.overflow = "hidden";
    void loadExplanation(controller.signal);
    return () => {
      controller.abort();
      dialog?.close();
      document.body.style.overflow = previousOverflow;
    };
  }, [loadExplanation]);

  const explanation = details?.analysisResult?.trim();
  const waiting = details && ["PENDING", "PROCESSING", "PROCESSED"].includes(details.status);
  const closeDialog = () => {
    // Close before unmounting so the browser restores focus to the opening button.
    dialogRef.current?.close();
    onClose();
  };

  return (
    <dialog
      ref={dialogRef}
      aria-labelledby="explanation-title"
      onCancel={(event) => {
        event.preventDefault();
        closeDialog();
      }}
      className="m-auto max-h-[90dvh] w-[calc(100%-2rem)] max-w-3xl overflow-y-auto rounded-2xl border border-[#DCD6C8] bg-white p-0 text-[#1B1B18] shadow-2xl backdrop:bg-black/50"
    >
      <div className="sticky top-0 flex items-start justify-between gap-4 border-b border-[#DCD6C8] bg-white px-6 py-5">
        <div className="min-w-0">
          <h2 id="explanation-title" className="text-xl font-semibold text-[#0B3D3A]">Report explanation</h2>
          <p className="mt-1 break-words text-sm text-[#4A463E]">{report.fileName || report.reportType}</p>
          <p className="mt-1 break-words text-sm text-[#4A463E]">{report.patientName} · {report.reportType.replaceAll("_", " ")}</p>
        </div>
        <button
          type="button"
          autoFocus
          onClick={closeDialog}
          aria-label="Close explanation"
          className="flex h-10 w-10 shrink-0 cursor-pointer items-center justify-center rounded-lg border border-[#DCD6C8] text-2xl hover:bg-[#F6F3EC] focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-[#0B3D3A]"
        >×</button>
      </div>
      <div className="p-6" aria-busy={loading}>
        {loading ? (
          <p role="status">Loading explanation...</p>
        ) : error ? (
          <p role="alert" className="text-[#C0392B]">{error}</p>
        ) : explanation ? (
          <div className="whitespace-pre-wrap break-words text-sm leading-7">{explanation}</div>
        ) : (
          <div role="status" className="rounded-lg bg-[#F6F3EC] p-4 text-sm leading-6">
            <p>{waiting
              ? "This report’s explanation is still being prepared. Refresh to check again."
              : details?.status === "FAILED"
                ? "An explanation could not be generated for this report. The original PDF is still available."
                : "No explanation is available for this report yet."}</p>
            {details?.analysisError && <p className="mt-2">{details.analysisError}</p>}
          </div>
        )}
        {!loading && (error || !explanation) && (
          <button type="button" onClick={() => {
            setLoading(true);
            void loadExplanation();
          }} className="mt-4 cursor-pointer rounded-lg bg-[#0B3D3A] px-4 py-2 text-sm font-semibold text-white">
            {error ? "Try again" : "Refresh explanation"}
          </button>
        )}
      </div>
    </dialog>
  );
}
