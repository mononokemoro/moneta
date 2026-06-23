import { useEffect, useState } from "react";
import { fetchCalendarMarkers } from "../api/cashbook";
import type { LedgerBook } from "../api/ledgerBook";
import { daysInMonth, parseIso, toIsoDate } from "../util/dateUtil";

type Props = {
  selected: string;
  book: LedgerBook;
  refreshKey?: number;
  onSelect: (iso: string) => void;
};

export function MiniCalendar({ selected, book, refreshKey = 0, onSelect }: Props) {
  const base = parseIso(selected);
  const [vy, setVy] = useState(base.getFullYear());
  const [vm, setVm] = useState(base.getMonth() + 1);
  const [entryDays, setEntryDays] = useState<Set<number>>(() => new Set());

  useEffect(() => {
    const d = parseIso(selected);
    setVy(d.getFullYear());
    setVm(d.getMonth() + 1);
  }, [selected]);

  const yearMonth = `${vy}-${String(vm).padStart(2, "0")}`;

  useEffect(() => {
    let cancelled = false;
    fetchCalendarMarkers(yearMonth, book)
      .then((dates) => {
        if (cancelled) return;
        const days = new Set<number>();
        for (const iso of dates) {
          const parsed = parseIso(iso);
          if (parsed.getFullYear() === vy && parsed.getMonth() + 1 === vm) {
            days.add(parsed.getDate());
          }
        }
        setEntryDays(days);
      })
      .catch(() => {
        if (!cancelled) setEntryDays(new Set());
      });
    return () => {
      cancelled = true;
    };
  }, [yearMonth, book, refreshKey]);

  const firstWeekday = new Date(vy, vm - 1, 1).getDay();
  const dim = daysInMonth(vy, vm);
  const cells: (number | null)[] = [];
  for (let i = 0; i < firstWeekday; i++) cells.push(null);
  for (let d = 1; d <= dim; d++) cells.push(d);

  function shiftMonth(delta: number) {
    const iso = toIsoDate(new Date(vy, vm - 1 + delta, 1));
    const p = parseIso(iso);
    setVy(p.getFullYear());
    setVm(p.getMonth() + 1);
  }

  function pick(day: number) {
    const iso = `${vy}-${String(vm).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
    onSelect(iso);
  }

  const selParts = selected.split("-").map(Number);

  return (
    <div className="cb-cal">
      <div className="cb-cal__hdr">
        <button type="button" className="cb-cal__nav" onClick={() => shiftMonth(-1)} aria-label="이전 달">
          ‹
        </button>
        <span className="cb-cal__title">
          {vy}.{String(vm).padStart(2, "0")}
        </span>
        <button type="button" className="cb-cal__nav" onClick={() => shiftMonth(1)} aria-label="다음 달">
          ›
        </button>
      </div>
      <div className="cb-cal__weekdays">
        {["일", "월", "화", "수", "목", "금", "토"].map((w) => (
          <span key={w}>{w}</span>
        ))}
      </div>
      <div className="cb-cal__grid">
        {cells.map((cell, i) => {
          if (cell === null) return <span key={`e-${i}`} className="cb-cal__cell cb-cal__cell--empty" />;
          const active = selParts[0] === vy && selParts[1] === vm && selParts[2] === cell;
          const hasEntry = entryDays.has(cell);
          return (
            <button
              key={cell}
              type="button"
              className={`cb-cal__cell cb-cal__cell--day${active ? " is-active" : ""}${hasEntry ? " has-entry" : ""}`}
              onClick={() => pick(cell)}
            >
              <span className="cb-cal__dayNum">{cell}</span>
            </button>
          );
        })}
      </div>
    </div>
  );
}
