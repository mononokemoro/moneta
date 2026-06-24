/** 로컬 기준 YYYY-MM-DD (API·내부 상태) */
export function toIsoDate(d: Date): string {
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}-${m}-${day}`;
}

const WEEK_KO = ["일", "월", "화", "수", "목", "금", "토"];

function expandTwoDigitYear(yy: number): number {
  if (yy < 0 || yy > 99) return yy;
  return 2000 + yy;
}

function validateIsoParts(y: number, m: number, d: number): string | null {
  const dt = new Date(y, m - 1, d);
  if (dt.getFullYear() !== y || dt.getMonth() !== m - 1 || dt.getDate() !== d) return null;
  return `${String(y).padStart(4, "0")}-${String(m).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
}

/** 다양한 입력 → yyyy-MM-dd */
export function normalizeToIsoDate(input: string): string | null {
  const raw = input.trim();
  if (!raw) return null;

  if (/^\d{4}-\d{2}-\d{2}$/.test(raw)) {
    const [y, m, d] = raw.split("-").map(Number);
    return validateIsoParts(y, m, d);
  }

  if (/^\d{2}-\d{2}-\d{2}$/.test(raw)) {
    const [yy, m, d] = raw.split("-").map(Number);
    return validateIsoParts(expandTwoDigitYear(yy), m, d);
  }

  const digits = raw.replace(/\D/g, "");
  if (digits.length === 6) {
    const yy = Number(digits.slice(0, 2));
    const m = Number(digits.slice(2, 4));
    const d = Number(digits.slice(4, 6));
    return validateIsoParts(expandTwoDigitYear(yy), m, d);
  }

  if (digits.length === 8) {
    const y = Number(digits.slice(0, 4));
    const m = Number(digits.slice(4, 6));
    const d = Number(digits.slice(6, 8));
    return validateIsoParts(y, m, d);
  }

  return null;
}

/** yyyy-MM-dd → yy-mm-dd */
export function toDisplayDate(isoDate: string): string {
  const iso = normalizeToIsoDate(isoDate);
  if (!iso) return "";
  const [y, m, d] = iso.split("-");
  return `${y.slice(-2)}-${m}-${d}`;
}

/** yy-mm-dd 등 → yyyy-MM-dd */
export function fromDisplayDate(input: string): string | null {
  return normalizeToIsoDate(input);
}

/** 화면 표시용 yy-mm-dd */
export function formatDisplayDate(isoDate: string): string {
  return toDisplayDate(isoDate);
}

/** 예: 26-06-23 (화) */
export function formatDayTitle(isoDate: string): string {
  const iso = normalizeToIsoDate(isoDate);
  if (!iso) return isoDate;
  const [y, m, d] = iso.split("-").map(Number);
  const dt = new Date(y, m - 1, d);
  const w = WEEK_KO[dt.getDay()];
  return `${toDisplayDate(iso)} (${w})`;
}

export function parseIso(iso: string): Date {
  const normalized = normalizeToIsoDate(iso) ?? iso;
  const [y, m, d] = normalized.split("-").map(Number);
  return new Date(y, m - 1, d);
}

export function addMonths(iso: string, delta: number): string {
  const dt = parseIso(iso);
  dt.setMonth(dt.getMonth() + delta);
  return toIsoDate(dt);
}

export function addDays(iso: string, delta: number): string {
  const dt = parseIso(iso);
  dt.setDate(dt.getDate() + delta);
  return toIsoDate(dt);
}

/** 예: 26-06-18 */
export function formatVisitDate(isoDate: string): string {
  return toDisplayDate(isoDate);
}

/** 해당 월의 일 수 */
export function daysInMonth(year: number, month1Based: number): number {
  return new Date(year, month1Based, 0).getDate();
}

/** yy-mm-dd 입력용 (숫자 6자리 → yy-mm-dd) */
export function formatDateInput(raw: string): string {
  const digits = raw.replace(/\D/g, "").slice(0, 6);
  if (digits.length <= 2) return digits;
  if (digits.length <= 4) return `${digits.slice(0, 2)}-${digits.slice(2)}`;
  return `${digits.slice(0, 2)}-${digits.slice(2, 4)}-${digits.slice(4)}`;
}
