import type { LedgerBook } from "./ledgerBook";

export type ProductType = "SAVINGS" | "INSURANCE" | "LOAN" | "CARD";
export type ProductStatus = "ACTIVE" | "MATURED" | "TERMINATED" | "PREPAID" | "CANCELLED";

export interface FinancialProduct {
  id: number | null;
  productType: ProductType;
  status: ProductStatus;
  sortOrder: number;
  classification: string;
  name: string;
  paymentMethod: string;
  joinDate: string;
  maturityDate: string;
  startDate: string;
  autoTransferDay: string;
  transferDay: string;
  repaymentDay: string;
  paymentDay: string;
  periodStartMonth: string;
  periodStartDay: string;
  periodEndMonth: string;
  periodEndDay: string;
  principal: string;
  cardLimit: string;
}

export interface ProductList {
  savings: FinancialProduct[];
  insurance: FinancialProduct[];
  loans: FinancialProduct[];
  cards: FinancialProduct[];
}

const NO_STORE_INIT: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};
const NO_STORE_HEADERS = NO_STORE_INIT.headers as Record<string, string>;

export async function fetchFinancialProducts(book: LedgerBook): Promise<ProductList> {
  const r = await fetch(`/api/financial-products?book=${encodeURIComponent(book)}`, NO_STORE_INIT);
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export async function saveFinancialProducts(book: LedgerBook, products: ProductList): Promise<ProductList> {
  const r = await fetch(`/api/financial-products?book=${encodeURIComponent(book)}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ products }),
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export interface CardSyncFromTransactionsResponse {
  added: number;
  products: ProductList;
}

export async function syncCardsFromTransactions(
  book: LedgerBook
): Promise<CardSyncFromTransactionsResponse> {
  const r = await fetch(
    `/api/financial-products/sync-cards-from-transactions?book=${encodeURIComponent(book)}`,
    { ...NO_STORE_INIT, method: "POST" }
  );
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export function savingsStatusFromApi(status: ProductStatus): "active" | "matured" | "terminated" {
  switch (status) {
    case "MATURED":
      return "matured";
    case "TERMINATED":
      return "terminated";
    default:
      return "active";
  }
}

export function savingsStatusToApi(status: "active" | "matured" | "terminated"): ProductStatus {
  switch (status) {
    case "matured":
      return "MATURED";
    case "terminated":
      return "TERMINATED";
    default:
      return "ACTIVE";
  }
}

export function loanStatusFromApi(status: ProductStatus): "active" | "matured" | "prepaid" {
  switch (status) {
    case "MATURED":
      return "matured";
    case "PREPAID":
      return "prepaid";
    default:
      return "active";
  }
}

export function loanStatusToApi(status: "active" | "matured" | "prepaid"): ProductStatus {
  switch (status) {
    case "matured":
      return "MATURED";
    case "prepaid":
      return "PREPAID";
    default:
      return "ACTIVE";
  }
}

export function cardStatusFromApi(status: ProductStatus): "active" | "cancelled" {
  return status === "CANCELLED" ? "cancelled" : "active";
}

export function cardStatusToApi(status: "active" | "cancelled"): ProductStatus {
  return status === "cancelled" ? "CANCELLED" : "ACTIVE";
}
