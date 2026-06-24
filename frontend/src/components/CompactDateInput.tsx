import { useEffect, useState } from "react";

import { formatDateInput, fromDisplayDate, toDisplayDate } from "../util/dateUtil";

type Props = {
  value: string;
  disabled?: boolean;
  className?: string;
  autoFocus?: boolean;
  onChange: (isoDate: string) => void;
  onEnter?: (isoDate: string) => void;
  onKeyDown?: (e: React.KeyboardEvent<HTMLInputElement>) => void;
};

export function CompactDateInput({
  value,
  disabled = false,
  className,
  autoFocus = false,
  onChange,
  onEnter,
  onKeyDown,
}: Props) {
  const [draft, setDraft] = useState(() => toDisplayDate(value));

  useEffect(() => {
    setDraft(toDisplayDate(value));
  }, [value]);

  function commit(next = draft) {
    const iso = fromDisplayDate(next);
    if (iso) {
      onChange(iso);
      setDraft(toDisplayDate(iso));
      return;
    }
    setDraft(toDisplayDate(value));
  }

  return (
    <input
      className={className}
      inputMode="numeric"
      maxLength={8}
      placeholder="yy-mm-dd"
      value={draft}
      disabled={disabled}
      autoFocus={autoFocus}
      onChange={(e) => setDraft(formatDateInput(e.target.value))}
      onBlur={() => commit()}
      onKeyDown={(e) => {
        if (e.key === "Enter") {
          e.preventDefault();
          const iso = fromDisplayDate(draft);
          if (iso) {
            onChange(iso);
            setDraft(toDisplayDate(iso));
            onEnter?.(iso);
            return;
          }
        }
        onKeyDown?.(e);
      }}
    />
  );
}
