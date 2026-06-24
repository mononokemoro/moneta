import { useEffect, useState } from "react";
import { CompactDateInput } from "./CompactDateInput";
import { formatDayTitle } from "../util/dateUtil";

type Props = {
  open: boolean;
  count: number;
  currentDate: string;
  initialDate: string;
  busy?: boolean;
  error?: string | null;
  onClose: () => void;
  onConfirm: (targetDate: string) => void;
};

export function TransactionMoveDialog({
  open,
  count,
  currentDate,
  initialDate,
  busy = false,
  error = null,
  onClose,
  onConfirm,
}: Props) {
  const [targetDate, setTargetDate] = useState(initialDate);

  useEffect(() => {
    if (open) setTargetDate(initialDate);
  }, [open, initialDate]);

  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === "Escape" && !busy) onClose();
    };
    window.addEventListener("keydown", onKey);
    return () => window.removeEventListener("keydown", onKey);
  }, [open, busy, onClose]);

  if (!open) return null;

  return (
    <div className="cb-modalBackdrop" onClick={() => !busy && onClose()}>
      <div
        className="cb-modal"
        role="dialog"
        aria-modal="true"
        aria-labelledby="cb-move-dialog-title"
        onClick={(e) => e.stopPropagation()}
      >
        <header className="cb-modal__hdr">
          <h2 id="cb-move-dialog-title" className="cb-modal__title">
            날짜이동
          </h2>
          <p className="cb-modal__hdrDate">{formatDayTitle(currentDate)}</p>
        </header>
        <div className="cb-modal__body">
          <p className="cb-modal__hint">선택한 {count}건을 이동할 날짜를 선택하세요.</p>
          <label className="cb-modal__field">
            <span className="cb-modal__label">이동 날짜</span>
            <CompactDateInput
              className="cb-modal__date"
              value={targetDate}
              disabled={busy}
              autoFocus
              onChange={setTargetDate}
              onEnter={onConfirm}
            />
          </label>
          {error && <p className="cb-err">{error}</p>}
        </div>
        <footer className="cb-modal__actions">
          <button
            type="button"
            className="cb-btn cb-btn--primary"
            disabled={busy}
            onClick={() => onConfirm(targetDate)}
          >
            이동
          </button>
          <button type="button" className="cb-btn cb-btn--secondary" disabled={busy} onClick={onClose}>
            닫기
          </button>
        </footer>
      </div>
    </div>
  );
}
