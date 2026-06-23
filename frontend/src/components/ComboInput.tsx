import { forwardRef, useEffect, useRef, useState } from "react";
import { createPortal } from "react-dom";

type Props = {
  value: string;
  onChange: (value: string) => void;
  options: string[];
  disabled?: boolean;
  className?: string;
  placeholder?: string;
};

export const ComboInput = forwardRef<HTMLInputElement, Props>(function ComboInput(
  { value, onChange, options, disabled, className, placeholder },
  ref,
) {
  const holder = useRef<{ el: HTMLInputElement | null }>({ el: null });
  const setInputRef = (node: HTMLInputElement | null) => {
    holder.current.el = node;
    if (typeof ref === "function") ref(node);
    else if (ref) (ref as React.MutableRefObject<HTMLInputElement | null>).current = node;
  };
  const [open, setOpen] = useState(false);
  const [highlight, setHighlight] = useState(0);
  const [rect, setRect] = useState<DOMRect | null>(null);

  const query = value.trim().toLowerCase();
  const filtered = options
    .filter((o) => !query || o.toLowerCase().includes(query))
    .slice(0, 16);

  function updateRect() {
    const el = holder.current.el;
    if (el) setRect(el.getBoundingClientRect());
  }

  useEffect(() => {
    if (!open) return;
    updateRect();
    const sync = () => updateRect();
    window.addEventListener("scroll", sync, true);
    window.addEventListener("resize", sync);
    return () => {
      window.removeEventListener("scroll", sync, true);
      window.removeEventListener("resize", sync);
    };
  }, [open, value]);

  useEffect(() => {
    setHighlight(0);
  }, [value, open]);

  function selectOption(opt: string) {
    onChange(opt);
    setOpen(false);
  }

  const dropdown =
    open &&
    filtered.length > 0 &&
    rect &&
    createPortal(
      <ul
        className="cb-combo__list"
        style={{
          position: "fixed",
          top: rect.bottom + 2,
          left: rect.left,
          width: rect.width,
        }}
        role="listbox"
      >
        {filtered.map((opt, i) => (
          <li
            key={opt}
            role="option"
            aria-selected={i === highlight}
            className={i === highlight ? "cb-combo__option is-active" : "cb-combo__option"}
            onMouseDown={(e) => {
              e.preventDefault();
              selectOption(opt);
            }}
          >
            {opt}
          </li>
        ))}
      </ul>,
      document.body,
    );

  return (
    <>
      <input
        ref={setInputRef}
        className={className}
        value={value}
        disabled={disabled}
        placeholder={placeholder}
        onChange={(e) => {
          onChange(e.target.value);
          setOpen(true);
        }}
        onFocus={() => {
          updateRect();
          setOpen(true);
        }}
        onBlur={() => {
          window.setTimeout(() => setOpen(false), 120);
        }}
        onKeyDown={(e) => {
          if (e.key === "ArrowDown") {
            e.preventDefault();
            if (!open) {
              setOpen(true);
              return;
            }
            setHighlight((h) => Math.min(h + 1, filtered.length - 1));
          } else if (e.key === "ArrowUp") {
            e.preventDefault();
            setHighlight((h) => Math.max(h - 1, 0));
          } else if (e.key === "Enter" && open && filtered[highlight]) {
            e.preventDefault();
            selectOption(filtered[highlight]);
          } else if (e.key === "Escape") {
            setOpen(false);
          }
        }}
      />
      {dropdown}
    </>
  );
});
