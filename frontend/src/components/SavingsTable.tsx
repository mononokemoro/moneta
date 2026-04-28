import { useRef, useState } from "react";
import type { SavingsRow } from "../api/cashbook";
import { createTransaction, deleteTransaction } from "../api/cashbook";
import { formatMoney } from "../formatMoney";

type Props = {
  txDate: string;
  rows: SavingsRow[];
  selected: Set<number>;
  onToggle: (id: number, checked: boolean) => void;
  onReload: () => Promise<void>;
};

export function SavingsTable({ txDate, rows, selected, onToggle, onReload }: Props) {
  const [draft, setDraft] = useState({
    title: "",
    amount: "",
    accumulated: "",
    remarks: "",
  });
  const [busy, setBusy] = useState(false);
  const addFormRef = useRef<HTMLFormElement | null>(null);

  const sum = rows.reduce((a, r) => a + (Number(r.amount) || 0), 0);

  async function handleAdd(e: React.FormEvent) {
    e.preventDefault();
    setBusy(true);
    try {
      await createTransaction({
        txDate,
        txType: "SAVINGS",
        title: draft.title || "(미입력)",
        amount: Number(draft.amount.replace(/,/g, "")) || 0,
        category: "저축",
        remarks: draft.remarks,
        accumulatedAmount: Number(draft.accumulated.replace(/,/g, "")) || 0,
      });
      setDraft({ title: "", amount: "", accumulated: "", remarks: "" });
      await onReload();
    } finally {
      setBusy(false);
    }
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

  function focusAddRow() {
    const firstInput = addFormRef.current?.querySelector("input");
    firstInput?.focus();
  }

  return (
    <section className="cb-panel">
      <div className="cb-panel__head cb-th cb-th--savings">
        <div className="cb-panel__headInner">
          <div className="cb-panel__title">
            <span className="cb-panel__icon">⌂</span>
            <span>저축 / 보험</span>
          </div>
          <div className="cb-panel__headActions">
            <button type="button" className="cb-panel__headBtn" onClick={focusAddRow}>
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
        <table className="cb-table">
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
            {rows.map((r) => (
              <tr key={r.id}>
                <td>
                  <input
                    type="checkbox"
                    checked={selected.has(r.id)}
                    onChange={(e) => onToggle(r.id, e.target.checked)}
                  />
                </td>
                <td>{r.title}</td>
                <td className="cb-num">{formatMoney(r.amount)}</td>
                <td className="cb-num">{formatMoney(r.accumulatedAmount)}</td>
                <td>{r.remarks}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="cb-tfoot">
              <td colSpan={5}>
                <span className="cb-meta">{rows.length}건</span>
                <span className="cb-meta cb-meta--sum">불입 합계 {formatMoney(sum)}원</span>
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
      <form ref={addFormRef} className="cb-addrow" onSubmit={handleAdd}>
        <span className="cb-addrow__label">행 추가</span>
        <input
          placeholder="항목"
          value={draft.title}
          onChange={(e) => setDraft((d) => ({ ...d, title: e.target.value }))}
        />
        <input
          placeholder="불입금액"
          className="cb-num"
          value={draft.amount}
          onChange={(e) => setDraft((d) => ({ ...d, amount: e.target.value }))}
        />
        <input
          placeholder="누적금액"
          className="cb-num"
          value={draft.accumulated}
          onChange={(e) => setDraft((d) => ({ ...d, accumulated: e.target.value }))}
        />
        <input
          placeholder="비고"
          value={draft.remarks}
          onChange={(e) => setDraft((d) => ({ ...d, remarks: e.target.value }))}
        />
        <button type="submit" className="cb-btn cb-btn--primary" disabled={busy}>
          등록
        </button>
      </form>
    </section>
  );
}
