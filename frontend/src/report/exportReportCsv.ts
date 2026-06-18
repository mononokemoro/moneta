import type { ReportData } from "../api/report";
import { formatMoney } from "../formatMoney";

function csvCell(v: string | number): string {
  const s = String(v);
  if (s.includes(",") || s.includes('"') || s.includes("\n")) {
    return `"${s.replace(/"/g, '""')}"`;
  }
  return s;
}

export function downloadReportCsv(data: ReportData): void {
  const lines: string[] = [];
  lines.push(data.title);

  if (data.viewMode === "matrix" && data.columns.length > 0) {
    const header = ["구분", ...data.columns.map((c) => c.monthLabel), `${data.periodLabel}합계`];
    lines.push(header.map(csvCell).join(","));
    for (const row of data.rows) {
      lines.push(
        [row.label, ...row.values.map(formatMoney), formatMoney(row.periodTotal)]
          .map(csvCell)
          .join(",")
      );
    }
  } else if (data.viewMode === "detail" && data.details.length > 0) {
    lines.push(["일자", "구분", "항목", "분류", "카드명", "금액", "비고"].map(csvCell).join(","));
    for (const d of data.details) {
      lines.push(
        [d.date, d.txType, d.title, d.category, d.cardName, formatMoney(d.amount), d.remarks]
          .map(csvCell)
          .join(",")
      );
    }
  } else if (data.viewMode === "calendar" && data.calendarDays.length > 0) {
    lines.push(["일자", "합계", "현금", "카드"].map(csvCell).join(","));
    for (const d of data.calendarDays) {
      lines.push(
        [d.date, formatMoney(d.total), formatMoney(d.cash), formatMoney(d.card)]
          .map(csvCell)
          .join(",")
      );
    }
  }

  const bom = "\uFEFF";
  const blob = new Blob([bom + lines.join("\n")], { type: "text/csv;charset=utf-8" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  a.href = url;
  a.download = `report-${data.year}-${data.category}-${data.subView}.csv`;
  a.click();
  URL.revokeObjectURL(url);
}
