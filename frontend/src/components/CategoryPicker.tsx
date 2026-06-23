import { forwardRef, useEffect, useMemo, useRef, useState } from "react";
import { createPortal } from "react-dom";
import {
  selectableCategoryGroups,
  type CategoryGroup,
} from "../api/categories";
import { matchesKoreanSearch } from "../util/koreanSearch";

type Props = {
  value: string;
  onChange: (value: string) => void;
  groups: CategoryGroup[];
  disabled?: boolean;
  className?: string;
  placeholder?: string;
};

function filterGroups(groups: CategoryGroup[], query: string): CategoryGroup[] {
  const q = query.trim();
  if (!q) return groups;
  return groups
    .map((g) => {
      const children = g.children.filter((c) => matchesKoreanSearch(c.name, q));
      if (children.length > 0) {
        return { ...g, children };
      }
      return null;
    })
    .filter((g): g is CategoryGroup => g !== null);
}

export const CategoryPicker = forwardRef<HTMLInputElement, Props>(function CategoryPicker(
  { value, onChange, groups, disabled, className, placeholder },
  ref,
) {
  const holder = useRef<{ el: HTMLInputElement | null }>({ el: null });
  const setInputRef = (node: HTMLInputElement | null) => {
    holder.current.el = node;
    if (typeof ref === "function") ref(node);
    else if (ref) (ref as React.MutableRefObject<HTMLInputElement | null>).current = node;
  };

  const [open, setOpen] = useState(false);
  const [filterText, setFilterText] = useState("");
  const [activeMajorId, setActiveMajorId] = useState<number | null>(null);
  const [rect, setRect] = useState<DOMRect | null>(null);

  const selectable = useMemo(() => selectableCategoryGroups(groups), [groups]);
  const filtered = useMemo(() => filterGroups(selectable, filterText), [selectable, filterText]);
  const activeMajor = filtered.find((g) => g.id === activeMajorId) ?? null;

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
  }, [open, filterText]);

  useEffect(() => {
    if (!open) {
      setFilterText("");
      setActiveMajorId(null);
      return;
    }
    if (filtered.length === 0) {
      setActiveMajorId(null);
      return;
    }
    if (activeMajorId == null || !filtered.some((g) => g.id === activeMajorId)) {
      setActiveMajorId(filtered[0].id);
    }
  }, [open, filtered, activeMajorId]);

  function selectOption(name: string) {
    onChange(name);
    setOpen(false);
  }

  const dropdown =
    open &&
    filtered.length > 0 &&
    rect &&
    createPortal(
      <div
        className="cb-catpicker__menu"
        style={{
          position: "fixed",
          top: rect.bottom + 2,
          left: rect.left,
          minWidth: Math.max(rect.width, 220),
        }}
      >
        <ul className="cb-catpicker__majors" role="listbox" aria-label="대분류">
          {filtered.map((major) => {
            const isActive = major.id === activeMajorId;
            return (
              <li
                key={major.id}
                role="option"
                aria-selected={isActive}
                className={isActive ? "cb-catpicker__major is-active" : "cb-catpicker__major"}
                onMouseEnter={() => setActiveMajorId(major.id)}
                onMouseDown={(e) => {
                  e.preventDefault();
                  if (major.children.length === 0) selectOption(major.name);
                }}
              >
                <span className="cb-catpicker__majorLabel">{major.name}</span>
              </li>
            );
          })}
        </ul>
        {activeMajor && activeMajor.children.length > 0 ? (
          <ul
            className="cb-catpicker__minors"
            role="listbox"
            aria-label={`${activeMajor.name} 소분류`}
          >
            {activeMajor.children.map((minor) => (
              <li
                key={minor.id}
                role="option"
                className="cb-catpicker__minor"
                onMouseDown={(e) => {
                  e.preventDefault();
                  selectOption(minor.name);
                }}
              >
                {minor.name}
              </li>
            ))}
          </ul>
        ) : (
          <div className="cb-catpicker__minors cb-catpicker__minors--empty" aria-hidden="true" />
        )}
      </div>,
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
          setFilterText(e.target.value);
          setOpen(true);
        }}
        onFocus={() => {
          updateRect();
          setFilterText("");
          setOpen(true);
        }}
        onBlur={() => {
          window.setTimeout(() => setOpen(false), 120);
        }}
        onKeyDown={(e) => {
          if (e.key === "Escape") {
            setOpen(false);
          }
        }}
      />
      {dropdown}
    </>
  );
});
