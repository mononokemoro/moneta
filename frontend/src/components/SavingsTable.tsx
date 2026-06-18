import { useEffect, useRef, useState } from "react";
import type { SavingsRow } from "../api/cashbook";
import { createTransaction, deleteTransaction, updateTransaction } from "../api/cashbook";
import type { LedgerBook } from "../api/ledgerBook";
import { formatMoney } from "../formatMoney";
import { amountToInput, parseAmount } from "../util/parseAmount";
import { buildTableDrafts } from "../util/tableDrafts";
import { CategoryDatalist } from "./CategoryDatalist";

type RowDraft = {
  key: string;
  id?: number;
  title: string;
  amount: string;
  accumulated: string;
  remarks: string;
};

type Props = {
  book: LedgerBook;
  txDate: string;
  rows: SavingsRow[];
  selected: Set<number>;
  onToggle: (id: number, checked: boolean) => void;
  onReload: () => Promise<void>;
  titleOptions?: string[];
};

function emptyDraft(): RowDraft {
  return {
    key: `new-${Math.random().toString(36).slice(2)}`,
    title: "",
    amount: "",
    accumulated: "",
    remarks: "",
  };
}

function fromRow(r: SavingsRow): RowDraft {
  return {
    key: String(r.id),
    id: r.id,
    title: r.title,
    amount: amountToInput(r.amount),
    accumulated: amountToInput(r.accumulatedAmount),
    remarks: r.remarks,
  };
}

function hasContent(d: RowDraft): boolean {
  return !!(d.title.trim() || d.amount.trim() || d.accumulated.trim() || d.remarks.trim());
}

function rowChanged(d: RowDraft, r: SavingsRow): boolean {
  return (
    d.title !== r.title ||
    parseAmount(d.amount) !== r.amount ||
    parseAmount(d.accumulated) !== r.accumulatedAmount ||
    d.remarks !== r.remarks
  );
}

export function SavingsTable({ book, txDate, rows, selected, onToggle, onReload, titleOptions = [] }: Props) {
  const [drafts, setDrafts] = useState<RowDraft[]>([]);
  const [busy, setBusy] = useState(false);
  const newRowRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    setDrafts(buildTableDrafts(rows.map(fromRow), emptyDraft));
  }, [rows, txDate]);

  const sum = rows.reduce((a, r) => a + (Number(r.amount) || 0), 0);

  function patch(key: string, patch: Partial<RowDraft>) {
    setDrafts((prev) => prev.map((d) => (d.key === key ? { ...d, ...patch } : d)));
  }

  async function commitRow(d: RowDraft) {
    if (busy) return;
    const orig = d.id ? rows.find((r) => r.id === d.id) : undefined;

    if (!d.id) {
      if (!hasContent(d)) return;
      setBusy(true);
      try {
        await createTransaction({
          txDate,
          txType: "SAVINGS",
          title: d.title.trim() || "(미입력)",
          amount: parseAmount(d.amount),
          category: "저축",
          remarks: d.remarks,
          accumulatedAmount: parseAmount(d.accumulated),
          book,
        });
        await onReload();
      } finally {
        setBusy(false);
      }
      return;
    }

    if (!orig || !rowChanged(d, orig)) return;
    setBusy(true);
    try {
      await updateTransaction(d.id, {
        title: d.title.trim() || "(미입력)",
        amount: parseAmount(d.amount),
        category: "저축",
        remarks: d.remarks,
        accumulatedAmount: parseAmount(d.accumulated),
      });
      await onReload();
    } finally {
      setBusy(false);
    }
  }

  function handleRowBlur(d: RowDraft, e: React.FocusEvent<HTMLTableRowElement>) {
    if (e.currentTarget.contains(e.relatedTarget as Node)) return;
    void commitRow(d);
  }

  async function handleDeleteSelected() {
    if (selected.size === 0) return;
    setBusy(true);
    try {
      for (const id of selected) {
        await deleteTransaction(id);
      }
      await onReload();
    } finally {
      setBusy(false);
    }
  }

  function focusNewRow() {
    newRowRef.current?.focus();
  }

  return (
    <section className="cb-panel cb-panel--excel">
      <div className="cb-panel__head cb-th cb-th--savings">
        <div className="cb-panel__headInner">
          <div className="cb-panel__title">
            <span className="cb-panel__icon">⌂</span>
            <span>저축 / 보험</span>
          </div>
          <div className="cb-panel__headActions">
            <button type="button" className="cb-panel__headBtn" onClick={focusNewRow}>
              + 행추가
            </button>
            <button
              type="button"
              className="cb-panel__headBtn"
              disabled={busy || selected.size === 0}
              onClick={handleDeleteSelected}
            >
              선택 삭제
            </button>
          </div>
        </div>
      </div>
      <div className="cb-panel__tablewrap">
        <CategoryDatalist id="sav-titles" options={titleOptions} />
        <table className="cb-table cb-table--inline cb-table--excel">
          <thead>
            <tr>
              <th className="cb-col-check" />
              <th>항목</th>
              <th className="cb-num">불입금액</th>
              <th className="cb-num">누적금액</th>
              <th>비고</th>
            </tr>
          </thead>
          <tbody>
            {drafts.map((d, idx) => (
              <tr
                key={d.key}
                className={d.id ? "cb-row--saved" : "cb-row--new"}
                onBlur={(e) => handleRowBlur(d, e)}
              >
                <td>
                  {d.id ? (
                    <input
                      type="checkbox"
                      checked={selected.has(d.id)}
                      onChange={(e) => onToggle(d.id!, e.target.checked)}
                    />
                  ) : null}
                </td>
                <td>
                  <input
                    ref={idx === drafts.length - 1 ? newRowRef : undefined}
                    className="cb-cell"
                    list={titleOptions.length > 0 ? "sav-titles" : undefined}
                    value={d.title}
                    onChange={(e) => patch(d.key, { title: e.target.value })}
                    disabled={busy}
                  />
                </td>
                <td>
                  <input
                    className="cb-cell cb-num"
                    value={d.amount}
                    onChange={(e) => patch(d.key, { amount: e.target.value })}
                    disabled={busy}
                  />
                </td>
                <td>
                  <input
                    className="cb-cell cb-num"
                    value={d.accumulated}
                    onChange={(e) => patch(d.key, { accumulated: e.target.value })}
                    disabled={busy}
                  />
                </td>
                <td>
                  <input
                    className="cb-cell"
                    value={d.remarks}
                    onChange={(e) => patch(d.key, { remarks: e.target.value })}
                    disabled={busy}
                  />
                </td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="cb-tfoot">
              <td colSpan={5}>
                <span className="cb-meta">총 {rows.length}건</span>
                <span className="cb-meta cb-meta--sum">불입 합계 {formatMoney(sum)}원</span>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
  );
}
