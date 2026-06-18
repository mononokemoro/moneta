import { useCallback, useEffect, useState } from "react";
import "./App.css";
import "./cashbook.css";
import "./report.css";
import "./settings.css";
import { fetchDay, type DayView } from "./api/cashbook";
import { loadLedgerBook, saveLedgerBook, type LedgerBook } from "./api/ledgerBook";
import { LeftSidebar } from "./components/LeftSidebar";
import { MainBoard } from "./components/MainBoard";
import { ReportView } from "./components/ReportView";
import { SettingsView } from "./components/SettingsView";
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
  const [keywordRefresh, setKeywordRefresh] = useState(0);

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

  useEffect(() => {
    if (view === "cashbook") {
      setKeywordRefresh((n) => n + 1);
    }
  }, [view]);

  return (
    <div className={`cb-shell${view !== "cashbook" ? " cb-shell--report" : ""}`}>
      {view === "cashbook" && (
        <div className="cb-cashbook-layout">
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
            keywordRefresh={keywordRefresh}
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
      {view === "settings" && (
        <div className="cb-settings-layout">
          <SettingsView book={book} onBookChange={handleBookChange} />
          <RightSidebar view={view} onViewChange={setView} day={day} />
        </div>
      )}
    </div>
  );
}
