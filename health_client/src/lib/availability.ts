export type AvailabilitySlot = { date: string; time: string };
export type AvailabilityRule = {
  id: string; startDate: string; daysOfWeek: string[];
  startTime: string; endTime: string; slotMinutes: number; repeatWeekly: boolean;
};
export const weekdays = ["MONDAY", "TUESDAY", "WEDNESDAY", "THURSDAY", "FRIDAY", "SATURDAY", "SUNDAY"];
export const dayLabel = (day: string) => day.charAt(0) + day.slice(1).toLowerCase();
export const clinicToday = () => new Intl.DateTimeFormat("en-CA", { timeZone: "Asia/Colombo", year: "numeric", month: "2-digit", day: "2-digit" }).format(new Date());
export const addDays = (date: string, count: number) => {
  const value = new Date(`${date}T12:00:00Z`); value.setUTCDate(value.getUTCDate() + count);
  return value.toISOString().slice(0, 10);
};
export const formatAppointmentDate = (date: string) => {
  const value = new Date(`${date}T12:00:00Z`);
  return Number.isNaN(value.getTime()) ? date : `${date} (${new Intl.DateTimeFormat("en", { weekday: "long", timeZone: "Asia/Colombo" }).format(value)})`;
};
const minutes = (time: string) => Number(time.slice(0, 2)) * 60 + Number(time.slice(3));
export function previewSlots(rules: AvailabilityRule[], excluded: string[], manual: AvailabilitySlot[], first: string) {
  const last = addDays(first, 13);
  const slots = new Map<string, AvailabilitySlot>();
  const add = (slot: AvailabilitySlot) => {
    if (slot.date >= first && slot.date <= last && !excluded.includes(slot.date) && new Date(`${slot.date}T${slot.time}:00+05:30`).getTime() > Date.now())
      slots.set(`${slot.date} ${slot.time}`, slot);
  };
  manual.forEach(add);
  for (let offset = 0; offset < 14; offset++) {
    const date = addDays(first, offset);
    const day = new Intl.DateTimeFormat("en", { weekday: "long", timeZone: "UTC" }).format(new Date(`${date}T12:00:00Z`)).toUpperCase();
    for (const rule of rules) {
      if (date < rule.startDate || (!rule.repeatWeekly && date >= addDays(rule.startDate, 7)) || !rule.daysOfWeek.includes(day) || rule.slotMinutes < 5) continue;
      for (let time = minutes(rule.startTime); time + rule.slotMinutes <= minutes(rule.endTime); time += rule.slotMinutes)
        add({ date, time: `${String(Math.floor(time / 60)).padStart(2, "0")}:${String(time % 60).padStart(2, "0")}` });
    }
  }
  return [...slots.values()].sort((a, b) => `${a.date} ${a.time}`.localeCompare(`${b.date} ${b.time}`));
}
