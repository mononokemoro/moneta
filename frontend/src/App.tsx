import { useCallback, useEffect, useState } from "react";
import "./App.css";
import "./cashbook.css";
import "./report.css";
import { fetchDay, type DayView } from "./api/cashbook";
import { loadLedgerBook, saveLedgerBook, type LedgerBook } from "./api/ledgerBook";
import { LeftSidebar } from "./components/LeftSidebar";
import { LeftNavRail } from "./components/LeftNavRail";
import { MainBoard } from "./components/MainBoard";
import { ReportView } from "./components/ReportView";
import { RightSidebar, type SidebarView } from "./components/RightSidebar";
import { toIsoDate } from "./util/dateUtil";

export default function App() {
  const [view, setView] = useState<SidebarView>("cashbook");
  const [book, setBook] = useState<LedgerBook>(() => loadLedgerBook());
  const [date, setDate] = useState(() => toIsoDate(new Date()));
  const [day, setDay] = useState<DayView | null>(null);
  const [scheduleNote, setScheduleNote] = useState("");
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  const handleBookChange = useCallback((next: LedgerBook) => {
    setBook(next);
    saveLedgerBook(next);
  }, []);

  const reload = useCallback(async () => {
    const d = await fetchDay(date, book);
    setDay(d);
    setErr(null);
  }, [date, book]);

  useEffect(() => {
    if (view !== "cashbook") return;
    let cancelled = false;
    setLoading(true);
    setErr(null);
    fetchDay(date, book)
      .then((d) => {
        if (!cancelled) setDay(d);
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
  }, [date, book, view]);

  return (
    <div className={`cb-shell${view === "report" ? " cb-shell--report" : ""}`}>
      {view === "cashbook" && (
        <div className="cb-cashbook-layout">
          <LeftNavRail />
          <LeftSidebar
            book={book}
            onBookChange={handleBookChange}
            date={date}
            day={day}
            scheduleNote={scheduleNote}
            onScheduleChange={setScheduleNote}
            onSelectDate={setDate}
            onReload={reload}
          />
          <MainBoard
            book={book}
            date={date}
            day={day}
            loading={loading}
            error={err}
            scheduleNote={scheduleNote}
            onScheduleChange={setScheduleNote}
            onDateChange={setDate}
            onReload={reload}
          />
          <RightSidebar view={view} onViewChange={setView} day={day} />
        </div>
      )}
      {view === "report" && (
        <div className="cb-report-layout">
          <ReportView book={book} onBookChange={handleBookChange} />
          <RightSidebar view={view} onViewChange={setView} day={day} />
        </div>
      )}
    </div>
  );
}
