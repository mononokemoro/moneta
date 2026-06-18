import type { LedgerBook } from "./ledgerBook";

export type TxType = "EXPENSE" | "INCOME" | "SAVINGS";

const NO_STORE_INIT: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};
const NO_STORE_HEADERS = NO_STORE_INIT.headers as Record<string, string>;

export type ExpenseScope = "NORMAL" | "COMMON";

export interface TransactionRow {
  id: number;
  title: string;
  amount: number;
  category: string;
  cardName: string;
  remarks: string;
  expenseScope?: ExpenseScope;
}

export interface SavingsRow {
  id: number;
  title: string;
  amount: number;
  accumulatedAmount: number;
  remarks: string;
}

export interface FixedItem {
  id: number;
  title: string;
  defaultAmount: number;
  category: string;
  cardName: string;
  txType: TxType;
}

export interface DayView {
  book: string;
  bookLabel: string;
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
  paymentSummary: PaymentSummary;
  monthlyPaymentSummary: PaymentSummary;
  fixedItems?: FixedItem[];
}

export interface PaymentSummary {
  cash: number;
  creditCard: number;
  debitCard: number;
  otherCard: number;
}

function bookQuery(book: LedgerBook): string {
  return `book=${encodeURIComponent(book)}`;
}

export async function fetchDay(date: string, book: LedgerBook = "PERSONAL"): Promise<DayView> {
  const ts = Date.now();
  const r = await fetch(`/api/day?date=${encodeURIComponent(date)}&${bookQuery(book)}&_ts=${ts}`, NO_STORE_INIT);
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
  book?: LedgerBook;
  expenseScope?: ExpenseScope;
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

export interface UpdateTxBody {
  title: string;
  amount: number;
  category?: string;
  cardName?: string;
  remarks?: string;
  accumulatedAmount?: number;
  expenseScope?: ExpenseScope;
}

export async function updateTransaction(id: number, body: UpdateTxBody): Promise<void> {
  const r = await fetch(`/api/transactions/${id}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function sendFixedItemsToCashbook(
  txDate: string,
  entries: { fixedItemId: number; amount: number }[],
  book: LedgerBook = "PERSONAL"
): Promise<void> {
  const r = await fetch("/api/fixed-items/send-to-cashbook", {
    ...NO_STORE_INIT,
    method: "POST",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ txDate, entries, book }),
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function updateBudget(
  yearMonth: string,
  totalBudget: number,
  book: LedgerBook = "PERSONAL"
): Promise<void> {
  const r = await fetch(`/api/budget/${encodeURIComponent(yearMonth)}?${bookQuery(book)}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ totalBudget }),
  });
  if (!r.ok) throw new Error(await r.text());
}

export async function updateCashBalance(amount: number, book: LedgerBook = "PERSONAL"): Promise<void> {
  const r = await fetch(`/api/cash-balance?${bookQuery(book)}`, {
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
  dayMemo: string,
  book: LedgerBook = "PERSONAL"
): Promise<void> {
  const ts = Date.now();
  const r = await fetch(
    `/api/day/${encodeURIComponent(date)}/sheet?${bookQuery(book)}&_ts=${ts}`,
    {
      ...NO_STORE_INIT,
      method: "PUT",
      headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
      body: JSON.stringify({ scheduleNote, dayMemo }),
    }
  );
  if (!r.ok) throw new Error(await r.text());
}
