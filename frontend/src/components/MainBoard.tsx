import { useEffect, useState } from "react";
import type { DayView } from "../api/cashbook";
import { deleteTransaction, updateDailySheet } from "../api/cashbook";
import { fetchCategoryKeywords, type CategoryKeyword } from "../api/categoryKeywords";
import type { LedgerBook } from "../api/ledgerBook";
import { addDays, formatDayTitle, toIsoDate } from "../util/dateUtil";
import { ExpenseTable } from "./ExpenseTable";
import { SavingsTable } from "./SavingsTable";
import { useCategories } from "./CategoryDatalist";

type Props = {
  book: LedgerBook;
  date: string;
  day: DayView | null;
  loading: boolean;
  error: string | null;
  scheduleNote: string;
  onScheduleChange: (v: string) => void;
  onDateChange: (iso: string) => void;
  onReload: () => Promise<void>;
  keywordRefresh?: number;
};

export function MainBoard({
  book,
  date,
  day,
  loading,
  error,
  scheduleNote,
  onScheduleChange,
  onDateChange,
  onReload,
  keywordRefresh = 0,
}: Props) {
  const [selExp, setSelExp] = useState<Set<number>>(new Set());
  const [selInc, setSelInc] = useState<Set<number>>(new Set());
  const [selSav, setSelSav] = useState<Set<number>>(new Set());
  const [dayMemo, setDayMemo] = useState("");
  const [saving, setSaving] = useState(false);
  const [busy, setBusy] = useState(false);
  const categories = useCategories(book);
  const [categoryKeywords, setCategoryKeywords] = useState<CategoryKeyword[]>([]);
  const expenseCats = categories?.expense.map((c) => c.name) ?? [];
  const incomeCats = categories?.income.map((c) => c.name) ?? [];
  const savingsTitles = categories?.savings.map((c) => c.name) ?? [];

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

  useEffect(() => {
    if (!day || day.date !== date) return;
    onScheduleChange(day.scheduleNote ?? "");
    setDayMemo(day.dayMemo ?? "");
  }, [date, day?.date, onScheduleChange]);

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

  async function handleSave() {
    setSaving(true);
    try {
      await updateDailySheet(date, scheduleNote, dayMemo, book);
      await onReload();
    } finally {
      setSaving(false);
    }
  }

  async function handleDeleteSelected() {
    if (selectedCount === 0) return;
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
          <button type="button" className="cb-btn cb-btn--primary" disabled={saving || !day} onClick={handleSave}>
            {saving ? "저장 중…" : "저장"}
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
            categoryOptions={expenseCats}
            categoryKeywords={categoryKeywords}
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
            categoryOptions={incomeCats}
            categoryKeywords={categoryKeywords}
          />
          <SavingsTable
            book={book}
            txDate={date}
            rows={day.savings}
            selected={selSav}
            onToggle={(id, c) => toggle(setSelSav, id, c)}
            onReload={onReload}
            titleOptions={savingsTitles}
          />

          <div className="cb-bottom">
            <div className="cb-bottom__toolbar">
              <button
                type="button"
                className="cb-btn cb-btn--ghost"
                disabled={busy || selectedCount === 0}
                onClick={handleDeleteSelected}
              >
                삭제
              </button>
              <button type="button" className="cb-btn cb-btn--ghost" disabled title="추후">
                복사
              </button>
              <button type="button" className="cb-btn cb-btn--ghost" disabled title="추후">
                이동
              </button>
              <button type="button" className="cb-btn cb-btn--ghost" disabled title="추후">
                할부
              </button>
            </div>
            <div className="cb-bottom__memo">
              <textarea
                className="cb-memo"
                rows={3}
                placeholder="오늘의 메모를 입력하세요. 메모와 가계부 입력사항은 함께 저장됩니다."
                value={dayMemo}
                onChange={(e) => setDayMemo(e.target.value)}
              />
              <button type="button" className="cb-btn cb-btn--primary" disabled={saving} onClick={handleSave}>
                {saving ? "저장 중…" : "저장"}
              </button>
            </div>
          </div>
        </>
      )}
    </main>
  );
}
