import type { CategoryKeyword } from "../api/categoryKeywords";
import type { TxType } from "../api/cashbook";

export function matchCategoryByKeyword(
  title: string,
  rules: CategoryKeyword[],
  txType: TxType
): string | null {
  if (!title.trim() || rules.length === 0) return null;

  const matched = rules
    .filter((r) => r.txType === txType && r.keyword.trim())
    .filter((r) => title.includes(r.keyword))
    .sort((a, b) => {
      const len = b.keyword.length - a.keyword.length;
      if (len !== 0) return len;
      return a.id - b.id;
    });

  return matched[0]?.categoryName ?? null;
}
