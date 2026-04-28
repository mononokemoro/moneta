export type TxType = "EXPENSE" | "INCOME" | "SAVINGS";

const NO_STORE_INIT: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};
const NO_STORE_HEADERS = NO_STORE_INIT.headers as Record<string, string>;

export interface TransactionRow {
  id: number;
  title: string;
  amount: number;
  category: string;
  cardName: string;
  remarks: string;
}

export interface SavingsRow {
  id: number;
  title: string;
  amount: number;
  accumulatedAmount: number;
  remarks: string;
}

export interface DayView {
  date: string;
  yearMonth: string;
  budget: {
    totalBudget: number;
    spentInMonth: number;
    remainingBudget: number;
    periodLabel: string;
  };
  cashBalance: number;
  expenses: TransactionRow[];
  incomes: TransactionRow[];
  savings: SavingsRow[];
  scheduleNote: string;
  dayMemo: string;
  paymentSummary: {
    cash: number;
    creditCard: number;
    debitCard: number;
    otherCard: number;
  };
}

export async function fetchDay(date: string): Promise<DayView> {
  const ts = Date.now();
  const r = await fetch(`/api/day?date=${encodeURIComponent(date)}&_ts=${ts}`, NO_STORE_INIT);
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export interface CreateTxBody {
  txDate: string;
  txType: TxType;
  title: string;
  amount: number;
  category?: string;
  cardName?: string;
  remarks?: string;
  accumulatedAmount?: number;
}

export async function createTransaction(body: CreateTxBody): Promise<void> {
  const r = await fetch("/api/transactions", {
    ...NO_STORE_INIT,
    method: "POST",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function deleteTransaction(id: number): Promise<void> {
  const r = await fetch(`/api/transactions/${id}`, { ...NO_STORE_INIT, method: "DELETE" });
  if (!r.ok) throw new Error(await r.text());
}

export async function updateBudget(yearMonth: string, totalBudget: number): Promise<void> {
  const r = await fetch(`/api/budget/${encodeURIComponent(yearMonth)}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ totalBudget }),
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function updateCashBalance(amount: number): Promise<void> {
  const r = await fetch("/api/cash-balance", {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ amount }),
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function updateDailySheet(
  date: string,
  scheduleNote: string,
  dayMemo: string
): Promise<void> {
  const ts = Date.now();
  const r = await fetch(`/api/day/${encodeURIComponent(date)}/sheet?_ts=${ts}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ scheduleNote, dayMemo }),
  });
  if (!r.ok) throw new Error(await r.text());
}
