import type { LedgerBook } from "./ledgerBook";

export type FixedKind = "INCOME" | "SAVINGS" | "LOAN" | "EXPENSE";
export type FixedScheduleType = "MONTHLY" | "DAILY";
export type FixedHolidayAdjust = "NONE" | "PREVIOUS" | "NEXT";
export type FixedPeriodType = "CONTINUOUS" | "RANGE";

export interface FixedItemRow {
  id: number;
  title: string;
  defaultAmount: number;
  interestAmount: number;
  category: string;
  cardName: string;
  paymentMethod: string;
  remarks: string;
  txType: string;
  kind: FixedKind;
  scheduleType: FixedScheduleType;
  dayOfMonth: number | null;
  lastDayOfMonth: boolean;
  holidayAdjust: FixedHolidayAdjust;
  periodType: FixedPeriodType;
  periodStart: string | null;
  periodEnd: string | null;
  sortOrder: number;
  scheduleLabel: string;
  holidayCheck: boolean;
}

export interface FixedItemSaveBody {
  kind: FixedKind;
  scheduleType: FixedScheduleType;
  dayOfMonth?: number | null;
  lastDayOfMonth?: boolean;
  holidayAdjust?: FixedHolidayAdjust;
  periodType?: FixedPeriodType;
  periodStart?: string | null;
  periodEnd?: string | null;
  title: string;
  defaultAmount?: number;
  interestAmount?: number;
  category?: string;
  cardName?: string;
  paymentMethod?: string;
  remarks?: string;
}

const NO_STORE_INIT: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};
const NO_STORE_HEADERS = NO_STORE_INIT.headers as Record<string, string>;

function bookQuery(book: LedgerBook): string {
  return `book=${encodeURIComponent(book)}`;
}

export async function fetchFixedItems(
  book: LedgerBook,
  kind?: FixedKind,
  scheduleType?: FixedScheduleType,
): Promise<FixedItemRow[]> {
  const params = new URLSearchParams({ book });
  if (kind) params.set("kind", kind);
  if (scheduleType) params.set("scheduleType", scheduleType);
  const r = await fetch(`/api/fixed-items?${params.toString()}`, NO_STORE_INIT);
  if (!r.ok) throw new Error(await r.text());
  const data = (await r.json()) as { items: FixedItemRow[] };
  return data.items ?? [];
}

export async function createFixedItem(book: LedgerBook, body: FixedItemSaveBody): Promise<FixedItemRow> {
  const r = await fetch(`/api/fixed-items?${bookQuery(book)}`, {
    ...NO_STORE_INIT,
    method: "POST",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function updateFixedItem(
  id: number,
  book: LedgerBook,
  body: FixedItemSaveBody,
): Promise<FixedItemRow> {
  const r = await fetch(`/api/fixed-items/${id}?${bookQuery(book)}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function deleteFixedItems(book: LedgerBook, ids: number[]): Promise<void> {
  const r = await fetch(`/api/fixed-items/bulk-delete?${bookQuery(book)}`, {
    ...NO_STORE_INIT,
    method: "POST",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ ids }),
  });
  if (!r.ok) throw new Error(await r.text());
}
