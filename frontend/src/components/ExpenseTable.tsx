import { useEffect, useRef, useState } from "react";
import type { CategoryKeyword } from "../api/categoryKeywords";
import {
  createTransaction,
  deleteTransaction,
  updateTransaction,
  type ExpenseScope,
  type TransactionRow,
  type TxType,
} from "../api/cashbook";
import type { LedgerBook } from "../api/ledgerBook";
import { formatMoney } from "../formatMoney";
import { amountToInput, parseAmount } from "../util/parseAmount";
import { buildTableDrafts } from "../util/tableDrafts";
import { matchCategoryByKeyword } from "../util/matchCategoryKeyword";
import { CategoryDatalist } from "./CategoryDatalist";

type RowDraft = {
  key: string;
  id?: number;
  title: string;
  amount: string;
  category: string;
  cardName: string;
  remarks: string;
  expenseScope: ExpenseScope;
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
  categoryOptions?: string[];
  categoryKeywords?: CategoryKeyword[];
};

function emptyDraft(): RowDraft {
  return {
    key: `new-${Math.random().toString(36).slice(2)}`,
    title: "",
    amount: "",
    category: "",
    cardName: "",
    remarks: "",
    expenseScope: "NORMAL",
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
    expenseScope: r.expenseScope === "COMMON" ? "COMMON" : "NORMAL",
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
  const scope = r.expenseScope === "COMMON" ? "COMMON" : "NORMAL";
  return (
    d.title !== r.title ||
    parseAmount(d.amount) !== r.amount ||
    d.category !== r.category ||
    d.cardName !== r.cardName ||
    d.remarks !== r.remarks ||
    d.expenseScope !== scope
  );
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
  categoryOptions = [],
  categoryKeywords = [],
}: Props) {
  const [drafts, setDrafts] = useState<RowDraft[]>([]);
  const [busy, setBusy] = useState(false);
  const newRowRef = useRef<HTMLInputElement | null>(null);
  const withCard = variant === "expense";
  const withScope = withCard;
  const catListId = `cat-${variant}`;

  useEffect(() => {
    setDrafts(buildTableDrafts(rows.map(fromRow), emptyDraft));
  }, [rows, txDate]);

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

  async function handleScopeChange(d: RowDraft, checked: boolean) {
    const expenseScope: ExpenseScope = checked ? "COMMON" : "NORMAL";
    patchRow(d.key, { expenseScope });
    if (d.id) {
      await commitRow({ ...d, expenseScope });
    }
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
          expenseScope: withScope ? d.expenseScope : undefined,
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
        expenseScope: withScope ? d.expenseScope : undefined,
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

  const hdrCls =
    variant === "expense" ? "cb-th cb-th--expense" : "cb-th cb-th--income";
  const title = variant === "expense" ? "지출내역" : "수입내역";
  const icon = variant === "expense" ? "↓" : "↑";

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
        <CategoryDatalist id={catListId} options={categoryOptions} />
        <table className="cb-table cb-table--inline cb-table--excel">
          <thead>
            <tr>
              <th className="cb-col-check" />
              {withScope && <th className="cb-col-scope">공통</th>}
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
                {withScope && (
                  <td className="cb-col-scope">
                    <input
                      type="checkbox"
                      className="cb-scope-check"
                      checked={d.expenseScope === "COMMON"}
                      onChange={(e) => void handleScopeChange(d, e.target.checked)}
                      disabled={busy}
                      aria-label="공통 항목"
                      title="공통 항목 (다른 장부에도 등록)"
                    />
                  </td>
                )}
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
                    onChange={(e) => patchRow(d.key, { amount: e.target.value })}
                    disabled={busy}
                  />
                </td>
                <td>
                  <input
                    className="cb-cell"
                    list={categoryOptions.length > 0 ? catListId : undefined}
                    value={d.category}
                    onChange={(e) => patchRow(d.key, { category: e.target.value })}
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
          <tfoot>
            <tr className="cb-tfoot">
              <td colSpan={(withCard ? 6 : 5) + (withScope ? 1 : 0)}>
                <span className="cb-meta">총 {rows.length}건</span>
                <span className="cb-meta cb-meta--sum">{formatMoney(sum)}</span>
                {variant === "expense" && (
                  <span className="cb-meta">
                    (현금 {formatMoney(cashSum)} + 카드 {formatMoney(cardSum)})
                  </span>
                )}
              </td>
            </tr>
          </tfoot>
        </table>
      </div>
    </section>
  );
}
