import { useEffect, useState } from "react";

import {
  TRANSACTION_TABLE_COLUMNS,
  formatTransactionTableCell,
  groupRowsByTxType,
  type TransactionTablePreview,
  type TransactionTableRow,
} from "../api/transactionTable";
import { formatDisplayDate } from "../util/dateUtil";

type Props = {
  open: boolean;
  data: TransactionTablePreview | null;
  busy: boolean;
  error: string | null;
  nested?: boolean;
  onClose: () => void;
};

function formatTableCell(key: keyof TransactionTableRow, value: unknown): string {
  if (key === "txDate") return formatDisplayDate(String(value ?? ""));
  return formatTransactionTableCell(value);
}

function previewTitle(data: TransactionTablePreview): string {
  if (data.txDate) {
    return `${data.title} · ${formatDisplayDate(data.txDate)}`;
  }
  return data.title;
}

function TransactionTable({ rows }: { rows: TransactionTableRow[] }) {
  return (
    <table className="cb-table cb-table--excel cb-catmgmt__tablePreviewTable">
      <thead>
        <tr>
          {TRANSACTION_TABLE_COLUMNS.map((col) => (
            <th key={col.label}>
              <code>{col.label}</code>
            </th>
          ))}
        </tr>
      </thead>
      <tbody>
        {rows.map((row) => (
          <tr key={row.id} className="cb-row--saved">
            {TRANSACTION_TABLE_COLUMNS.map((col) => (
              <td
                key={col.label}
                className={
                  col.key === "amount" || col.key === "accumulatedAmount" ? "cb-num" : undefined
                }
                title={formatTableCell(col.key, row[col.key])}
              >
                {formatTableCell(col.key, row[col.key])}
              </td>
            ))}
          </tr>
        ))}
      </tbody>
    </table>
  );
}

export function TransactionTablePreviewDialog({
  open,
  data,
  busy,
  error,
  nested = false,
  onClose,
}: Props) {
  const [copyHint, setCopyHint] = useState<string | null>(null);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !busy) onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, busy, onClose]);

  useEffect(() => {
    if (!open) setCopyHint(null);
  }, [open, data?.querySql]);

  async function copyQuery() {
    if (!data?.querySql) return;
    try {
      await navigator.clipboard.writeText(data.querySql);
      setCopyHint("복사됨");
    } catch {
      setCopyHint("복사 실패");
    }
  }

  if (!open) return null;

  const sections =
    data?.groupByTxType && data.rows.length > 0 ? groupRowsByTxType(data.rows) : null;

  return (
    <div
      className={`cb-modalBackdrop${nested ? " cb-modalBackdrop--nested" : ""}`}
      onClick={() => !busy && onClose()}
    >
      <div
        className="cb-modal cb-modal--catTxTable"
        role="dialog"
        aria-modal="true"
        aria-labelledby="cb-tx-table-title"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="cb-modal__hdr cb-catmgmt__tablePreviewHdr">
          <h2 id="cb-tx-table-title" className="cb-modal__title">
            {data ? previewTitle(data) : "cb_transaction"}
          </h2>
          {data?.subtitle && <p className="cb-modal__hdrDate">{data.subtitle}</p>}
          {data?.querySql && (
            <div className="cb-catmgmt__tableQuery">
              <div className="cb-catmgmt__tableQueryHead">
                <span className="cb-catmgmt__tableQueryLabel">조회 SQL</span>
                <button
                  type="button"
                  className="cb-btn cb-btn--ghost cb-btn--xs"
                  disabled={busy}
                  onClick={() => void copyQuery()}
                >
                  {copyHint ?? "복사"}
                </button>
              </div>
              <pre className="cb-catmgmt__tableQueryCode">
                <code>{data.querySql}</code>
              </pre>
            </div>
          )}
        </header>
        <div className="cb-modal__body cb-catmgmt__tablePreviewBody">
          {busy && <p className="cb-modal__hint">불러오는 중…</p>}
          {error && <p className="cb-err">{error}</p>}
          {!busy && !error && data && data.rows.length === 0 && (
            <p className="cb-modal__hint">등록된 내역이 없습니다.</p>
          )}
          {!busy && !error && data && data.rows.length > 0 && (
            <div className="cb-catmgmt__tablePreviewScroll">
              {sections ? (
                sections.map((section) => (
                  <section key={section.label} className="cb-catmgmt__tablePreviewSection">
                    <h3 className="cb-catmgmt__tablePreviewSectionTitle">{section.label}</h3>
                    <TransactionTable rows={section.rows} />
                  </section>
                ))
              ) : (
                <TransactionTable rows={data.rows} />
              )}
            </div>
          )}
        </div>
        <footer className="cb-modal__actions">
          <button type="button" className="cb-btn cb-btn--secondary" disabled={busy} onClick={onClose}>
            닫기
          </button>
        </footer>
      </div>
    </div>
  );
}
