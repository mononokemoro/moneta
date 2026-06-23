import { useEffect, useState } from "react";
import type { DayView } from "../api/cashbook";
import { deleteTransaction } from "../api/cashbook";
import { fetchCategoryKeywords, type CategoryKeyword } from "../api/categoryKeywords";
import type { LedgerBook } from "../api/ledgerBook";
import { addDays, formatDayTitle, toIsoDate } from "../util/dateUtil";
import { confirmDelete } from "../util/confirmDialog";
import { ExpenseTable } from "./ExpenseTable";
import { SavingsTable } from "./SavingsTable";
import { flattenSelectableCategoryNames } from "../api/categories";
import { useCategories } from "./CategoryDatalist";

type Props = {
  book: LedgerBook;
  date: string;
  day: DayView | null;
  loading: boolean;
  error: string | null;
  onDateChange: (iso: string) => void;
  onReload: () => Promise<void>;
  keywordRefresh?: number;
  onUnsavedChange?: (dirty: boolean) => void;
};

export function MainBoard({
  book,
  date,
  day,
  loading,
  error,
  onDateChange,
  onReload,
  keywordRefresh = 0,
  onUnsavedChange,
}: Props) {
  const [selExp, setSelExp] = useState<Set<number>>(new Set());
  const [selInc, setSelInc] = useState<Set<number>>(new Set());
  const [selSav, setSelSav] = useState<Set<number>>(new Set());
  const [dirtyExp, setDirtyExp] = useState(false);
  const [dirtyInc, setDirtyInc] = useState(false);
  const [dirtySav, setDirtySav] = useState(false);
  const [busy, setBusy] = useState(false);
  const categories = useCategories(book, keywordRefresh);
  const [categoryKeywords, setCategoryKeywords] = useState<CategoryKeyword[]>([]);
  const expenseGroups = categories?.expense ?? [];
  const incomeGroups = categories?.income ?? [];
  const savingsTitles = categories ? flattenSelectableCategoryNames(categories.savings) : [];

  useEffect(() => {
    let cancelled = false;
    fetchCategoryKeywords(book)
      .then((rows) => {
        if (!cancelled) setCategoryKeywords(rows);
      })
      .catch(() => {
        if (!cancelled) setCategoryKeywords([]);
      });
    return () => {
      cancelled = true;
    };
  }, [book, keywordRefresh]);

  useEffect(() => {
    setSelExp(new Set());
    setSelInc(new Set());
    setSelSav(new Set());
  }, [date, book]);

  const tablesDirty = dirtyExp || dirtyInc || dirtySav;
  useEffect(() => {
    onUnsavedChange?.(tablesDirty);
    return () => onUnsavedChange?.(false);
  }, [tablesDirty, onUnsavedChange]);

  function toggle(setter: React.Dispatch<React.SetStateAction<Set<number>>>, id: number, checked: boolean) {
    setter((prev) => {
      const n = new Set(prev);
      if (checked) n.add(id);
      else n.delete(id);
      return n;
    });
  }

  const title = formatDayTitle(date);
  const selectedCount = selExp.size + selInc.size + selSav.size;

  async function handleDeleteSelected() {
    if (selectedCount === 0) return;
    if (!confirmDelete(selectedCount)) return;
    setBusy(true);
    try {
      for (const id of [...selExp, ...selInc, ...selSav]) {
        await deleteTransaction(id);
      }
      await onReload();
    } finally {
      setBusy(false);
    }
  }

  return (
    <main className="cb-main">
      <header className="cb-main__hdr">
        <div className="cb-main__leftTools">
          <h1 className="cb-main__title">{title}</h1>
          <button type="button" className="cb-chip" onClick={() => onDateChange(toIsoDate(new Date()))}>
            오늘
          </button>
          <button type="button" className="cb-iconBtn" onClick={() => onDateChange(addDays(date, -1))} aria-label="이전">
            ‹
          </button>
          <button type="button" className="cb-iconBtn" onClick={() => onDateChange(addDays(date, 1))} aria-label="다음">
            ›
          </button>
        </div>
        <div className="cb-main__actions">
          <button type="button" className="cb-btn cb-btn--ghost" disabled title="추후">
            엑셀 ▼
          </button>
          <button type="button" className="cb-iconBtn" onClick={() => window.print()} aria-label="인쇄">
            🖨
          </button>
          <button type="button" className="cb-btn cb-btn--ghost" disabled title="추후">
            할부
          </button>
          <button type="button" className="cb-btn cb-btn--ghost" disabled title="추후">
            이동
          </button>
          <button type="button" className="cb-btn cb-btn--ghost" disabled title="추후">
            복사
          </button>
          <button
            type="button"
            className="cb-btn cb-btn--ghost"
            disabled={busy || selectedCount === 0}
            onClick={handleDeleteSelected}
          >
            삭제
          </button>
          <button type="button" className="cb-btn cb-btn--primary" disabled title="추후">
            저장
          </button>
        </div>
      </header>

      {loading && <p className="cb-muted">불러오는 중…</p>}
      {error && <p className="cb-err">{error}</p>}

      {day && !loading && (
        <>
          <ExpenseTable
            book={book}
            txDate={date}
            txType="EXPENSE"
            variant="expense"
            rows={day.expenses}
            selected={selExp}
            onToggle={(id, c) => toggle(setSelExp, id, c)}
            onReload={onReload}
            categoryGroups={expenseGroups}
            categoryKeywords={categoryKeywords}
            onDirtyChange={setDirtyExp}
          />
          <ExpenseTable
            book={book}
            txDate={date}
            txType="INCOME"
            variant="income"
            rows={day.incomes}
            selected={selInc}
            onToggle={(id, c) => toggle(setSelInc, id, c)}
            onReload={onReload}
            categoryGroups={incomeGroups}
            categoryKeywords={categoryKeywords}
            onDirtyChange={setDirtyInc}
          />
          <SavingsTable
            book={book}
            txDate={date}
            rows={day.savings}
            selected={selSav}
            onToggle={(id, c) => toggle(setSelSav, id, c)}
            onReload={onReload}
            titleOptions={savingsTitles}
            onDirtyChange={setDirtySav}
          />
        </>
      )}
    </main>
  );
}
