import type { LedgerBook } from "../api/ledgerBook";
import { LEDGER_BOOKS } from "../api/ledgerBook";

type Props = {
  book: LedgerBook;
  onChange: (book: LedgerBook) => void;
};

export function BookSwitcher({ book, onChange }: Props) {
  return (
    <div className="cb-bookSwitch" role="tablist" aria-label="장부 선택">
      {LEDGER_BOOKS.map((b) => (
        <button
          key={b.id}
          type="button"
          role="tab"
          aria-selected={book === b.id}
          className={book === b.id ? "is-active" : ""}
          onClick={() => onChange(b.id)}
        >
          {b.label}
        </button>
      ))}
    </div>
  );
}
