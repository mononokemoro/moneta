export interface TransactionTableRow {
  id: number;
  book: string;
  txDate: string;
  txType: string;
  title: string;
  amount: number;
  categoryId: number | null;
  householdCategoryId: number | null;
  cardProductId: number | null;
  savingsProductId: number | null;
  remarks: string;
  accumulatedAmount: number | null;
  sortOrder: number | null;
  expenseScope: string;
  linkedTxId: number | null;
}

export interface TransactionTablePreview {
  tableName: string;
  title: string;
  subtitle?: string;
  txDate?: string;
  count: number;
  querySql?: string;
  groupByTxType?: boolean;
  rows: TransactionTableRow[];
}

export const TX_TABLE_SECTIONS: { label: string; types: string[] }[] = [
  { label: "지출", types: ["EXPENSE"] },
  { label: "수입", types: ["INCOME"] },
  { label: "저축/보험", types: ["SAVINGS", "INSURANCE"] },
];

export function groupRowsByTxType(
  rows: TransactionTableRow[],
): { label: string; rows: TransactionTableRow[] }[] {
  return TX_TABLE_SECTIONS.map((section) => ({
    label: section.label,
    rows: rows.filter((row) => section.types.includes(row.txType)),
  })).filter((section) => section.rows.length > 0);
}

export const TRANSACTION_TABLE_COLUMNS: {
  key: keyof TransactionTableRow;
  label: string;
}[] = [
  { key: "id", label: "id" },
  { key: "book", label: "book" },
  { key: "txDate", label: "tx_date" },
  { key: "txType", label: "tx_type" },
  { key: "title", label: "title" },
  { key: "amount", label: "amount" },
  { key: "categoryId", label: "category_id" },
  { key: "householdCategoryId", label: "household_category_id" },
  { key: "cardProductId", label: "card_product_id" },
  { key: "savingsProductId", label: "savings_product_id" },
  { key: "remarks", label: "remarks" },
  { key: "accumulatedAmount", label: "accumulated_amount" },
  { key: "sortOrder", label: "sort_order" },
  { key: "expenseScope", label: "expense_scope" },
  { key: "linkedTxId", label: "linked_tx_id" },
];

export function formatTransactionTableCell(value: unknown): string {
  if (value == null || value === "") return "—";
  if (typeof value === "number") return String(value);
  return String(value);
}
