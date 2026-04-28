import { useRef, useState } from "react";
import {
  createTransaction,
  deleteTransaction,
  type TransactionRow,
  type TxType,
} from "../api/cashbook";
import { formatMoney } from "../formatMoney";

type Props = {
  txDate: string;
  txType: TxType;
  variant: "expense" | "income";
  rows: TransactionRow[];
  selected: Set<number>;
  onToggle: (id: number, checked: boolean) => void;
  onReload: () => Promise<void>;
};

export function ExpenseTable({
  txDate,
  txType,
  variant,
  rows,
  selected,
  onToggle,
  onReload,
}: Props) {
  const [draft, setDraft] = useState({
    title: "",
    amount: "",
    category: "",
    cardName: "",
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
        txType,
        title: draft.title || "(미입력)",
        amount: Number(draft.amount.replace(/,/g, "")) || 0,
        category: draft.category,
        cardName: variant === "expense" ? draft.cardName : "",
        remarks: draft.remarks,
      });
      setDraft({ title: "", amount: "", category: "", cardName: "", remarks: "" });
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

  const hdrCls =
    variant === "expense" ? "cb-th cb-th--expense" : "cb-th cb-th--income";
  const title =
    variant === "expense" ? "지출내역" : "수입내역";
  const icon = variant === "expense" ? "↓" : "↑";

  function focusAddRow() {
    const firstInput = addFormRef.current?.querySelector("input");
    firstInput?.focus();
  }

  return (
    <section className="cb-panel">
      <div className={`cb-panel__head ${hdrCls}`}>
        <div className="cb-panel__headInner">
          <div className="cb-panel__title">
            <span className="cb-panel__icon">{icon}</span>
            <span>{title}</span>
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
              <th className="cb-num">금액</th>
              <th>분류</th>
              {variant === "expense" && <th>카드명</th>}
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
                <td>{r.category}</td>
                {variant === "expense" && <td>{r.cardName}</td>}
                <td>{r.remarks}</td>
              </tr>
            ))}
          </tbody>
          <tfoot>
            <tr className="cb-tfoot">
              <td colSpan={variant === "expense" ? 6 : 5}>
                <span className="cb-meta">{rows.length}건</span>
                <span className="cb-meta cb-meta--sum">합계 {formatMoney(sum)}원</span>
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
          placeholder="금액"
          className="cb-num"
          value={draft.amount}
          onChange={(e) => setDraft((d) => ({ ...d, amount: e.target.value }))}
        />
        <input
          placeholder="분류"
          value={draft.category}
          onChange={(e) => setDraft((d) => ({ ...d, category: e.target.value }))}
        />
        {variant === "expense" && (
          <input
            placeholder="카드명"
            value={draft.cardName}
            onChange={(e) => setDraft((d) => ({ ...d, cardName: e.target.value }))}
          />
        )}
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
