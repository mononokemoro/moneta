/** 로컬 기준 YYYY-MM-DD */
export function toIsoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

const WEEK_KO = ["일", "월", "화", "수", "목", "금", "토"];

/** 예: 2026.04.28 (화) */
export function formatDayTitle(isoDate: string): string {
  const [y, m, d] = isoDate.split("-").map(Number);
  const dt = new Date(y, m - 1, d);
  const w = WEEK_KO[dt.getDay()];
  return `${y}.${String(m).padStart(2, "0")}.${String(d).padStart(2, "0")} (${w})`;
}

export function parseIso(iso: string): Date {
  const [y, m, d] = iso.split("-").map(Number);
  return new Date(y, m - 1, d);
}

export function addMonths(iso: string, delta: number): string {
  const dt = parseIso(iso);
  dt.setMonth(dt.getMonth() + delta);
  return toIsoDate(dt);
}

/** 해당 월의 일 수 */
export function daysInMonth(year: number, month1Based: number): number {
  return new Date(year, month1Based, 0).getDate();
}
