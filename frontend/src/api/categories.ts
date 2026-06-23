import type { LedgerBook } from "./ledgerBook";

export type CategoryType = "EXPENSE" | "INCOME" | "SAVINGS" | "INSURANCE";
export type CategoryTier = "MAJOR" | "MINOR";

export interface CategoryItem {
  id: number;
  categoryType: CategoryType;
  name: string;
  tier: CategoryTier;
  parentId: number | null;
  sortOrder: number;
  enabled: boolean;
  userCreated: boolean;
  fixedExpense: boolean;
  inUse: boolean;
}

export interface CategoryGroup {
  id: number;
  name: string;
  sortOrder: number;
  enabled: boolean;
  userCreated: boolean;
  fixedExpense: boolean;
  inUse: boolean;
  children: CategoryItem[];
}

export interface CategoryList {
  expense: CategoryGroup[];
  income: CategoryGroup[];
  savings: CategoryGroup[];
  insurance: CategoryGroup[];
}

const NO_STORE_INIT: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};
const NO_STORE_HEADERS = NO_STORE_INIT.headers as Record<string, string>;

const cache = new Map<LedgerBook, CategoryList>();

export function flattenMinorNames(groups: CategoryGroup[]): string[] {
  return groups.flatMap((g) => g.children.map((c) => c.name));
}

/** 입력/선택 UI용 활성 분류 트리 */
export function selectableCategoryGroups(groups: CategoryGroup[]): CategoryGroup[] {
  return groups
    .filter((g) => g.enabled)
    .map((g) => ({
      ...g,
      children: g.children.filter((c) => c.enabled),
    }));
}

/** 가계부·예약어 등에서 선택 가능한 분류명 (소분류 우선, 없으면 대분류) */
export function flattenSelectableCategoryNames(groups: CategoryGroup[]): string[] {
  const names: string[] = [];
  const seen = new Set<string>();
  for (const g of groups) {
    if (!g.enabled) continue;
    const enabledChildren = g.children.filter((c) => c.enabled);
    if (enabledChildren.length > 0) {
      for (const c of enabledChildren) {
        if (!seen.has(c.name)) {
          seen.add(c.name);
          names.push(c.name);
        }
      }
    } else if (!seen.has(g.name)) {
      seen.add(g.name);
      names.push(g.name);
    }
  }
  return names;
}

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

export async function createCategory(
  book: LedgerBook,
  categoryType: CategoryType,
  tier: CategoryTier,
  name: string,
  parentId?: number | null
): Promise<CategoryItem> {
  const r = await fetch(`/api/categories?book=${encodeURIComponent(book)}`, {
    ...NO_STORE_INIT,
    method: "POST",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ categoryType, tier, name, parentId: parentId ?? null }),
  });
  if (!r.ok) throw new Error(await r.text());
  clearCategoryCache(book);
  return r.json();
}

export async function updateCategory(
  id: number,
  book: LedgerBook,
  name: string,
  parentId?: number | null
): Promise<CategoryItem> {
  const r = await fetch(`/api/categories/${id}?book=${encodeURIComponent(book)}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ name, parentId: parentId ?? null }),
  });
  if (!r.ok) throw new Error(await r.text());
  clearCategoryCache(book);
  return r.json();
}

export async function deleteCategory(id: number, book: LedgerBook): Promise<void> {
  const r = await fetch(`/api/categories/${id}?book=${encodeURIComponent(book)}`, {
    ...NO_STORE_INIT,
    method: "DELETE",
  });
  if (!r.ok) throw new Error(await r.text());
  clearCategoryCache(book);
}

export async function reorderCategories(
  book: LedgerBook,
  items: { id: number; sortOrder: number; parentId?: number | null }[]
): Promise<void> {
  const r = await fetch(`/api/categories/reorder?book=${encodeURIComponent(book)}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ items }),
  });
  if (!r.ok) throw new Error(await r.text());
  clearCategoryCache(book);
}

export function groupsForType(list: CategoryList, type: CategoryType): CategoryGroup[] {
  switch (type) {
    case "EXPENSE":
      return list.expense;
    case "INCOME":
      return list.income;
    case "SAVINGS":
      return list.savings;
    case "INSURANCE":
      return list.insurance;
  }
}

export async function saveCategoryPreferences(
  book: LedgerBook,
  items: { id: number; enabled?: boolean; fixedExpense?: boolean }[]
): Promise<void> {
  const r = await fetch(`/api/categories/preferences?book=${encodeURIComponent(book)}`, {
    ...NO_STORE_INIT,
    method: "PUT",
    headers: { ...NO_STORE_HEADERS, "Content-Type": "application/json" },
    body: JSON.stringify({ items }),
  });
  if (!r.ok) throw new Error(await r.text());
  clearCategoryCache(book);
}
