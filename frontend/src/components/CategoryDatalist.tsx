import { useEffect, useState } from "react";
import { fetchCategories, type CategoryList } from "../api/categories";

import type { LedgerBook } from "../api/ledgerBook";

export function useCategories(book: LedgerBook) {
  const [categories, setCategories] = useState<CategoryList | null>(null);

  useEffect(() => {
    fetchCategories(book)
      .then(setCategories)
      .catch(() => setCategories(null));
  }, [book]);

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
