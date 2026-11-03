"use client";

import { useEffect, useState } from "react";
import { ApiError, apiFetch } from "@/lib/api";
import { addDays, clinicToday, dayLabel, formatAppointmentDate, previewSlots, weekdays, type AvailabilityRule, type AvailabilitySlot } from "@/lib/availability";

type Availability = {
  specialization: string; availableSlots: AvailabilitySlot[]; oneOffSlots: AvailabilitySlot[];
  availabilityRules: AvailabilityRule[]; excludedDates: string[]; windowStart: string; windowEnd: string;
};
const input = { padding: 10, border: "1px solid #DCD6C8", borderRadius: 6, background: "#FFF", color: "#000", width: "100%", marginTop: 6 };
const button = { padding: "9px 16px", border: "1px solid #0B3D3A", borderRadius: 6, background: "#0B3D3A", color: "#FFF", cursor: "pointer" };
const secondary = { ...button, background: "#FFF", color: "#0B3D3A", padding: "6px 12px" };
const danger = { ...secondary, color: "#A12424", borderColor: "#E3BABA" };

function loadError(failure: unknown) {
  if (failure instanceof ApiError) {
    if (failure.status === 401) return "Your session or service authentication could not be verified. Sign in again; if this continues, the service configuration needs attention.";
    if (failure.status === 403) return "The service denied access to your schedule. Please sign in with your registered doctor account.";
    if (failure.status === 404 || failure.status === 405) return "The connected appointment service does not support this schedule editor yet. It needs the latest backend update.";
    return "Your schedule could not be loaded (HTTP " + failure.status + "). Your saved availability has not been changed.";
  }
  return "Cannot reach the appointment service. Check your connection and reload your schedule.";
}

export default function DoctorAvailability() {
  const [specialization, setSpecialization] = useState("");
  const [rules, setRules] = useState<AvailabilityRule[]>([]);
  const [excluded, setExcluded] = useState<string[]>([]);
  const [manual, setManual] = useState<AvailabilitySlot[]>([]);
  const [first, setFirst] = useState(clinicToday);
  const [startDate, setStartDate] = useState(clinicToday);
  const [days, setDays] = useState<string[]>([]);
  const [startTime, setStartTime] = useState("09:00");
  const [endTime, setEndTime] = useState("12:00");
  const [duration, setDuration] = useState(30);
  const [repeat, setRepeat] = useState(true);
  const [editing, setEditing] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const [reload, setReload] = useState(0);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [dirty, setDirty] = useState(false);
  const [formDirty, setFormDirty] = useState(false);
  const accept = (data: Availability) => {
    if (!Array.isArray(data.availabilityRules) || !Array.isArray(data.excludedDates) || !data.windowStart)
      throw new Error("The connected appointment service is running an older version. Update the backend before editing your schedule.");
    setSpecialization(data.specialization || ""); setRules(data.availabilityRules);
    setExcluded(data.excludedDates); setManual(data.oneOffSlots || []); setFirst(data.windowStart);
  };
  useEffect(() => {
    let active = true;
    apiFetch<Availability>("/api/doc_booking/doctor/me/availability", { cache: "no-store" })
      .then(data => { if (active) { accept(data); setLoaded(true); setError(""); } })
      .catch(failure => { if (active) { setError(failure instanceof Error && failure.message.includes("older version") ? failure.message : loadError(failure)); setLoaded(false); } })
      .finally(() => { if (active) setLoading(false); });
    return () => { active = false; };
  }, [reload]);
  const changed = () => { setDirty(true); setNotice(""); };
  const editForm = () => { setFormDirty(true); setNotice(""); };
  const slots = previewSlots(rules, excluded, manual, first);
  const addRule = (event: React.FormEvent) => {
    event.preventDefault(); setError("");
    const length = (Number(endTime.slice(0, 2)) * 60 + Number(endTime.slice(3))) - (Number(startTime.slice(0, 2)) * 60 + Number(startTime.slice(3)));
    if (!days.length) { setError("Choose at least one weekday."); return; }
    if (!startDate || (!editing && startDate < clinicToday())) { setError("Choose today or a future start date."); return; }
    if (length < duration || length % duration !== 0) { setError("Choose an end time after the start time. The range must contain whole appointments of your selected duration."); return; }
    const rule = { id: editing || crypto.randomUUID(), startDate, daysOfWeek: days, startTime, endTime, slotMinutes: duration, repeatWeekly: repeat };
    setRules(current => editing ? current.map(item => item.id === editing ? rule : item) : [...current, rule]);
    setEditing(null); setFormDirty(false); changed();
  };
  const save = async () => {
    setSaving(true); setError(""); setNotice("");
    try {
      const data = await apiFetch<Availability>("/api/doc_booking/doctor/me/availability", {
        method: "PUT", body: JSON.stringify({ specialization, availabilityRules: rules, excludedDates: excluded }),
      });
      accept(data); setDirty(false); setNotice("Schedule saved. Patients can book your open appointments for the next two weeks.");
    } catch (failure) { setError(failure instanceof Error ? failure.message : "Unable to save your schedule."); }
    finally { setSaving(false); }
  };
  return <section style={{ background: "#FFF", border: "1px solid #DCD6C8", borderRadius: 10, padding: 22, marginBottom: 24, color: "#000" }} aria-labelledby="availability-title">
    <h2 id="availability-title" style={{ fontFamily: "'Fraunces', Georgia, serif", fontSize: 18, color: "#0B3D3A", margin: "0 0 12px" }}>My availability</h2>
    <p style={{ fontSize: 13, marginBottom: 16 }}>Choose your working days and time range. All times are in Sri Lanka time (Asia/Colombo). One patient can book each appointment.</p>
    {loading && <p role="status">Loading your saved schedule…</p>}
    {!loading && !loaded && <div style={{ background: "#FFF3E0", padding: 12, borderRadius: 6, marginBottom: 16 }}>
      <p style={{ marginBottom: 10 }}>The form is shown below, but saving stays disabled until your existing schedule loads.</p>
      <button type="button" style={secondary} onClick={() => { setLoading(true); setError(""); setReload(value => value + 1); }}>Reload schedule</button>
    </div>}
    {error && <p role="alert" style={{ color: "#A12424", marginBottom: 14 }}>{error}</p>}
    <fieldset disabled={loading || saving || !loaded} style={{ border: 0, padding: 0, margin: 0, minWidth: 0, opacity: loaded ? 1 : 0.65 }}>
      <label style={{ display: "block", fontSize: 14, marginBottom: 16 }}>Specialization<input value={specialization} maxLength={120} onChange={event => { setSpecialization(event.target.value); changed(); }} placeholder="e.g. General practitioner" style={input} /></label>
      <form onSubmit={addRule} style={{ background: "#F6F3EC", padding: 16, borderRadius: 8 }}>
        <h3 style={{ fontSize: 15, color: "#0B3D3A", marginBottom: 12 }}>{editing ? "Edit time range" : "Add a time range"}</h3>
        <div style={{ display: "flex", flexWrap: "wrap", gap: 8, marginBottom: 16 }} role="group" aria-label="Days of the week">
          {weekdays.map(day => <label key={day} style={{ display: "flex", alignItems: "center", gap: 6, padding: "8px 10px", border: "1px solid #DCD6C8", borderRadius: 6, background: days.includes(day) ? "#E6F4EA" : "#FFF", cursor: "pointer" }}>
            <input type="checkbox" checked={days.includes(day)} onChange={() => { setDays(current => current.includes(day) ? current.filter(value => value !== day) : [...current, day]); editForm(); }} />{dayLabel(day)}
          </label>)}
        </div>
        <div style={{ display: "grid", gridTemplateColumns: "repeat(auto-fit, minmax(160px, 1fr))", gap: 12, fontSize: 14 }}>
          <label>Starting from<input type="date" value={startDate} required min={editing ? undefined : clinicToday()} onChange={event => { setStartDate(event.target.value); editForm(); }} style={input} /></label>
          <label>Start time<input type="time" value={startTime} required onChange={event => { setStartTime(event.target.value); editForm(); }} style={input} /></label>
          <label>End time<input type="time" value={endTime} required onChange={event => { setEndTime(event.target.value); editForm(); }} style={input} /></label>
          <label>Appointment duration<select value={duration} onChange={event => { setDuration(Number(event.target.value)); editForm(); }} style={input}>{[5, 10, 15, 20, 30, 60].map(value => <option key={value} value={value}>{value} minutes</option>)}</select></label>
          <label>Repeat<select value={repeat ? "weekly" : "once"} onChange={event => { setRepeat(event.target.value === "weekly"); editForm(); }} style={input}><option value="weekly">Repeat every week</option><option value="once">Only once</option></select></label>
        </div>
        <p style={{ fontSize: 12, margin: "12px 0" }}>{repeat ? "Your selected days repeat every week until you remove the rule. Only the next 14 days are shown and bookable." : "Each selected weekday occurs once in the seven days starting from your chosen date."} The end time is when your last appointment finishes.</p>
        <button type="submit" style={button}>{editing ? "Update time range" : "Add time range"}</button>
        {editing && <button type="button" style={{ ...secondary, marginLeft: 8 }} onClick={() => { setEditing(null); setFormDirty(false); }}>Cancel edit</button>}
      </form>
      <h3 style={{ fontSize: 15, color: "#0B3D3A", margin: "18px 0 8px" }}>Schedule rules</h3>
      {!rules.length && <p style={{ fontSize: 13 }}>No time ranges added yet.</p>}
      <ul style={{ padding: 0, listStyle: "none", margin: 0 }}>
        {rules.map(rule => <li key={rule.id} style={{ display: "flex", flexWrap: "wrap", gap: 10, alignItems: "center", justifyContent: "space-between", padding: "12px 0", borderBottom: "1px solid #EEE" }}>
          <div><strong>{weekdays.filter(day => rule.daysOfWeek.includes(day)).map(dayLabel).join(", ")}</strong><p style={{ fontSize: 13, margin: "4px 0 0" }}>{rule.startTime}–{rule.endTime} · {rule.slotMinutes} minutes · {rule.repeatWeekly ? "Every week" : "Only once"} · From {rule.startDate}</p></div>
          <div style={{ display: "flex", gap: 8 }}>
            <button type="button" style={secondary} onClick={() => { setEditing(rule.id); setStartDate(rule.startDate); setDays(rule.daysOfWeek); setStartTime(rule.startTime); setEndTime(rule.endTime); setDuration(rule.slotMinutes); setRepeat(rule.repeatWeekly); setFormDirty(false); }}>Edit</button>
            <button type="button" style={danger} onClick={() => { setRules(current => current.filter(item => item.id !== rule.id)); if (editing === rule.id) { setEditing(null); setFormDirty(false); } changed(); }}>Remove rule</button>
          </div>
        </li>)}
      </ul>
      <h3 style={{ fontSize: 15, color: "#0B3D3A", margin: "18px 0 8px" }}>Next two weeks{dirty ? " — unsaved preview" : ""}</h3>
      <p style={{ fontSize: 12, marginBottom: 8 }}>Skip a date if you are unavailable that day. It will not remove your weekly rule or cancel appointments already booked. This is your working schedule; patients see only unbooked times.</p>
      <div style={{ maxHeight: 340, overflowY: "auto" }}>
        {Array.from({ length: 14 }, (_, index) => addDays(first, index)).map(date => {
          const times = slots.filter(slot => slot.date === date).map(slot => slot.time);
          const skipped = excluded.includes(date);
          return <div key={date} style={{ padding: "12px 0", borderBottom: "1px solid #EEE", display: "flex", justifyContent: "space-between", alignItems: "center", gap: 12 }}>
            <div><strong style={{ fontSize: 14 }}>{formatAppointmentDate(date)}</strong><p style={{ fontSize: 13, margin: "4px 0 0" }}>{skipped ? "Unavailable for this date" : times.length ? times.join(", ") : "No available times"}</p></div>
            {(skipped || times.length > 0) && <button type="button" style={{ ...(skipped ? secondary : danger), flexShrink: 0 }} aria-label={(skipped ? "Restore " : "Skip ") + date} onClick={() => { setExcluded(current => skipped ? current.filter(item => item !== date) : [...current, date]); changed(); }}>{skipped ? "Restore day" : "Skip day"}</button>}
          </div>;
        })}
      </div>
      {formDirty && <p style={{ fontSize: 13, marginTop: 14 }}>Click {editing ? "Update time range" : "Add time range"} to include the form changes before saving.</p>}
      <button type="button" style={{ ...button, marginTop: 18, opacity: !dirty || formDirty ? 0.6 : 1 }} disabled={!specialization.trim() || !dirty || formDirty || !!editing} onClick={save}>{saving ? "Saving…" : "Save availability"}</button>
      {dirty && <span style={{ marginLeft: 12, fontSize: 13 }}>Changes are published when you save.</span>}
    </fieldset>
    {notice && <p role="status" style={{ color: "#1B5E20", marginTop: 12 }}>{notice}</p>}
  </section>;
}
