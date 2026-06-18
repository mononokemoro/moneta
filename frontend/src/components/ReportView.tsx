import { useEffect, useMemo, useState } from "react";
import {
  CartesianGrid,
  Legend,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from "recharts";
import { fetchReport, type ReportData } from "../api/report";
import type { LedgerBook } from "../api/ledgerBook";
import { formatMoney } from "../formatMoney";
import { downloadReportCsv } from "../report/exportReportCsv";
import { ReportCalendar, ReportDetail, ReportMatrix } from "../report/ReportPanels";
import { BookSwitcher } from "./BookSwitcher";
import {
  clampReportYear,
  defaultSubView,
  REPORT_CATEGORIES,
  REPORT_MIN_YEAR,
  reportMaxYear,
  yearRange,
  type ReportCategory,
  type ReportGranularity,
  type ReportPeriod,
} from "../report/reportConfig";

const DETAIL_SUBVIEWS = new Set([
  "DETAIL",
  "SAVINGS_DETAIL",
  "INSURANCE_DETAIL",
  "BY_PAYMENT_DATE",
]);

type Props = {
  book: LedgerBook;
  onBookChange: (book: LedgerBook) => void;
};

export function ReportView({ book, onBookChange }: Props) {
  const now = new Date().getFullYear();
  const [year, setYear] = useState(now);
  const [period, setPeriod] = useState<ReportPeriod>("H1");
  const [category, setCategory] = useState<ReportCategory>("ALL");
  const [subView, setSubView] = useState(defaultSubView("ALL"));
  const [granularity, setGranularity] = useState<ReportGranularity>("MONTHLY");
  const [data, setData] = useState<ReportData | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  const catDef = REPORT_CATEGORIES.find((c) => c.id === category)!;
  const maxYear = reportMaxYear(now);
  const years = useMemo(() => yearRange(year, 7, REPORT_MIN_YEAR, maxYear), [year, maxYear]);

  useEffect(() => {
    setSubView(defaultSubView(category));
  }, [category]);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setErr(null);
    fetchReport(year, period, category, subView, granularity, book)
      .then((d) => {
        if (!cancelled) setData(d);
      })
      .catch((e: unknown) => {
        if (!cancelled) setErr(e instanceof Error ? e.message : "조회 실패");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [year, period, category, subView, granularity, book]);

  const chartData = useMemo(() => {
    if (!data?.trend?.length) return [];
    const label =
      data.rows.find((r) => r.key === "EXPENSE")?.label ??
      data.rows[0]?.label ??
      "금액";
    return data.trend.map((p) => ({
      name: p.label,
      [label]: p.value,
    }));
  }, [data]);

  const chartKey = data?.rows[0]?.label ?? "금액";
  const showGranularity =
    catDef.showGranularity &&
    !DETAIL_SUBVIEWS.has(subView) &&
    category !== "CALENDAR" &&
    category !== "BUDGET";

  function shiftYear(delta: number) {
    setYear((y) => clampReportYear(y + delta, REPORT_MIN_YEAR, maxYear));
  }

  const canShiftPrev = year > REPORT_MIN_YEAR;
  const canShiftNext = year < maxYear;

  return (
    <main className="cb-report">
      <header className="cb-report__top">
        <BookSwitcher book={book} onChange={onBookChange} />
        <nav className="cb-report__mainTabs" aria-label="보고서 구분">
          {REPORT_CATEGORIES.map((c) => (
            <button
              key={c.id}
              type="button"
              className={category === c.id ? "is-active" : ""}
              onClick={() => setCategory(c.id)}
            >
              {c.label}
            </button>
          ))}
        </nav>
        <div className="cb-report__actions">
          <button
            type="button"
            className="cb-btn cb-btn--ghost"
            disabled={!data}
            onClick={() => data && downloadReportCsv(data)}
          >
            엑셀저장
          </button>
          <button type="button" className="cb-btn cb-btn--ghost" onClick={() => window.print()}>
            인쇄
          </button>
        </div>
      </header>

      <nav className="cb-report__subTabs" aria-label="보고서 하위 메뉴">
        {catDef.subViews.map((sv) => (
          <button
            key={sv.id}
            type="button"
            className={subView === sv.id ? "is-active" : ""}
            onClick={() => setSubView(sv.id)}
          >
            {sv.label}
          </button>
        ))}
      </nav>

      <div className="cb-report__subbar">
        {showGranularity ? (
          <div className="cb-report__periodTabs">
            {(
              [
                ["MONTHLY", "월간"],
                ["WEEKLY", "주간"],
                ["DAILY", "일간"],
              ] as const
            ).map(([g, label]) => (
              <button
                key={g}
                type="button"
                className={granularity === g ? "is-active" : ""}
                onClick={() => setGranularity(g)}
              >
                {label}
              </button>
            ))}
          </div>
        ) : (
          <div />
        )}
        <div className="cb-report__half">
          {(
            [
              ["H1", "상반기"],
              ["H2", "하반기"],
              ["FY", "전체"],
            ] as const
          ).map(([p, label]) => (
            <button
              key={p}
              type="button"
              className={period === p ? "is-active" : ""}
              onClick={() => setPeriod(p)}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      <div className="cb-report__years">
        <button
          type="button"
          className="cb-report__yearNav"
          disabled={!canShiftPrev}
          onClick={() => shiftYear(-1)}
          aria-label="이전 연도"
        >
          ‹
        </button>
        {years.map((yy) => (
          <button
            key={yy}
            type="button"
            className={year === yy ? "is-active" : ""}
            onClick={() => setYear(yy)}
          >
            {yy}
          </button>
        ))}
        <button
          type="button"
          className="cb-report__yearNav"
          disabled={!canShiftNext}
          onClick={() => shiftYear(1)}
          aria-label="다음 연도"
        >
          ›
        </button>
      </div>

      {loading && <p className="cb-muted">불러오는 중…</p>}
      {err && <p className="cb-err">{err}</p>}

      {data && !loading && (
        <>
          <div className="cb-report__titleRow">
            <h1 className="cb-report__h1">{data.title}</h1>
          </div>

          {data.viewMode === "matrix" && <ReportMatrix data={data} />}
          {data.viewMode === "detail" && <ReportDetail data={data} />}
          {data.viewMode === "calendar" && <ReportCalendar data={data} />}

          {data.footnotes.length > 0 && (
            <p className="cb-report__footnotes">
              {data.footnotes.map((note) => (
                <span key={note}>
                  * {note}
                  <br />
                </span>
              ))}
            </p>
          )}

          {data.viewMode === "matrix" && chartData.length > 0 && (
            <section className="cb-report__chart">
              <h2 className="cb-report__h2">추이</h2>
              <div className="cb-report__chartbox">
                <ResponsiveContainer width="100%" height={220}>
                  <LineChart data={chartData} margin={{ top: 6, right: 12, left: 4, bottom: 4 }}>
                    <CartesianGrid strokeDasharray="3 3" stroke="#e5e8eb" />
                    <XAxis dataKey="name" tick={{ fontSize: 10 }} />
                    <YAxis
                      tickFormatter={(v) => formatMoney(Number(v))}
                      width={56}
                      tick={{ fontSize: 10 }}
                    />
                    <Tooltip
                      formatter={(value: number) => [formatMoney(value), chartKey]}
                      labelFormatter={(l) => String(l)}
                    />
                    <Legend />
                    <Line
                      type="monotone"
                      dataKey={chartKey}
                      stroke="#30b06e"
                      strokeWidth={2}
                      dot={{ r: 4 }}
                      activeDot={{ r: 6 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              </div>
            </section>
          )}
        </>
      )}
    </main>
  );
}
