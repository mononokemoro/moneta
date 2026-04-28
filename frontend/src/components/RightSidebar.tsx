import type { DayView } from "../api/cashbook";
import { formatMoney } from "../formatMoney";

export type SidebarView = "cashbook" | "report";

type Props = {
  view: SidebarView;
  onViewChange: (v: SidebarView) => void;
  day: DayView | null;
};

export function RightSidebar({ view, onViewChange, day }: Props) {
  const p = day?.paymentSummary;

  return (
    <aside className="cb-side cb-side--right">
      <nav className="cb-tabs" aria-label="보조 메뉴">
        <button
          type="button"
          className={view === "cashbook" ? "cb-tab is-active" : "cb-tab"}
          onClick={() => onViewChange("cashbook")}
        >
          가계부
        </button>
        <button
          type="button"
          className={view === "report" ? "cb-tab is-active" : "cb-tab"}
          onClick={() => onViewChange("report")}
        >
          보고서
        </button>
      </nav>

      {view === "cashbook" && (
        <section className="cb-card cb-card--compact">
          <h3 className="cb-card__title">결제 요약 (선택일)</h3>
          {!p ? (
            <p className="cb-muted">불러오는 중…</p>
          ) : (
            <ul className="cb-paylist">
              <li>
                <span>현금</span>
                <strong>{formatMoney(p.cash)}</strong>
              </li>
              <li>
                <span>신용카드</span>
                <strong>{formatMoney(p.creditCard)}</strong>
              </li>
              <li>
                <span>체크카드</span>
                <strong>{formatMoney(p.debitCard)}</strong>
              </li>
              <li>
                <span>기타 카드</span>
                <strong>{formatMoney(p.otherCard)}</strong>
              </li>
            </ul>
          )}
        </section>
      )}

    </aside>
  );
}
