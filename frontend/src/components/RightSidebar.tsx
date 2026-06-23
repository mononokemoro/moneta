import type { DayView } from "../api/cashbook";
import { formatMoney } from "../formatMoney";
import { formatVisitDate } from "../util/dateUtil";

export type SidebarView = "cashbook" | "report" | "data" | "settings";

type Props = {
  view: SidebarView;
  onViewChange: (v: SidebarView) => void;
  date: string;
  day: DayView | null;
};

function sumPayment(p: { cash: number; creditCard: number; debitCard: number; otherCard: number }) {
  return p.cash + p.creditCard + p.debitCard + p.otherCard;
}

export function RightSidebar({ view, onViewChange, date, day }: Props) {
  const daily = day?.paymentSummary;
  const monthly = day?.monthlyPaymentSummary ?? daily;
  const cardMonthly =
    (monthly?.creditCard ?? 0) + (monthly?.debitCard ?? 0) + (monthly?.otherCard ?? 0);
  const monthTotal = monthly ? sumPayment(monthly) : 0;
  const budget = day?.budget;

  return (
    <aside className="cb-side cb-side--right">
      <div className="cb-profile">
        <div className="cb-profile__avatar" aria-hidden />
        <div className="cb-profile__txt">
          <div className="cb-profile__name">미니가계부</div>
          <div className="cb-profile__sub">
            {budget ? (
              <>
                결산일 {budget.periodLabel}
                <br />
                방문일 {formatVisitDate(date)}
              </>
            ) : (
              "Cashbook"
            )}
          </div>
        </div>
      </div>

      <button
        type="button"
        className={`cb-side__settings${view === "settings" ? " is-active" : ""}`}
        aria-label="설정"
        aria-current={view === "settings" ? "page" : undefined}
        onClick={() => onViewChange("settings")}
      >
        설정
      </button>

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
        <button
          type="button"
          className={view === "data" ? "cb-tab is-active" : "cb-tab"}
          onClick={() => onViewChange("data")}
        >
          데이터
        </button>
      </nav>

      {view === "cashbook" && (
        <>
          <section className="cb-card cb-card--compact">
            <h3 className="cb-card__title">
              이달 지출 {day?.budget.periodLabel ? `(${day.budget.periodLabel})` : ""}
            </h3>
            {!monthly ? (
              <p className="cb-muted">불러오는 중…</p>
            ) : (
              <ul className="cb-paylist">
                <li>
                  <span>현금지출</span>
                  <strong>{formatMoney(monthly.cash)}</strong>
                </li>
                <li className="cb-paylist__group">
                  <span>카드지출</span>
                  <strong>{formatMoney(cardMonthly)}</strong>
                </li>
                <li className="cb-paylist__sub">
                  <span>신용카드</span>
                  <strong>{formatMoney(monthly.creditCard)}</strong>
                </li>
                <li className="cb-paylist__sub">
                  <span>체크카드</span>
                  <strong>{formatMoney(monthly.debitCard)}</strong>
                </li>
                <li className="cb-paylist__sub">
                  <span>기타</span>
                  <strong>{formatMoney(monthly.otherCard)}</strong>
                </li>
                <li className="cb-paylist__total">
                  <span>합계</span>
                  <strong>{formatMoney(monthTotal)}</strong>
                </li>
              </ul>
            )}
          </section>

          <section className="cb-card cb-card--compact">
            <h3 className="cb-card__title">결제 요약 (선택일)</h3>
            {!daily ? (
              <p className="cb-muted">불러오는 중…</p>
            ) : (
              <ul className="cb-paylist">
                <li>
                  <span>현금</span>
                  <strong>{formatMoney(daily.cash)}</strong>
                </li>
                <li>
                  <span>신용카드</span>
                  <strong>{formatMoney(daily.creditCard)}</strong>
                </li>
                <li>
                  <span>체크카드</span>
                  <strong>{formatMoney(daily.debitCard)}</strong>
                </li>
                <li>
                  <span>기타 카드</span>
                  <strong>{formatMoney(daily.otherCard)}</strong>
                </li>
              </ul>
            )}
          </section>
        </>
      )}
    </aside>
  );
}
