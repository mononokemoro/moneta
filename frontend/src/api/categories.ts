import type { LedgerBook } from "./ledgerBook";

export interface CategoryList {
  expense: { categoryType: string; name: string }[];
  income: { categoryType: string; name: string }[];
  savings: { categoryType: string; name: string }[];
  insurance: { categoryType: string; name: string }[];
}

const NO_STORE_INIT: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};

const cache = new Map<LedgerBook, CategoryList>();

export async function fetchCategories(book: LedgerBook = "PERSONAL"): Promise<CategoryList> {
  const hit = cache.get(book);
  if (hit) return hit;
  const r = await fetch(`/api/categories?book=${encodeURIComponent(book)}`, NO_STORE_INIT);
  if (!r.ok) throw new Error(await r.text());
  const data = (await r.json()) as CategoryList;
  cache.set(book, data);
  return data;
}

export function clearCategoryCache(book?: LedgerBook) {
  if (book) cache.delete(book);
  else cache.clear();
}
