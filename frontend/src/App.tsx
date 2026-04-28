import { useCallback, useEffect, useState } from "react";
import "./App.css";
import "./cashbook.css";
import "./report.css";
import { fetchDay, type DayView } from "./api/cashbook";
import { LeftSidebar } from "./components/LeftSidebar";
import { LeftNavRail } from "./components/LeftNavRail";
import { MainBoard } from "./components/MainBoard";
import { ReportView } from "./components/ReportView";
import { RightSidebar, type SidebarView } from "./components/RightSidebar";
import { toIsoDate } from "./util/dateUtil";

export default function App() {
  const [view, setView] = useState<SidebarView>("cashbook");
  const [date, setDate] = useState(() => toIsoDate(new Date()));
  const [day, setDay] = useState<DayView | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  const reload = useCallback(async () => {
    const d = await fetchDay(date);
    setDay(d);
    setErr(null);
  }, [date]);

  useEffect(() => {
    if (view !== "cashbook") return;
    let cancelled = false;
    setLoading(true);
    setErr(null);
    fetchDay(date)
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
  }, [date, view]);

  return (
    <div className={`cb-shell${view === "report" ? " cb-shell--report" : ""}`}>
      {view === "cashbook" && (
        <div className="cb-cashbook-layout">
          <LeftNavRail />
          <LeftSidebar date={date} onSelectDate={setDate} />
          <MainBoard date={date} day={day} loading={loading} error={err} onReload={reload} />
          <RightSidebar view={view} onViewChange={setView} day={day} />
        </div>
      )}
      {view === "report" && (
        <div className="cb-report-layout">
          <ReportView />
          <RightSidebar view={view} onViewChange={setView} day={day} />
        </div>
      )}
    </div>
  );
}
