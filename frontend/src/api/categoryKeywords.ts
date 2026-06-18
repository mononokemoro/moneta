import type { LedgerBook } from "./ledgerBook";
import type { TxType } from "./cashbook";

export interface CategoryKeyword {
  id: number;
  txType: TxType;
  keyword: string;
  categoryName: string;
}

const NO_STORE_INIT: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};
const NO_STORE_HEADERS = NO_STORE_INIT.headers as Record<string, string>;

const cache = new Map<string, CategoryKeyword[]>();

function cacheKey(book: LedgerBook): string {
  return book;
}

export async function fetchCategoryKeywords(book: LedgerBook = "PERSONAL"): Promise<CategoryKeyword[]> {
  const hit = cache.get(cacheKey(book));
  if (hit) return hit;
  const r = await fetch(`/api/category-keywords?book=${encodeURIComponent(book)}`, NO_STORE_INIT);
  if (!r.ok) throw new Error(await r.text());
  const data = (await r.json()) as CategoryKeyword[];
  cache.set(cacheKey(book), data);
  return data;
}

export function clearCategoryKeywordCache(book?: LedgerBook) {
  if (book) cache.delete(cacheKey(book));
  else cache.clear();
}

export async function createCategoryKeyword(
  book: LedgerBook,
  body: { txType: TxType; keyword: string; categoryName: string }
): Promise<CategoryKeyword> {
  const r = await fetch(`/api/category-keywords?book=${encodeURIComponent(book)}`, {
    ...NO_STORE_INIT,
    method: "POST",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) throw new Error(await r.text());
  clearCategoryKeywordCache(book);
  return r.json();
}

export async function updateCategoryKeyword(
  id: number,
  body: { txType: TxType; keyword: string; categoryName: string }
): Promise<CategoryKeyword> {
  const r = await fetch(`/api/category-keywords/${id}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify(body),
  });
  if (!r.ok) throw new Error(await r.text());
  clearCategoryKeywordCache();
  return r.json();
}

export async function deleteCategoryKeyword(id: number, book: LedgerBook): Promise<void> {
  const r = await fetch(`/api/category-keywords/${id}`, { ...NO_STORE_INIT, method: "DELETE" });
  if (!r.ok) throw new Error(await r.text());
  clearCategoryKeywordCache(book);
}
