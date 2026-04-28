export type ReportPeriod = "H1" | "H2" | "FY";

const NO_STORE_INIT: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};

export interface MonthlyReport {
  year: number;
  period: string;
  periodLabel: string;
  columns: { yearMonth: string; monthLabel: string; rangeLabel: string }[];
  rows: {
    key: string;
    label: string;
    values: number[];
    periodTotal: number;
  }[];
  expenseTrend: { yearMonth: string; label: string; value: number }[];
}

export async function fetchMonthlyReport(year: number, period: ReportPeriod): Promise<MonthlyReport> {
  const ts = Date.now();
  const r = await fetch(
    `/api/report/monthly?year=${year}&period=${encodeURIComponent(period)}&_ts=${ts}`,
    NO_STORE_INIT
  );
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}
