import { useEffect, useRef, useState } from "react";
import type { SavingsRow } from "../api/cashbook";
import { createTransaction, updateTransaction } from "../api/cashbook";
import type { LedgerBook } from "../api/ledgerBook";
import { upsertProductBalanceAnchor } from "../api/savingsBalance";
import { formatMoney } from "../formatMoney";
import { amountToInput, formatAmountInput, parseAmount } from "../util/parseAmount";
import { buildTableDrafts } from "../util/tableDrafts";
import { ComboInput } from "./ComboInput";

type RowDraft = {
  key: string;
  id?: number;
  savingsProductId: number | null;
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
  onDirtyChange?: (dirty: boolean) => void;
};

function emptyDraft(): RowDraft {
  return {
    key: `new-${Math.random().toString(36).slice(2)}`,
    savingsProductId: null,
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
    savingsProductId: r.savingsProductId ?? null,
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

function anchorChanged(d: RowDraft, r: SavingsRow): boolean {
  return parseAmount(d.accumulated) !== r.accumulatedAmount;
}

function hasUnsavedDrafts(drafts: RowDraft[], rows: SavingsRow[]): boolean {
  return drafts.some((d) => {
    if (!d.id) return hasContent(d);
    const orig = rows.find((r) => r.id === d.id);
    return orig ? rowChanged(d, orig) : false;
  });
}

export function SavingsTable({ book, txDate, rows, selected, onToggle, onReload, titleOptions = [], onDirtyChange }: Props) {
  const [drafts, setDrafts] = useState<RowDraft[]>([]);
  const [busy, setBusy] = useState(false);
  const newRowRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    setDrafts(buildTableDrafts(rows.map(fromRow), emptyDraft));
  }, [rows, txDate]);

  const dirty = hasUnsavedDrafts(drafts, rows);
  useEffect(() => {
    onDirtyChange?.(dirty);
  }, [dirty, onDirtyChange]);

  const sum = rows.reduce((a, r) => a + (Number(r.amount) || 0), 0);

  function patch(key: string, patch: Partial<RowDraft>) {
    setDrafts((prev) => prev.map((d) => (d.key === key ? { ...d, ...patch } : d)));
  }

  async function commitAnchorIfNeeded(d: RowDraft, orig?: SavingsRow) {
    if (!orig || !anchorChanged(d, orig)) return;
    const balance = parseAmount(d.accumulated);
    await upsertProductBalanceAnchor(book, {
      productId: d.savingsProductId ?? orig.savingsProductId ?? undefined,
      title: d.title.trim() || orig.title,
      anchorDate: txDate,
      balance,
    });
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
          savingsProductId: d.savingsProductId,
          book,
        });
        if (d.accumulated.trim()) {
          await upsertProductBalanceAnchor(book, {
            title: d.title.trim() || "(미입력)",
            anchorDate: txDate,
            balance: parseAmount(d.accumulated),
          });
        }
        await onReload();
      } finally {
        setBusy(false);
      }
      return;
    }

    if (!orig) return;

    const needsTxUpdate =
      d.title !== orig.title ||
      parseAmount(d.amount) !== orig.amount ||
      d.remarks !== orig.remarks;
    const needsAnchor = anchorChanged(d, orig);

    if (!needsTxUpdate && !needsAnchor) return;

    setBusy(true);
    try {
      if (needsTxUpdate) {
        await updateTransaction(d.id, {
          title: d.title.trim() || "(미입력)",
          amount: parseAmount(d.amount),
          category: "저축",
          remarks: d.remarks,
          savingsProductId: d.savingsProductId ?? orig.savingsProductId,
        });
      }
      if (needsAnchor) {
        await commitAnchorIfNeeded(d, orig);
      }
      await onReload();
    } finally {
      setBusy(false);
    }
  }

  function handleRowBlur(d: RowDraft, e: React.FocusEvent<HTMLTableRowElement>) {
    if (e.currentTarget.contains(e.relatedTarget as Node)) return;
    void commitRow(d);
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
          </div>
        </div>
      </div>
      <div className="cb-panel__tablewrap">
        <div className="cb-panel__tablescroll">
          <table className="cb-table cb-table--inline cb-table--excel cb-table--savings">
            <colgroup>
              <col className="cb-col-check" />
              <col className="cb-col-title" />
              <col className="cb-col-amount" />
              <col className="cb-col-accumulated" />
              <col className="cb-col-remarks" />
            </colgroup>
            <thead>
              <tr>
                <th className="cb-col-check" />
                <th>항목</th>
                <th className="cb-num">금액</th>
                <th className="cb-num" title="계산값 · 편집 시 잔액 확인(기준점) 저장">
                  누적금액
                </th>
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
                  <ComboInput
                    ref={idx === drafts.length - 1 ? newRowRef : undefined}
                    className="cb-cell"
                    options={titleOptions}
                    value={d.title}
                    onChange={(v) => patch(d.key, { title: v })}
                    disabled={busy}
                  />
                </td>
                <td>
                  <input
                    className="cb-cell cb-num"
                    value={d.amount}
                    onChange={(e) => patch(d.key, { amount: formatAmountInput(e.target.value) })}
                    disabled={busy}
                  />
                </td>
                <td>
                  <input
                    className="cb-cell cb-num cb-cell--computed"
                    value={d.accumulated}
                    title="불입 합계·개설잔액·잔액 확인 기준으로 계산. 수정하면 해당일 잔액 확인으로 저장됩니다."
                    onChange={(e) => patch(d.key, { accumulated: formatAmountInput(e.target.value) })}
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
          </table>
        </div>
        <div className="cb-panel__summary">
          <span className="cb-meta">총 {rows.length}건</span>
          <span className="cb-meta cb-meta--sum">불입 합계 {formatMoney(sum)}원</span>
        </div>
      </div>
    </section>
  );
}
