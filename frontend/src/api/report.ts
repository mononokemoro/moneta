import type { ReportCategory, ReportGranularity, ReportPeriod } from "../report/reportConfig";
import type { LedgerBook } from "./ledgerBook";

const NO_STORE_INIT: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};

export interface ReportData {
  year: number;
  period: string;
  periodLabel: string;
  category: string;
  subView: string;
  granularity: string;
  title: string;
  viewMode: "matrix" | "detail" | "calendar";
  columns: { yearMonth: string; monthLabel: string; rangeLabel: string }[];
  rows: {
    key: string;
    label: string;
    values: number[];
    periodTotal: number;
  }[];
  trend: { yearMonth: string; label: string; value: number }[];
  details: {
    date: string;
    txType: string;
    title: string;
    category: string;
    cardName: string;
    amount: number;
    remarks: string;
  }[];
  calendarDays: { date: string; total: number; cash: number; card: number }[];
  footnotes: string[];
}

/** @deprecated fetchReport 사용 */
export type MonthlyReport = ReportData;
export type { ReportPeriod };

export async function fetchReport(
  year: number,
  period: ReportPeriod,
  category: ReportCategory,
  subView: string,
  granularity: ReportGranularity,
  book: LedgerBook = "PERSONAL"
): Promise<ReportData> {
  const ts = Date.now();
  const q = new URLSearchParams({
    year: String(year),
    period,
    category,
    subView,
    granularity,
    book,
    _ts: String(ts),
  });
  const r = await fetch(`/api/report?${q}`, NO_STORE_INIT);
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function fetchMonthlyReport(year: number, period: ReportPeriod): Promise<ReportData> {
  const ts = Date.now();
  const r = await fetch(
    `/api/report/monthly?year=${year}&period=${encodeURIComponent(period)}&_ts=${ts}`,
    NO_STORE_INIT
  );
  if (!r.ok) throw new Error(await r.text());
  const legacy = await r.json();
  return {
    ...legacy,
    category: "ALL",
    subView: "MONTHLY",
    granularity: "MONTHLY",
    title: `월간 보고서 · ${legacy.year}년 · ${legacy.periodLabel}`,
    viewMode: "matrix",
    details: [],
    calendarDays: [],
    footnotes: [],
    trend: legacy.expenseTrend ?? [],
  };
}
