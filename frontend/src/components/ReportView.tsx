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
import { fetchMonthlyReport, type MonthlyReport, type ReportPeriod } from "../api/report";
import { formatMoney } from "../formatMoney";

const YEARS = [2024, 2025, 2026, 2027];

export function ReportView() {
  const y = new Date().getFullYear();
  const [year, setYear] = useState(y);
  const [period, setPeriod] = useState<ReportPeriod>("H1");
  const [data, setData] = useState<MonthlyReport | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setErr(null);
    fetchMonthlyReport(year, period)
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
  }, [year, period]);

  const chartData = useMemo(() => {
    if (!data?.expenseTrend) return [];
    return data.expenseTrend.map((p) => ({
      name: p.label,
      지출: p.value,
    }));
  }, [data]);

  return (
    <main className="cb-report">
      <header className="cb-report__top">
        <nav className="cb-report__mainTabs" aria-label="보고서 구분">
          <button type="button" className="is-active">
            전체
          </button>
          <button type="button" disabled title="추후">
            수입
          </button>
          <button type="button" disabled title="추후">
            저축/보험
          </button>
          <button type="button" disabled title="추후">
            대출
          </button>
          <button type="button" disabled title="추후">
            지출
          </button>
          <button type="button" disabled title="추후">
            카드
          </button>
          <button type="button" disabled title="추후">
            예산/실적
          </button>
          <button type="button" disabled title="추후">
            달력보기
          </button>
        </nav>
        <div className="cb-report__actions">
          <button type="button" className="cb-btn cb-btn--ghost" disabled title="추후">
            엑셀저장
          </button>
          <button type="button" className="cb-btn cb-btn--ghost" onClick={() => window.print()}>
            인쇄
          </button>
        </div>
      </header>

      <div className="cb-report__subbar">
        <div className="cb-report__periodTabs">
          <button type="button" className="is-active">
            월간
          </button>
          <button type="button" disabled title="추후">
            주간
          </button>
          <button type="button" disabled title="추후">
            일간
          </button>
        </div>
        <div className="cb-report__half">
          <button
            type="button"
            className={period === "H1" ? "is-active" : ""}
            onClick={() => setPeriod("H1")}
          >
            상반기
          </button>
          <button
            type="button"
            className={period === "H2" ? "is-active" : ""}
            onClick={() => setPeriod("H2")}
          >
            하반기
          </button>
          <button
            type="button"
            className={period === "FY" ? "is-active" : ""}
            onClick={() => setPeriod("FY")}
          >
            전체
          </button>
        </div>
      </div>

      <div className="cb-report__years">
        {YEARS.map((yy) => (
          <button
            key={yy}
            type="button"
            className={year === yy ? "is-active" : ""}
            onClick={() => setYear(yy)}
          >
            {yy}
          </button>
        ))}
      </div>

      {loading && <p className="cb-muted">불러오는 중…</p>}
      {err && <p className="cb-err">{err}</p>}

      {data && !loading && (
        <>
          <div className="cb-report__titleRow">
            <h1 className="cb-report__h1">
              월간 보고서 · {data.year}년 · {data.periodLabel}
            </h1>
          </div>

          <div className="cb-report__tablewrap">
            <table className="cb-report-table">
              <thead>
                <tr>
                  <th className="cb-report-table__corner">구분</th>
                  {data.columns.map((c) => (
                    <th key={c.yearMonth}>
                      <div>{c.monthLabel}</div>
                      <div className="cb-report-table__sub">{c.rangeLabel}</div>
                    </th>
                  ))}
                  <th className="cb-report-table__sum">{data.periodLabel}합계</th>
                </tr>
              </thead>
              <tbody>
                {data.rows.map((row) => (
                  <tr key={row.key}>
                    <th scope="row">{row.label}</th>
                    {row.values.map((v, i) => (
                      <td key={i} className="cb-num">
                        {formatMoney(v)}
                      </td>
                    ))}
                    <td className="cb-num cb-report-table__sumcell">{formatMoney(row.periodTotal)}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <p className="cb-report__footnotes">
            * 저축/보험은 「저축」 거래 중 분류에 「보험」이 포함된 것만 보험 행으로 집계합니다.
            <br />
            * 대출·지출은 「지출」 거래 중 분류에 「대출」 포함 여부로 나눕니다.
          </p>

          <section className="cb-report__chart">
            <h2 className="cb-report__h2">월별 지출 추이</h2>
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
                    formatter={(value: number) => [formatMoney(value), "지출"]}
                    labelFormatter={(l) => String(l)}
                  />
                  <Legend />
                  <Line
                    type="monotone"
                    dataKey="지출"
                    stroke="#30b06e"
                    strokeWidth={2}
                    dot={{ r: 4 }}
                    activeDot={{ r: 6 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            </div>
          </section>
        </>
      )}
    </main>
  );
}
