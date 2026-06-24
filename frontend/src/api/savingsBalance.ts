import type { LedgerBook } from "./ledgerBook";

const NO_STORE_INIT: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};
const NO_STORE_HEADERS = NO_STORE_INIT.headers as Record<string, string>;

export interface ProductBalanceAnchorUpsertRequest {
  productId?: number | null;
  title?: string;
  anchorDate: string;
  balance: number;
  remarks?: string;
}

export interface ProductBalanceAnchorDto {
  id: number;
  productId: number;
  productName: string;
  anchorDate: string;
  balance: number;
  remarks: string;
}

export async function upsertProductBalanceAnchor(
  book: LedgerBook,
  req: ProductBalanceAnchorUpsertRequest
): Promise<ProductBalanceAnchorDto> {
  const r = await fetch(`/api/savings-balance/anchor?book=${encodeURIComponent(book)}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify(req),
  });
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}

export interface ProductPeriodSummaryDto {
  productId: number;
  productName: string;
  periodType: string;
  periodKey: string;
  inflow: number;
  outflow: number;
  netFlow: number;
  endBalance: number;
}

export async function fetchProductPeriodSummary(
  book: LedgerBook,
  yearMonth: string
): Promise<ProductPeriodSummaryDto[]> {
  const r = await fetch(
    `/api/savings-balance/period-summary?book=${encodeURIComponent(book)}&yearMonth=${encodeURIComponent(yearMonth)}`,
    NO_STORE_INIT
  );
  if (!r.ok) throw new Error(await r.text());
  return r.json();
}
