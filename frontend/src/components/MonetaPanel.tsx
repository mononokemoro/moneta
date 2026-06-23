import type { ReactNode } from "react";
import { SettingsSectionToolbar } from "./SettingsSectionToolbar";

type Tab = { id: string; label: string };

type Props = {
  title?: string;
  tabs: Tab[];
  activeTab: string;
  onTabChange: (id: string) => void;
  subNav?: ReactNode;
  hint?: ReactNode;
  err?: string | null;
  busy?: boolean;
  wide?: boolean;
  onSave: () => void;
  onClose?: () => void;
  closeLabel?: string;
  children: ReactNode;
};

export function MonetaPanel({
  tabs,
  activeTab,
  onTabChange,
  subNav,
  hint,
  err,
  busy,
  onSave,
  onClose,
  closeLabel = "초기화",
  children,
}: Props) {
  return (
    <div className="cb-settings__section">
      <SettingsSectionToolbar
        tabs={tabs}
        activeTab={activeTab}
        onTabChange={onTabChange}
        actions={
          <>
            <button type="button" className="cb-btn cb-btn--primary" disabled={busy} onClick={onSave}>
              저장
            </button>
            {onClose ? (
              <button type="button" className="cb-btn cb-btn--secondary" onClick={onClose}>
                {closeLabel}
              </button>
            ) : null}
          </>
        }
      />

      {subNav}
      {hint}
      {err ? <p className="cb-err cb-settings__err">{err}</p> : null}

      <div className="cb-settings__content">{children}</div>
    </div>
  );
}

export function MonetaSubNav({
  items,
  active,
  onChange,
}: {
  items: { id: string; label: string }[];
  active: string;
  onChange: (id: string) => void;
}) {
  return (
    <nav className="cb-settings__subToolbar" aria-label="하위 메뉴">
      {items.map((item, idx) => (
        <span key={item.id} className="cb-settings__subToolbarGroup">
          {idx > 0 ? <span className="cb-settings__subToolbarSep">|</span> : null}
          <button
            type="button"
            className={`cb-settings__subToolbarItem${active === item.id ? " is-active" : ""}`}
            onClick={() => onChange(item.id)}
          >
            {item.label}
          </button>
        </span>
      ))}
    </nav>
  );
}

export function MonetaHint({ children }: { children: ReactNode }) {
  return (
    <p className="cb-settings__hintLine">
      <span className="cb-settings__hintMark">›</span>
      {children}
    </p>
  );
}
