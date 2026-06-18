export type LedgerBook = "PERSONAL" | "HOUSEHOLD";

export const LEDGER_BOOKS: { id: LedgerBook; label: string }[] = [
  { id: "PERSONAL", label: "개인" },
  { id: "HOUSEHOLD", label: "가계" },
];

const STORAGE_KEY = "pininicong.ledgerBook";

export function loadLedgerBook(): LedgerBook {
  const raw = localStorage.getItem(STORAGE_KEY);
  if (raw === "PERSONAL" || raw === "HOUSEHOLD") return raw;
  return "PERSONAL";
}

export function saveLedgerBook(book: LedgerBook): void {
  localStorage.setItem(STORAGE_KEY, book);
}

export function ledgerBookLabel(book: LedgerBook): string {
  return LEDGER_BOOKS.find((b) => b.id === book)?.label ?? book;
}
