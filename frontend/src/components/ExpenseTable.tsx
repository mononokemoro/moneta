import { useEffect, useRef, useState } from "react";
import type { CategoryKeyword } from "../api/categoryKeywords";
import {
  createTransaction,
  updateTransaction,
  type TransactionRow,
  type TxType,
} from "../api/cashbook";
import type { LedgerBook } from "../api/ledgerBook";
import { formatMoney } from "../formatMoney";
import { amountToInput, formatAmountInput, parseAmount } from "../util/parseAmount";
import { buildTableDrafts } from "../util/tableDrafts";
import { matchCategoryByKeyword } from "../util/matchCategoryKeyword";
import type { CategoryGroup } from "../api/categories";
import { CategoryPicker } from "./CategoryPicker";

type RowDraft = {
  key: string;
  id?: number;
  title: string;
  amount: string;
  category: string;
  cardName: string;
  remarks: string;
};

type Props = {
  book: LedgerBook;
  txDate: string;
  txType: TxType;
  variant: "expense" | "income";
  rows: TransactionRow[];
  selected: Set<number>;
  onToggle: (id: number, checked: boolean) => void;
  onReload: () => Promise<void>;
  categoryGroups?: CategoryGroup[];
  categoryKeywords?: CategoryKeyword[];
  onDirtyChange?: (dirty: boolean) => void;
};

function emptyDraft(): RowDraft {
  return {
    key: `new-${Math.random().toString(36).slice(2)}`,
    title: "",
    amount: "",
    category: "",
    cardName: "",
    remarks: "",
  };
}

function fromRow(r: TransactionRow): RowDraft {
  return {
    key: String(r.id),
    id: r.id,
    title: r.title,
    amount: amountToInput(r.amount),
    category: r.category,
    cardName: r.cardName,
    remarks: r.remarks,
  };
}

function hasContent(d: RowDraft, withCard: boolean): boolean {
  return !!(
    d.title.trim() ||
    d.amount.trim() ||
    d.category.trim() ||
    d.remarks.trim() ||
    (withCard && d.cardName.trim())
  );
}

function rowChanged(d: RowDraft, r: TransactionRow): boolean {
  return (
    d.title !== r.title ||
    parseAmount(d.amount) !== r.amount ||
    d.category !== r.category ||
    d.cardName !== r.cardName ||
    d.remarks !== r.remarks
  );
}

function hasUnsavedDrafts(
  drafts: RowDraft[],
  rows: TransactionRow[],
  withCard: boolean,
): boolean {
  return drafts.some((d) => {
    if (!d.id) return hasContent(d, withCard);
    const orig = rows.find((r) => r.id === d.id);
    return orig ? rowChanged(d, orig) : false;
  });
}

export function ExpenseTable({
  book,
  txDate,
  txType,
  variant,
  rows,
  selected,
  onToggle,
  onReload,
  categoryGroups = [],
  categoryKeywords = [],
  onDirtyChange,
}: Props) {
  const [drafts, setDrafts] = useState<RowDraft[]>([]);
  const [busy, setBusy] = useState(false);
  const newRowRef = useRef<HTMLInputElement | null>(null);
  const withCard = variant === "expense";

  useEffect(() => {
    setDrafts(buildTableDrafts(rows.map(fromRow), emptyDraft));
  }, [rows, txDate]);

  const dirty = hasUnsavedDrafts(drafts, rows, withCard);
  useEffect(() => {
    onDirtyChange?.(dirty);
  }, [dirty, onDirtyChange]);

  const sum = rows.reduce((a, r) => a + (Number(r.amount) || 0), 0);
  const cashSum = rows
    .filter((r) => !r.cardName?.trim())
    .reduce((a, r) => a + (Number(r.amount) || 0), 0);
  const cardSum = sum - cashSum;

  function patchRow(key: string, updates: Partial<RowDraft>) {
    setDrafts((prev) => prev.map((d) => (d.key === key ? { ...d, ...updates } : d)));
  }

  function handleTitleChange(d: RowDraft, title: string) {
    const updates: Partial<RowDraft> = { title };
    const matched = matchCategoryByKeyword(title, categoryKeywords, txType);
    if (matched) updates.category = matched;
    patchRow(d.key, updates);
  }

  async function commitRow(d: RowDraft) {
    if (busy) return;
    const orig = d.id ? rows.find((r) => r.id === d.id) : undefined;

    if (!d.id) {
      if (!hasContent(d, withCard)) return;
      setBusy(true);
      try {
        await createTransaction({
          txDate,
          txType,
          title: d.title.trim() || "(미입력)",
          amount: parseAmount(d.amount),
          category: d.category,
          cardName: withCard ? d.cardName : "",
          remarks: d.remarks,
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
        category: d.category,
        cardName: withCard ? d.cardName : "",
        remarks: d.remarks,
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

  function focusNewRow() {
    newRowRef.current?.focus();
  }

  const hdrCls =
    variant === "expense" ? "cb-th cb-th--expense" : "cb-th cb-th--income";
  const title = variant === "expense" ? "지출내역" : "수입내역";
  const icon = variant === "expense" ? "↓" : "↑";
  const tableCls =
    variant === "expense"
      ? "cb-table cb-table--inline cb-table--excel cb-table--expense"
      : "cb-table cb-table--inline cb-table--excel cb-table--income";

  return (
    <section className="cb-panel cb-panel--excel">
      <div className={`cb-panel__head ${hdrCls}`}>
        <div className="cb-panel__headInner">
          <div className="cb-panel__title">
            <span className="cb-panel__icon">{icon}</span>
            <span>{title}</span>
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
          <table className={tableCls}>
            <colgroup>
              <col className="cb-col-check" />
              <col className="cb-col-title" />
              <col className="cb-col-amount" />
              <col className="cb-col-category" />
              {withCard && <col className="cb-col-card" />}
              <col className="cb-col-remarks" />
            </colgroup>
            <thead>
              <tr>
                <th className="cb-col-check" />
                <th>항목</th>
                <th className="cb-num">금액</th>
                <th>분류</th>
                {withCard && <th>카드명</th>}
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
                    value={d.title}
                    onChange={(e) => handleTitleChange(d, e.target.value)}
                    disabled={busy}
                  />
                </td>
                <td>
                  <input
                    className="cb-cell cb-num"
                    value={d.amount}
                    onChange={(e) => patchRow(d.key, { amount: formatAmountInput(e.target.value) })}
                    disabled={busy}
                  />
                </td>
                <td>
                  <CategoryPicker
                    className="cb-cell"
                    groups={categoryGroups}
                    value={d.category}
                    onChange={(v) => patchRow(d.key, { category: v })}
                    disabled={busy}
                  />
                </td>
                {withCard && (
                  <td>
                    <input
                      className="cb-cell"
                      value={d.cardName}
                      onChange={(e) => patchRow(d.key, { cardName: e.target.value })}
                      disabled={busy}
                    />
                  </td>
                )}
                <td>
                  <input
                    className="cb-cell"
                    value={d.remarks}
                    onChange={(e) => patchRow(d.key, { remarks: e.target.value })}
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
          <span className="cb-meta cb-meta--sum">{formatMoney(sum)}</span>
          {variant === "expense" && (
            <span className="cb-meta">
              (현금 {formatMoney(cashSum)} + 카드 {formatMoney(cardSum)})
            </span>
          )}
        </div>
      </div>
    </section>
  );
}
