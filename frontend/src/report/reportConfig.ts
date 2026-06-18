export type ReportPeriod = "H1" | "H2" | "FY";
export type ReportGranularity = "MONTHLY" | "WEEKLY" | "DAILY";
export type ReportCategory =
  | "ALL"
  | "INCOME"
  | "SAVINGS_INSURANCE"
  | "LOAN"
  | "EXPENSE"
  | "CARD"
  | "BUDGET"
  | "CALENDAR";

export type ReportSubView = string;

export type SubViewDef = {
  id: ReportSubView;
  label: string;
};

export type CategoryDef = {
  id: ReportCategory;
  label: string;
  subViews: SubViewDef[];
  /** true면 상단 월간/주간/일간 탭 표시 */
  showGranularity?: boolean;
};

export const REPORT_CATEGORIES: CategoryDef[] = [
  {
    id: "ALL",
    label: "전체",
    showGranularity: true,
    subViews: [
      { id: "DETAIL", label: "상세내역" },
      { id: "MONTHLY", label: "월간" },
    ],
  },
  {
    id: "INCOME",
    label: "수입",
    showGranularity: true,
    subViews: [
      { id: "DETAIL", label: "상세내역" },
      { id: "MONTHLY", label: "월간" },
    ],
  },
  {
    id: "SAVINGS_INSURANCE",
    label: "저축/보험",
    subViews: [
      { id: "ALL", label: "전체" },
      { id: "SAVINGS_MONTHLY", label: "저축 월간" },
      { id: "INSURANCE_MONTHLY", label: "보험 월간" },
      { id: "SAVINGS_DETAIL", label: "저축 상세 내역" },
      { id: "INSURANCE_DETAIL", label: "보험 상세 내역" },
    ],
  },
  {
    id: "LOAN",
    label: "대출",
    showGranularity: true,
    subViews: [
      { id: "ALL", label: "전체" },
      { id: "MONTHLY", label: "월간" },
      { id: "DETAIL", label: "상세내역" },
    ],
  },
  {
    id: "EXPENSE",
    label: "지출",
    subViews: [
      { id: "DETAIL", label: "상세내역" },
      { id: "MONTHLY", label: "월간" },
      { id: "WEEKLY", label: "주간" },
      { id: "DAILY", label: "일간" },
      { id: "FIXED", label: "고정지출" },
    ],
  },
  {
    id: "CARD",
    label: "카드",
    subViews: [
      { id: "DETAIL", label: "상세내역" },
      { id: "BY_PAYMENT_DATE", label: "결제일별" },
      { id: "MONTHLY", label: "월별" },
      { id: "BY_SETTLEMENT", label: "결산기간별" },
      { id: "BY_CARD_NAME", label: "카드대금별" },
    ],
  },
  {
    id: "BUDGET",
    label: "예산/실적",
    subViews: [
      { id: "BUDGET_WRITE", label: "예산작성" },
      { id: "BUDGET_EVAL", label: "예산평가" },
    ],
  },
  {
    id: "CALENDAR",
    label: "달력보기",
    subViews: [
      { id: "ALL", label: "전체" },
      { id: "EXPENSE_CASH_CARD", label: "지출(현금+카드)" },
      { id: "CARD_ONLY", label: "카드지출" },
    ],
  },
];

export function defaultSubView(category: ReportCategory): ReportSubView {
  return REPORT_CATEGORIES.find((c) => c.id === category)?.subViews[0]?.id ?? "MONTHLY";
}

/** 모네타 import 데이터 시작 연도 */
export const REPORT_MIN_YEAR = 2010;

export function reportMaxYear(now = new Date().getFullYear()): number {
  return now + 1;
}

/** 선택 연도를 중심으로 표시할 연도 버튼 (좌우 ‹ › 와 함께 사용) */
export function yearRange(
  center: number,
  windowSize = 7,
  minYear = REPORT_MIN_YEAR,
  maxYear = reportMaxYear()
): number[] {
  const half = Math.floor(windowSize / 2);
  let start = Math.max(minYear, center - half);
  let end = Math.min(maxYear, start + windowSize - 1);
  start = Math.max(minYear, end - windowSize + 1);
  const years: number[] = [];
  for (let y = start; y <= end; y++) years.push(y);
  return years;
}

export function clampReportYear(
  year: number,
  minYear = REPORT_MIN_YEAR,
  maxYear = reportMaxYear()
): number {
  return Math.min(maxYear, Math.max(minYear, year));
}
