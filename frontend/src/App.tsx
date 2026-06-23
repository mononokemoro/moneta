import { useCallback, useEffect, useState } from "react";
import "./App.css";
import "./cashbook.css";
import "./report.css";
import "./settings.css";
import "./data.css";
import { fetchDay, type DayView } from "./api/cashbook";
import { loadLedgerBook, saveLedgerBook, type LedgerBook } from "./api/ledgerBook";
import { LeftSidebar } from "./components/LeftSidebar";
import { MainBoard } from "./components/MainBoard";
import { ReportView } from "./components/ReportView";
import { DataView } from "./components/DataView";
import { SettingsView, type SettingsSection } from "./components/SettingsView";
import { RightSidebar, type SidebarView } from "./components/RightSidebar";
import { toIsoDate } from "./util/dateUtil";
import { confirmLeaveUnsaved } from "./util/confirmDialog";

export default function App() {
  const [view, setView] = useState<SidebarView>("cashbook");
  const [book, setBook] = useState<LedgerBook>(() => loadLedgerBook());
  const [date, setDate] = useState(() => toIsoDate(new Date()));
  const [day, setDay] = useState<DayView | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [keywordRefresh, setKeywordRefresh] = useState(0);
  const [calendarRefresh, setCalendarRefresh] = useState(0);
  const [tablesUnsaved, setTablesUnsaved] = useState(false);
  const [sidebarUnsaved, setSidebarUnsaved] = useState(false);
  const [settingsSection, setSettingsSection] = useState<SettingsSection>("categories");
  const hasUnsaved = tablesUnsaved || sidebarUnsaved;

  const handleBookChange = useCallback((next: LedgerBook) => {
    setBook(next);
    saveLedgerBook(next);
  }, []);

  const tryDateChange = useCallback(
    (next: string) => {
      if (next === date) return;
      if (hasUnsaved && !confirmLeaveUnsaved()) return;
      setDate(next);
    },
    [date, hasUnsaved],
  );

  const openSettings = useCallback(
    (section: SettingsSection = "categories") => {
      if (view === "cashbook" && hasUnsaved && !confirmLeaveUnsaved()) return;
      setSettingsSection(section);
      setView("settings");
    },
    [view, hasUnsaved],
  );

  const tryViewChange = useCallback(
    (next: SidebarView) => {
      if (next === view) return;
      if (view === "cashbook" && hasUnsaved && !confirmLeaveUnsaved()) return;
      setView(next);
    },
    [view, hasUnsaved],
  );

  const tryBookChange = useCallback(
    (next: LedgerBook) => {
      if (next === book) return;
      if (hasUnsaved && !confirmLeaveUnsaved()) return;
      handleBookChange(next);
    },
    [book, hasUnsaved, handleBookChange],
  );

  const reload = useCallback(async () => {
    const d = await fetchDay(date, book);
    setDay(d);
    setErr(null);
    setCalendarRefresh((n) => n + 1);
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
            onBookChange={tryBookChange}
            date={date}
            day={day}
            onSelectDate={tryDateChange}
            onReload={reload}
            calendarRefresh={calendarRefresh}
            onDirtyChange={setSidebarUnsaved}
            onOpenFixedSettings={() => openSettings("fixed")}
          />
          <MainBoard
            book={book}
            date={date}
            day={day}
            loading={loading}
            error={err}
            onDateChange={tryDateChange}
            onReload={reload}
            keywordRefresh={keywordRefresh}
            onUnsavedChange={setTablesUnsaved}
          />
          <RightSidebar view={view} onViewChange={tryViewChange} date={date} day={day} />
        </div>
      )}
      {view === "report" && (
        <div className="cb-report-layout">
          <ReportView book={book} onBookChange={handleBookChange} />
          <RightSidebar view={view} onViewChange={tryViewChange} date={date} day={day} />
        </div>
      )}
      {view === "data" && (
        <div className="cb-data-layout">
          <DataView />
          <RightSidebar view={view} onViewChange={tryViewChange} date={date} day={day} />
        </div>
      )}
      {view === "settings" && (
        <div className="cb-settings-layout">
          <SettingsView
            book={book}
            onBookChange={handleBookChange}
            section={settingsSection}
            onSectionChange={setSettingsSection}
          />
          <RightSidebar view={view} onViewChange={tryViewChange} date={date} day={day} />
        </div>
      )}
    </div>
  );
}
