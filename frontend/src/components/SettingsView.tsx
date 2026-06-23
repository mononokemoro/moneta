import { useEffect, useState } from "react";

import {
  clearCategoryCache,
  fetchCategories,
  flattenSelectableCategoryNames,
  type CategoryList,
} from "../api/categories";
import type { LedgerBook } from "../api/ledgerBook";
import { BookSwitcher } from "./BookSwitcher";
import { CategoryManagementPanel } from "./CategoryManagementPanel";
import { KeywordSettingsPanel } from "./KeywordSettingsPanel";
import { FixedRegistrationPanel } from "./FixedRegistrationPanel";
import { ProductManagementPanel } from "./ProductManagementPanel";

export type SettingsSection = "categories" | "products" | "keywords" | "fixed";

const SETTINGS_SECTIONS: { id: SettingsSection; label: string }[] = [
  { id: "categories", label: "분류항목관리" },
  { id: "products", label: "상품관리" },
  { id: "keywords", label: "예약어설정" },
  { id: "fixed", label: "고정등록" },
];

type Props = {
  book: LedgerBook;
  onBookChange: (book: LedgerBook) => void;
  section?: SettingsSection;
  onSectionChange?: (section: SettingsSection) => void;
};

export function SettingsView({ book, onBookChange, section: sectionProp, onSectionChange }: Props) {
  const [sectionState, setSectionState] = useState<SettingsSection>("categories");
  const section = sectionProp ?? sectionState;

  function setSection(next: SettingsSection) {
    onSectionChange?.(next);
    if (sectionProp == null) setSectionState(next);
  }
  const [categories, setCategories] = useState<CategoryList | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  const reload = async () => {
    clearCategoryCache(book);
    const cats = await fetchCategories(book);
    setCategories(cats);
    setErr(null);
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    reload()
      .catch((e: unknown) => {
        if (!cancelled) setErr(e instanceof Error ? e.message : "조회 실패");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [book]);

  const expenseNames = categories ? flattenSelectableCategoryNames(categories.expense) : [];
  const incomeNames = categories ? flattenSelectableCategoryNames(categories.income) : [];

  return (
    <main className="cb-settings">
      <header className="cb-settings__head">
        <div className="cb-settings__headStart">
          <nav className="cb-settings__primaryTabs" role="tablist" aria-label="설정 메뉴">
            {SETTINGS_SECTIONS.map(({ id, label }) => (
              <button
                key={id}
                type="button"
                role="tab"
                aria-selected={section === id}
                className={section === id ? "is-active" : ""}
                onClick={() => setSection(id)}
              >
                {label}
              </button>
            ))}
          </nav>
        </div>
        <BookSwitcher book={book} onChange={onBookChange} />
      </header>

      {loading && <p className="cb-muted">불러오는 중…</p>}
      {err && <p className="cb-err">{err}</p>}

      {!loading && !err && categories && (
        <div className="cb-settings__panel" role="tabpanel">
          {section === "categories" && (
            <CategoryManagementPanel book={book} categories={categories} onReload={reload} />
          )}
          {section === "products" && <ProductManagementPanel book={book} />}
          {section === "keywords" && (
            <KeywordSettingsPanel
              book={book}
              expenseCategories={expenseNames}
              incomeCategories={incomeNames}
            />
          )}
          {section === "fixed" && <FixedRegistrationPanel book={book} categories={categories} />}
        </div>
      )}
    </main>
  );
}
