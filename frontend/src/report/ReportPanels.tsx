import { formatMoney } from "../formatMoney";
import type { ReportData } from "../api/report";

type Props = { data: ReportData };

export function ReportMatrix({ data }: Props) {
  return (
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
  );
}

export function ReportDetail({ data }: Props) {
  if (data.details.length === 0) {
    return <p className="cb-muted">해당 기간에 내역이 없습니다.</p>;
  }
  return (
    <div className="cb-report__tablewrap">
      <table className="cb-report-table cb-report-table--detail">
        <thead>
          <tr>
            <th>일자</th>
            <th>구분</th>
            <th>항목</th>
            <th>분류</th>
            <th>카드명</th>
            <th className="cb-num">금액</th>
            <th>비고</th>
          </tr>
        </thead>
        <tbody>
          {data.details.map((d, i) => (
            <tr key={`${d.date}-${d.title}-${i}`}>
              <td>{d.date}</td>
              <td>{d.txType}</td>
              <td>{d.title}</td>
              <td>{d.category}</td>
              <td>{d.cardName}</td>
              <td className="cb-num">{formatMoney(d.amount)}</td>
              <td>{d.remarks}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

export function ReportCalendar({ data }: Props) {
  const byMonth = new Map<string, typeof data.calendarDays>();
  for (const d of data.calendarDays) {
    const ym = d.date.slice(0, 7);
    if (!byMonth.has(ym)) byMonth.set(ym, []);
    byMonth.get(ym)!.push(d);
  }

  const weekdays = ["일", "월", "화", "수", "목", "금", "토"];

  return (
    <div className="cb-report-cal">
      {[...byMonth.entries()].map(([ym, days]) => {
        const [y, m] = ym.split("-").map(Number);
        const firstWd = new Date(y, m - 1, 1).getDay();
        const dim = new Date(y, m, 0).getDate();
        const dayMap = new Map(days.map((d) => [Number(d.date.slice(8, 10)), d]));
        const cells: (number | null)[] = [];
        for (let i = 0; i < firstWd; i++) cells.push(null);
        for (let d = 1; d <= dim; d++) cells.push(d);

        return (
          <section key={ym} className="cb-report-cal__month">
            <h3 className="cb-report-cal__title">
              {y}년 {m}월
            </h3>
            <div className="cb-report-cal__weekdays">
              {weekdays.map((w) => (
                <span key={w}>{w}</span>
              ))}
            </div>
            <div className="cb-report-cal__grid">
              {cells.map((cell, i) => {
                if (cell === null) {
                  return <span key={`e-${i}`} className="cb-report-cal__cell cb-report-cal__cell--empty" />;
                }
                const info = dayMap.get(cell);
                const amt = info?.total ?? 0;
                const intensity = amt > 0 ? Math.min(1, amt / 500000) : 0;
                return (
                  <div
                    key={cell}
                    className={`cb-report-cal__cell${amt > 0 ? " has-data" : ""}`}
                    style={
                      amt > 0
                        ? { background: `rgba(49, 130, 246, ${0.12 + intensity * 0.45})` }
                        : undefined
                    }
                    title={
                      info
                        ? `합계 ${formatMoney(info.total)} (현금 ${formatMoney(info.cash)} / 카드 ${formatMoney(info.card)})`
                        : undefined
                    }
                  >
                    <span className="cb-report-cal__day">{cell}</span>
                    {amt > 0 && <span className="cb-report-cal__amt">{formatMoney(amt)}</span>}
                  </div>
                );
              })}
            </div>
          </section>
        );
      })}
    </div>
  );
}
