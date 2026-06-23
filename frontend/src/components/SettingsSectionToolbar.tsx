import type { ReactNode } from "react";

type Tab = { id: string; label: string };

type Props = {
  tabs: Tab[];
  activeTab: string;
  onTabChange: (id: string) => void;
  actions?: ReactNode;
  ariaLabel?: string;
};

export function SettingsSectionToolbar({
  tabs,
  activeTab,
  onTabChange,
  actions,
  ariaLabel = "설정 하위 메뉴",
}: Props) {
  return (
    <div className="cb-settings__toolbar">
      <nav className="cb-settings__subTabs" role="tablist" aria-label={ariaLabel}>
        {tabs.map((tab) => (
          <button
            key={tab.id}
            type="button"
            role="tab"
            aria-selected={activeTab === tab.id}
            className={activeTab === tab.id ? "is-active" : ""}
            onClick={() => onTabChange(tab.id)}
          >
            {tab.label}
          </button>
        ))}
      </nav>
      {actions ? <div className="cb-settings__toolbarActions">{actions}</div> : null}
    </div>
  );
}
