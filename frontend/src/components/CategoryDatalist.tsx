import { useEffect, useState } from "react";
import { clearCategoryCache, fetchCategories, type CategoryList } from "../api/categories";

import type { LedgerBook } from "../api/ledgerBook";

export function useCategories(book: LedgerBook, refreshKey = 0) {
  const [categories, setCategories] = useState<CategoryList | null>(null);

  useEffect(() => {
    let cancelled = false;
    if (refreshKey > 0) clearCategoryCache(book);
    fetchCategories(book)
      .then((data) => {
        if (!cancelled) setCategories(data);
      })
      .catch(() => {
        if (!cancelled) setCategories(null);
      });
    return () => {
      cancelled = true;
    };
  }, [book, refreshKey]);

  return categories;
}

type DatalistProps = {
  id: string;
  options: string[];
};

export function CategoryDatalist({ id, options }: DatalistProps) {
  if (options.length === 0) return null;
  return (
    <datalist id={id}>
      {options.map((o) => (
        <option key={o} value={o} />
      ))}
    </datalist>
  );
}
