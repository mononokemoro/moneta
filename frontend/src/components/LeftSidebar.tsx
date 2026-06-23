import { useEffect, useState } from "react";
import type { DayView } from "../api/cashbook";
import type { LedgerBook } from "../api/ledgerBook";
import { updateBudget, updateCashBalance } from "../api/cashbook";
import { formatMoney } from "../formatMoney";
import { amountToInput, formatAmountInput, parseAmount } from "../util/parseAmount";
import { BookSwitcher } from "./BookSwitcher";
import { FixedItemsCard } from "./FixedItemsCard";
import { MiniCalendar } from "./MiniCalendar";

type Props = {
  book: LedgerBook;
  onBookChange: (book: LedgerBook) => void;
  date: string;
  day: DayView | null;
  onSelectDate: (iso: string) => void;
  onReload: () => Promise<void>;
  calendarRefresh?: number;
  onDirtyChange?: (dirty: boolean) => void;
  onOpenFixedSettings?: () => void;
};

export function LeftSidebar({
  book,
  onBookChange,
  date,
  day,
  onSelectDate,
  onReload,
  calendarRefresh = 0,
  onDirtyChange,
  onOpenFixedSettings,
}: Props) {
  const [budgetDraft, setBudgetDraft] = useState("");
  const [cashDraft, setCashDraft] = useState("");
  const [fixedDirty, setFixedDirty] = useState(false);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!day) return;
    setBudgetDraft(amountToInput(day.budget.totalBudget));
    setCashDraft(amountToInput(day.cashBalance));
  }, [day?.budget.totalBudget, day?.cashBalance, day?.yearMonth]);

  const budgetDirty = day != null && parseAmount(budgetDraft) !== day.budget.totalBudget;
  const cashDirty = day != null && parseAmount(cashDraft) !== day.cashBalance;
  const sidebarDirty = budgetDirty || cashDirty || fixedDirty;
  useEffect(() => {
    onDirtyChange?.(sidebarDirty);
    return () => onDirtyChange?.(false);
  }, [sidebarDirty, onDirtyChange]);

  const budget = day?.budget;
  const spent = budget?.spentInMonth ?? 0;
  const total = budget?.totalBudget ?? 0;
  const remaining = budget?.remainingBudget ?? 0;
  const pct = total > 0 ? Math.min(100, (spent / total) * 100) : 0;

  async function saveBudget() {
    if (!day) return;
    const v = Number(budgetDraft.replace(/,/g, "")) || 0;
    if (v === day.budget.totalBudget) return;
    setSaving(true);
    try {
      await updateBudget(day.yearMonth, v, book);
      await onReload();
    } finally {
      setSaving(false);
    }
  }

  async function saveCash() {
    if (!day) return;
    const v = Number(cashDraft.replace(/,/g, "")) || 0;
    if (v === day.cashBalance) return;
    setSaving(true);
    try {
      await updateCashBalance(v, book);
      await onReload();
    } finally {
      setSaving(false);
    }
  }

  const dayNum = date.split("-")[2];

  return (
    <aside className="cb-side cb-side--left">
      <div className="cb-side__top">
        <BookSwitcher book={book} onChange={onBookChange} />

        <MiniCalendar
          selected={date}
          book={book}
          refreshKey={calendarRefresh}
          onSelect={onSelectDate}
        />

        <section className="cb-card">
          <h3 className="cb-card__title">
            이달의 지출예산 {budget ? `(${budget.periodLabel})` : ""}
          </h3>
          <label className="cb-field">
            총예산
            <input
              className="cb-num"
              value={budgetDraft}
              onChange={(e) => setBudgetDraft(formatAmountInput(e.target.value))}
              onBlur={saveBudget}
              disabled={!day || saving}
            />
          </label>
          <div className="cb-stat">
            <span>남은 예산</span>
            <strong className={remaining < 0 ? "cb-stat--warn" : ""}>{formatMoney(remaining)}</strong>
          </div>
          <div className="cb-progress" aria-hidden>
            <span className="cb-progress__bar" style={{ width: `${pct}%` }} />
          </div>
        </section>

        <section className="cb-card">
          <h3 className="cb-card__title">현금잔액 ({dayNum})</h3>
          <label className="cb-field">
            금액
            <input
              className="cb-num"
              value={cashDraft}
              onChange={(e) => setCashDraft(formatAmountInput(e.target.value))}
              onBlur={saveCash}
              disabled={!day || saving}
            />
          </label>
        </section>
      </div>

      <FixedItemsCard
        book={book}
        txDate={date}
        items={day?.fixedItems ?? []}
        disabled={!day}
        onReload={onReload}
        onDirtyChange={setFixedDirty}
        onOpenFixedSettings={onOpenFixedSettings}
      />
    </aside>
  );
}
