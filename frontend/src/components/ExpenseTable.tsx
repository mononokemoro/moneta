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
import { findMinorByName } from "../api/categories";
import { CategoryPicker } from "./CategoryPicker";

type RowDraft = {
  key: string;
  id?: number;
  title: string;
  householdCategoryId: number | null;
  householdCategory: string;
  amount: string;
  categoryId: number | null;
  category: string;
  cardProductId: number | null;
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
  householdCategoryGroups?: CategoryGroup[];
  categoryKeywords?: CategoryKeyword[];
  onDirtyChange?: (dirty: boolean) => void;
};

function emptyDraft(): RowDraft {
  return {
    key: `new-${Math.random().toString(36).slice(2)}`,
    title: "",
    householdCategoryId: null,
    householdCategory: "",
    amount: "",
    categoryId: null,
    category: "",
    cardProductId: null,
    cardName: "",
    remarks: "",
  };
}

function fromRow(r: TransactionRow): RowDraft {
  return {
    key: String(r.id),
    id: r.id,
    title: r.title,
    householdCategoryId: r.householdCategoryId ?? null,
    householdCategory: r.householdCategory ?? "",
    amount: amountToInput(r.amount),
    categoryId: r.categoryId ?? null,
    category: r.category,
    cardProductId: r.cardProductId ?? null,
    cardName: r.cardName,
    remarks: r.remarks,
  };
}

function hasContent(d: RowDraft, withCard: boolean, withHouseholdCategory: boolean): boolean {
  return !!(
    d.title.trim() ||
    d.amount.trim() ||
    d.category.trim() ||
    d.remarks.trim() ||
    (withHouseholdCategory && d.householdCategory.trim()) ||
    (withCard && (d.cardName.trim() || d.cardProductId != null))
  );
}

function rowChanged(d: RowDraft, r: TransactionRow): boolean {
  return (
    d.title !== r.title ||
    (d.householdCategoryId ?? null) !== (r.householdCategoryId ?? null) ||
    (d.householdCategory || "") !== (r.householdCategory || "") ||
    parseAmount(d.amount) !== r.amount ||
    (d.categoryId ?? null) !== (r.categoryId ?? null) ||
    d.category !== r.category ||
    (d.cardProductId ?? null) !== (r.cardProductId ?? null) ||
    d.cardName !== r.cardName ||
    d.remarks !== r.remarks
  );
}

function hasUnsavedDrafts(
  drafts: RowDraft[],
  rows: TransactionRow[],
  withCard: boolean,
  withHouseholdCategory: boolean,
): boolean {
  return drafts.some((d) => {
    if (!d.id) return hasContent(d, withCard, withHouseholdCategory);
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
  householdCategoryGroups,
  categoryKeywords = [],
  onDirtyChange,
}: Props) {
  const [drafts, setDrafts] = useState<RowDraft[]>([]);
  const [busy, setBusy] = useState(false);
  const newRowRef = useRef<HTMLInputElement | null>(null);
  const withCard = variant === "expense";
  const withHouseholdCategory = withCard && book === "PERSONAL" && !!householdCategoryGroups;

  useEffect(() => {
    setDrafts(buildTableDrafts(rows.map(fromRow), emptyDraft));
  }, [rows, txDate]);

  const dirty = hasUnsavedDrafts(drafts, rows, withCard, withHouseholdCategory);
  useEffect(() => {
    onDirtyChange?.(dirty);
  }, [dirty, onDirtyChange]);

  const sum = rows.reduce((a, r) => a + (Number(r.amount) || 0), 0);
  const cashSum = rows
    .filter((r) => r.cardProductId == null && !r.cardName?.trim())
    .reduce((a, r) => a + (Number(r.amount) || 0), 0);
  const cardSum = sum - cashSum;

  function patchRow(key: string, updates: Partial<RowDraft>) {
    setDrafts((prev) => prev.map((d) => (d.key === key ? { ...d, ...updates } : d)));
  }

  function handleTitleChange(d: RowDraft, title: string) {
    const updates: Partial<RowDraft> = { title };
    const matched = matchCategoryByKeyword(title, categoryKeywords, txType);
    if (matched) {
      updates.category = matched;
      const minor = findMinorByName(categoryGroups, matched);
      updates.categoryId = minor?.id ?? null;
    }
    patchRow(d.key, updates);
  }

  function resolveCategoryPayload(d: RowDraft, groups: CategoryGroup[]) {
    if (d.categoryId != null) {
      return { categoryId: d.categoryId, category: d.category };
    }
    const minor = findMinorByName(groups, d.category);
    return { categoryId: minor?.id ?? null, category: d.category };
  }

  function resolveCardPayload(d: RowDraft) {
    if (d.cardProductId != null) {
      return { cardProductId: d.cardProductId, cardName: d.cardName };
    }
    return { cardProductId: null, cardName: d.cardName };
  }

  async function commitRow(d: RowDraft) {
    if (busy) return;
    const orig = d.id ? rows.find((r) => r.id === d.id) : undefined;

    if (!d.id) {
      if (!hasContent(d, withCard, withHouseholdCategory)) return;
      setBusy(true);
      try {
        const categoryPayload = resolveCategoryPayload(d, categoryGroups);
        const householdPayload = withHouseholdCategory
          ? d.householdCategoryId != null
            ? { householdCategoryId: d.householdCategoryId, householdCategory: d.householdCategory }
            : {
                householdCategoryId:
                  findMinorByName(householdCategoryGroups ?? [], d.householdCategory)?.id ?? null,
                householdCategory: d.householdCategory,
              }
          : {};
        await createTransaction({
          txDate,
          txType,
          title: d.title.trim() || "(미입력)",
          amount: parseAmount(d.amount),
          ...categoryPayload,
          ...(withCard ? resolveCardPayload(d) : {}),
          remarks: d.remarks,
          book,
          ...householdPayload,
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
      const categoryPayload = resolveCategoryPayload(d, categoryGroups);
      const householdPayload = withHouseholdCategory
        ? d.householdCategoryId != null
          ? { householdCategoryId: d.householdCategoryId, householdCategory: d.householdCategory }
          : {
              householdCategoryId:
                findMinorByName(householdCategoryGroups ?? [], d.householdCategory)?.id ?? null,
              householdCategory: d.householdCategory,
            }
        : {};
      await updateTransaction(d.id, {
        title: d.title.trim() || "(미입력)",
        amount: parseAmount(d.amount),
        ...categoryPayload,
        ...(withCard ? resolveCardPayload(d) : {}),
        remarks: d.remarks,
        ...householdPayload,
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
              {withHouseholdCategory && <col className="cb-col-household-category" />}
              {withCard && <col className="cb-col-card" />}
              <col className="cb-col-remarks" />
            </colgroup>
            <thead>
              <tr>
                <th className="cb-col-check" />
                <th>항목</th>
                <th className="cb-num">금액</th>
                <th>분류</th>
                {withHouseholdCategory && <th>분류⇄</th>}
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
                    key={`${d.key}-personal-category`}
                    className="cb-cell"
                    groups={categoryGroups ?? []}
                    value={d.category}
                    categoryId={d.categoryId}
                    onChange={(v) => patchRow(d.key, { category: v })}
                    onCategoryIdChange={(id) => patchRow(d.key, { categoryId: id })}
                    disabled={busy}
                  />
                </td>
                {withHouseholdCategory && (
                  <td>
                    <CategoryPicker
                      key={`${d.key}-household-category`}
                      className="cb-cell"
                      groups={householdCategoryGroups ?? []}
                      value={d.householdCategory}
                      categoryId={d.householdCategoryId}
                      onChange={(v) => patchRow(d.key, { householdCategory: v })}
                      onCategoryIdChange={(id) => patchRow(d.key, { householdCategoryId: id })}
                      disabled={busy}
                    />
                  </td>
                )}
                {withCard && (
                  <td>
                    <input
                      className="cb-cell"
                      value={d.cardName}
                      onChange={(e) =>
                        patchRow(d.key, { cardName: e.target.value, cardProductId: null })
                      }
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
