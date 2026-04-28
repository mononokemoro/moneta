import { useEffect, useState } from "react";
import type { DayView } from "../api/cashbook";
import { formatDayTitle } from "../util/dateUtil";
import { ExpenseTable } from "./ExpenseTable";
import { SavingsTable } from "./SavingsTable";

type Props = {
  date: string;
  day: DayView | null;
  loading: boolean;
  error: string | null;
  onReload: () => Promise<void>;
};

export function MainBoard({ date, day, loading, error, onReload }: Props) {
  const [selExp, setSelExp] = useState<Set<number>>(new Set());
  const [selInc, setSelInc] = useState<Set<number>>(new Set());
  const [selSav, setSelSav] = useState<Set<number>>(new Set());

  useEffect(() => {
    setSelExp(new Set());
    setSelInc(new Set());
    setSelSav(new Set());
  }, [date]);

  function toggle(setter: React.Dispatch<React.SetStateAction<Set<number>>>, id: number, checked: boolean) {
    setter((prev) => {
      const n = new Set(prev);
      if (checked) n.add(id);
      else n.delete(id);
      return n;
    });
  }

  const title = formatDayTitle(date);

  return (
    <main className="cb-main">
      <header className="cb-main__hdr">
        <div className="cb-main__leftTools">
          <h1 className="cb-main__title">{title}</h1>
          <button type="button" className="cb-chip">
            오늘
          </button>
          <button type="button" className="cb-iconBtn" aria-label="이전">
            ‹
          </button>
          <button type="button" className="cb-iconBtn" aria-label="다음">
            ›
          </button>
          <button type="button" className="cb-iconBtn" aria-label="달력">
            🗓
          </button>
        </div>
        <div className="cb-main__actions">
          <button type="button" className="cb-btn cb-btn--ghost">
            엑셀 ▼
          </button>
          <button type="button" className="cb-iconBtn" onClick={() => window.print()} aria-label="인쇄">
            🖨
          </button>
          <button type="button" className="cb-btn cb-btn--primary">
            저장
          </button>
        </div>
      </header>

      {loading && <p className="cb-muted">불러오는 중…</p>}
      {error && <p className="cb-err">{error}</p>}

      {day && !loading && (
        <>
          <ExpenseTable
            txDate={date}
            txType="EXPENSE"
            variant="expense"
            rows={day.expenses}
            selected={selExp}
            onToggle={(id, c) => toggle(setSelExp, id, c)}
            onReload={onReload}
          />
          <ExpenseTable
            txDate={date}
            txType="INCOME"
            variant="income"
            rows={day.incomes}
            selected={selInc}
            onToggle={(id, c) => toggle(setSelInc, id, c)}
            onReload={onReload}
          />
          <SavingsTable
            txDate={date}
            rows={day.savings}
            selected={selSav}
            onToggle={(id, c) => toggle(setSelSav, id, c)}
            onReload={onReload}
          />

          <div className="cb-bottom">
            <div className="cb-bottom__dummy">
              <button type="button" className="cb-btn cb-btn--ghost" disabled>
                복사
              </button>
              <button type="button" className="cb-btn cb-btn--ghost" disabled>
                이동
              </button>
              <button type="button" className="cb-btn cb-btn--ghost" disabled>
                할부
              </button>
            </div>
          </div>
        </>
      )}
    </main>
  );
}
