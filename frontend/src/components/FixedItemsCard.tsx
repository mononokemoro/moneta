import { useEffect, useState } from "react";
import type { FixedItem } from "../api/cashbook";
import { sendFixedItemsToCashbook } from "../api/cashbook";
import type { LedgerBook } from "../api/ledgerBook";
import { amountToInput, formatAmountInput, parseAmount } from "../util/parseAmount";

type ItemState = {
  checked: boolean;
  amount: string;
};

type Props = {
  book: LedgerBook;
  txDate: string;
  items: FixedItem[];
  disabled?: boolean;
  onReload: () => Promise<void>;
  onDirtyChange?: (dirty: boolean) => void;
  onOpenFixedSettings?: () => void;
};

export function FixedItemsCard({ book, txDate, items, disabled, onReload, onDirtyChange, onOpenFixedSettings }: Props) {
  const [state, setState] = useState<Record<number, ItemState>>({});
  const [busy, setBusy] = useState(false);

  useEffect(() => {
    const next: Record<number, ItemState> = {};
    for (const item of items) {
      next[item.id] = {
        checked: false,
        amount: item.defaultAmount ? amountToInput(item.defaultAmount) : "",
      };
    }
    setState(next);
  }, [items, txDate]);

  const hasChecked = items.some((item) => state[item.id]?.checked);
  useEffect(() => {
    onDirtyChange?.(hasChecked);
  }, [hasChecked, onDirtyChange]);

  function patch(id: number, patch: Partial<ItemState>) {
    setState((prev) => ({ ...prev, [id]: { ...prev[id], ...patch } }));
  }

  async function handleSend() {
    const entries = items
      .filter((item) => state[item.id]?.checked)
      .map((item) => ({
        fixedItemId: item.id,
        amount: parseAmount(state[item.id]?.amount ?? ""),
      }))
      .filter((e) => e.amount !== 0);

    if (entries.length === 0) return;

    setBusy(true);
    try {
      await sendFixedItemsToCashbook(txDate, entries, book);
      await onReload();
      setState((prev) => {
        const next = { ...prev };
        for (const id of Object.keys(next)) {
          next[Number(id)] = { ...next[Number(id)], checked: false };
        }
        return next;
      });
    } finally {
      setBusy(false);
    }
  }

  const selectedCount = items.filter((item) => state[item.id]?.checked).length;

  return (
    <section className="cb-card cb-card--fixed">
      <h3 className="cb-card__title cb-card__title--link">
        <button type="button" className="cb-card__titleBtn" onClick={onOpenFixedSettings}>
          오늘의 고정
        </button>
      </h3>
      {items.length === 0 ? (
        <p className="cb-muted">오늘 해당하는 고정 항목이 없습니다.</p>
      ) : (
        <div className="cb-fixedlist-scroll">
          <ul className="cb-fixedlist">
            {items.map((item) => {
              const s = state[item.id] ?? { checked: false, amount: "" };
              return (
                <li key={item.id} className="cb-fixedlist__row">
                  <input
                    type="checkbox"
                    checked={s.checked}
                    onChange={(e) => patch(item.id, { checked: e.target.checked })}
                    disabled={disabled || busy}
                  />
                  <span className="cb-fixedlist__title" title={item.category}>
                    {item.title}
                  </span>
                  <input
                    className="cb-fixedlist__amount cb-num"
                    value={s.amount}
                    onChange={(e) => patch(item.id, { amount: formatAmountInput(e.target.value) })}
                    disabled={disabled || busy}
                  />
                </li>
              );
            })}
          </ul>
        </div>
      )}
      <button
        type="button"
        className="cb-btn cb-btn--secondary cb-btn--block"
        disabled={disabled || busy || selectedCount === 0}
        onClick={handleSend}
      >
        가계부로 보내기
      </button>
    </section>
  );
}
