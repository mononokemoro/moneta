import { useEffect, useState } from "react";
import type { DayView } from "../api/cashbook";
import type { LedgerBook } from "../api/ledgerBook";
import { updateBudget, updateCashBalance } from "../api/cashbook";
import { formatMoney } from "../formatMoney";
import { formatVisitDate } from "../util/dateUtil";
import { BookSwitcher } from "./BookSwitcher";
import { FixedItemsCard } from "./FixedItemsCard";
import { MiniCalendar } from "./MiniCalendar";

type Props = {
  book: LedgerBook;
  onBookChange: (book: LedgerBook) => void;
  date: string;
  day: DayView | null;
  scheduleNote: string;
  onScheduleChange: (v: string) => void;
  onSelectDate: (iso: string) => void;
  onReload: () => Promise<void>;
};

export function LeftSidebar({
  book,
  onBookChange,
  date,
  day,
  scheduleNote,
  onScheduleChange,
  onSelectDate,
  onReload,
}: Props) {
  const [budgetDraft, setBudgetDraft] = useState("");
  const [cashDraft, setCashDraft] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!day) return;
    setBudgetDraft(String(day.budget.totalBudget));
    setCashDraft(String(day.cashBalance));
  }, [day?.budget.totalBudget, day?.cashBalance, day?.yearMonth]);

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
      <BookSwitcher book={book} onChange={onBookChange} />
      <div className="cb-profile">
        <div className="cb-profile__avatar" aria-hidden />
        <div className="cb-profile__txt">
          <div className="cb-profile__name">미니가계부</div>
          <div className="cb-profile__sub">
            {budget ? (
              <>
                결산일 {budget.periodLabel}
                <br />
                방문일 {formatVisitDate(date)}
              </>
            ) : (
              "Cashbook"
            )}
          </div>
        </div>
      </div>

      <MiniCalendar selected={date} onSelect={onSelectDate} />

      <section className="cb-card">
        <h3 className="cb-card__title">
          이달의 지출예산 {budget ? `(${budget.periodLabel})` : ""}
        </h3>
        <label className="cb-field">
          총예산
          <input
            className="cb-num"
            value={budgetDraft}
            onChange={(e) => setBudgetDraft(e.target.value)}
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
            onChange={(e) => setCashDraft(e.target.value)}
            onBlur={saveCash}
            disabled={!day || saving}
          />
        </label>
      </section>

      <section className="cb-card">
        <h3 className="cb-card__title">오늘의 일정</h3>
        <textarea
          className="cb-schedule"
          rows={3}
          placeholder="오늘의 일정을 등록하세요."
          value={scheduleNote}
          onChange={(e) => onScheduleChange(e.target.value)}
          disabled={!day}
        />
      </section>

      <FixedItemsCard
        book={book}
        txDate={date}
        items={day?.fixedItems ?? []}
        disabled={!day}
        onReload={onReload}
      />
    </aside>
  );
}
