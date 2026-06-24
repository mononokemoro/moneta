import { useEffect, useState } from "react";
import type { DayView } from "../api/cashbook";
import { deleteTransaction, fetchDayTransactionTable, moveTransactions } from "../api/cashbook";
import { fetchCategoryKeywords, type CategoryKeyword } from "../api/categoryKeywords";
import type { LedgerBook } from "../api/ledgerBook";
import type { TransactionTablePreview } from "../api/transactionTable";
import { addDays, formatDayTitle, toIsoDate } from "../util/dateUtil";
import { confirmDelete } from "../util/confirmDialog";
import { ExpenseTable } from "./ExpenseTable";
import { SavingsTable } from "./SavingsTable";
import { flattenSelectableCategoryNames, selectableCategoryGroups } from "../api/categories";
import { useCategories } from "./CategoryDatalist";
import { TransactionMoveDialog } from "./TransactionMoveDialog";
import { TransactionTablePreviewDialog } from "./TransactionTablePreviewDialog";

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
  const [moveOpen, setMoveOpen] = useState(false);
  const [moveError, setMoveError] = useState<string | null>(null);
  const [tablePreviewOpen, setTablePreviewOpen] = useState(false);
  const [tablePreview, setTablePreview] = useState<TransactionTablePreview | null>(null);
  const [tablePreviewBusy, setTablePreviewBusy] = useState(false);
  const [tablePreviewErr, setTablePreviewErr] = useState<string | null>(null);
  const personalCategoryList = useCategories("PERSONAL", keywordRefresh);
  const householdCategoryList = useCategories("HOUSEHOLD", keywordRefresh);
  const [categoryKeywords, setCategoryKeywords] = useState<CategoryKeyword[]>([]);
  const personalExpenseGroups = selectableCategoryGroups(personalCategoryList?.expense ?? []);
  const householdExpenseGroups = selectableCategoryGroups(householdCategoryList?.expense ?? []);
  const expenseGroups =
    book === "PERSONAL" ? personalExpenseGroups : householdExpenseGroups;
  const incomeGroups = selectableCategoryGroups(
    (book === "PERSONAL" ? personalCategoryList : householdCategoryList)?.income ?? [],
  );
  const savingsTitles = flattenSelectableCategoryNames(
    (book === "PERSONAL" ? personalCategoryList : householdCategoryList)?.savings ?? [],
  );

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
    setTablePreviewOpen(false);
    setTablePreview(null);
    setTablePreviewErr(null);
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

  function openMoveDialog() {
    if (selectedCount === 0) return;
    setMoveError(null);
    setMoveOpen(true);
  }

  async function handleMoveConfirm(targetDate: string) {
    if (selectedCount === 0) return;
    setBusy(true);
    setMoveError(null);
    try {
      await moveTransactions([...selExp, ...selInc, ...selSav], targetDate, book);
      setMoveOpen(false);
      setSelExp(new Set());
      setSelInc(new Set());
      setSelSav(new Set());
      await onReload();
    } catch (e) {
      setMoveError(e instanceof Error ? e.message : "이동 실패");
    } finally {
      setBusy(false);
    }
  }

  async function openTablePreview() {
    setTablePreviewOpen(true);
    setTablePreviewBusy(true);
    setTablePreviewErr(null);
    try {
      const table = await fetchDayTransactionTable(date, book);
      setTablePreview({
        tableName: table.tableName,
        title: table.bookLabel,
        txDate: table.txDate,
        subtitle: `${table.count}건 · 당일 가계부와 동일 집합`,
        count: table.count,
        querySql: table.querySql,
        groupByTxType: true,
        rows: table.rows,
      });
    } catch (e: unknown) {
      setTablePreviewErr(e instanceof Error ? e.message : "테이블 조회 실패");
      setTablePreview(null);
    } finally {
      setTablePreviewBusy(false);
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
          <button
            type="button"
            className="cb-btn cb-btn--ghost"
            disabled={busy || loading || tablePreviewBusy}
            onClick={() => void openTablePreview()}
          >
            {tablePreviewBusy ? "조회 중…" : "테이블 조회"}
          </button>
          <button
            type="button"
            className="cb-btn cb-btn--ghost"
            disabled={busy || selectedCount === 0}
            onClick={openMoveDialog}
          >
            이동
          </button>
          <button
            type="button"
            className="cb-btn cb-btn--ghost"
            disabled={busy || selectedCount === 0}
            onClick={handleDeleteSelected}
          >
            삭제
          </button>
        </div>
      </header>

      <TransactionMoveDialog
        open={moveOpen}
        count={selectedCount}
        currentDate={date}
        initialDate={date}
        busy={busy}
        error={moveError}
        onClose={() => !busy && setMoveOpen(false)}
        onConfirm={(targetDate) => void handleMoveConfirm(targetDate)}
      />

      <TransactionTablePreviewDialog
        open={tablePreviewOpen}
        data={tablePreview}
        busy={tablePreviewBusy}
        error={tablePreviewErr}
        onClose={() => {
          setTablePreviewOpen(false);
          setTablePreview(null);
        }}
      />

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
            householdCategoryGroups={book === "PERSONAL" ? householdExpenseGroups : undefined}
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
