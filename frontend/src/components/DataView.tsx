import { useCallback, useEffect, useState } from "react";

import {
  downloadDatabaseBackup,
  executeSqlQuery,
  fetchDatabaseOverview,
  fetchTableRows,
  readBackupFile,
  restoreDatabaseBackup,
  triggerBackupDownload,
  type DatabaseOverview,
  type SqlQueryResponse,
  type TableRowsResponse,
  type TableSummary,
} from "../api/databaseAdmin";

type DataSection = "overview" | "tables" | "query" | "backup";

const SECTIONS: { id: DataSection; label: string }[] = [
  { id: "overview", label: "Overview" },
  { id: "tables", label: "Tables" },
  { id: "query", label: "SQL" },
  { id: "backup", label: "Backup" },
];

const DEFAULT_SQL = `SELECT id, book, tx_date, tx_type, title, amount, category
FROM cb_transaction
ORDER BY id DESC`;

function relationKindClass(kind: string): string {
  if (kind.toLowerCase().includes("fk")) return "fk";
  if (kind === "soft") return "soft";
  if (kind === "partition") return "partition";
  return "other";
}

function formatCell(value: unknown): string {
  if (value == null) return "—";
  if (typeof value === "object") return JSON.stringify(value);
  return String(value);
}

function QueryResults({ result }: { result: SqlQueryResponse }) {
  if (result.columns.length === 0) {
    return <p className="cb-muted">결과가 없습니다.</p>;
  }
  return (
    <div className="cb-data__scrollX">
      <table className="cb-data__rowsTable">
        <thead>
          <tr>
            {result.columns.map((col) => (
              <th key={col}>
                <code>{col}</code>
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {result.rows.map((row, i) => (
            <tr key={i}>
              {result.columns.map((col) => (
                <td key={col} className="cb-data__mono" title={formatCell(row[col])}>
                  {formatCell(row[col])}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function SqlQueryPanel({ tables }: { tables: TableSummary[] }) {
  const [sql, setSql] = useState(DEFAULT_SQL);
  const [limit, setLimit] = useState("200");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [result, setResult] = useState<SqlQueryResponse | null>(null);

  async function runQuery() {
    setBusy(true);
    setErr(null);
    try {
      const parsedLimit = Math.min(500, Math.max(1, Number(limit.replace(/,/g, "")) || 200));
      const data = await executeSqlQuery(sql, parsedLimit);
      setResult(data);
    } catch (e: unknown) {
      setResult(null);
      setErr(e instanceof Error ? e.message : "쿼리 실행 실패");
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="cb-data__panel cb-data__panel--query">
      <p className="cb-data__hint">
        SELECT 문만 실행됩니다 (최대 500행). Ctrl+Enter로 실행할 수 있습니다.
      </p>

      <div className="cb-data__queryShortcuts">
        <span className="cb-data__queryShortcutsLabel">테이블:</span>
        {tables.map((t) => (
          <button
            key={t.name}
            type="button"
            className="cb-btn cb-btn--ghost cb-btn--xs cb-data__queryTableBtn"
            title={t.name}
            onClick={() =>
              setSql(`SELECT *\nFROM ${t.name}\nORDER BY 1\nFETCH FIRST 50 ROWS ONLY`)
            }
          >
            {t.description}
          </button>
        ))}
      </div>

      <label className="cb-data__sqlLabel">
        SQL
        <textarea
          className="cb-data__sqlInput"
          value={sql}
          spellCheck={false}
          rows={8}
          onChange={(e) => setSql(e.target.value)}
          onKeyDown={(e) => {
            if (e.key === "Enter" && (e.ctrlKey || e.metaKey)) {
              e.preventDefault();
              void runQuery();
            }
          }}
        />
      </label>

      <div className="cb-data__queryActions">
        <label className="cb-data__queryLimit">
          최대 행
          <input
            className="cb-data__queryLimitInput"
            type="text"
            inputMode="numeric"
            value={limit}
            onChange={(e) => setLimit(e.target.value.replace(/[^\d]/g, ""))}
          />
        </label>
        <button
          type="button"
          className="cb-btn cb-btn--primary"
          disabled={busy || !sql.trim()}
          onClick={() => void runQuery()}
        >
          {busy ? "실행 중…" : "실행"}
        </button>
      </div>

      {err && <p className="cb-data__error">{err}</p>}

      {result && (
        <div className="cb-data__queryResult">
          <p className="cb-data__queryMeta">
            {result.rowCount.toLocaleString()}행 · {result.elapsedMs}ms
            {result.rowCount >= result.limit ? ` (최대 ${result.limit}행)` : ""}
          </p>
          <QueryResults result={result} />
        </div>
      )}
    </section>
  );
}

function RelationsPanel({ relations }: { relations: DatabaseOverview["relations"] }) {
  return (
    <section className="cb-data__relations" aria-label="테이블 연계">
      <div className="cb-data__relationsHead">
        <h2 className="cb-data__sectionTitle">테이블 연계</h2>
        <p className="cb-data__hint">
          JPA 엔티티 간 논리적 연계입니다. DB에 물리 FK가 없는 soft/partition 관계도 포함합니다.
        </p>
      </div>
      <div className="cb-data__scrollX cb-data__scrollX--relations">
        <table className="cb-data__relationTable">
          <thead>
            <tr>
              <th>출발</th>
              <th>컬럼</th>
              <th>→</th>
              <th>대상</th>
              <th>컬럼</th>
              <th>종류</th>
              <th>설명</th>
            </tr>
          </thead>
          <tbody>
            {relations.map((r, i) => (
              <tr key={i}>
                <td>
                  <code>{r.fromTable}</code>
                </td>
                <td>
                  <code>{r.fromColumn}</code>
                </td>
                <td>→</td>
                <td>
                  <code>{r.toTable}</code>
                </td>
                <td>
                  <code>{r.toColumn}</code>
                </td>
                <td>
                  <span className={`cb-data__kind cb-data__kind--${relationKindClass(r.kind)}`}>
                    {r.kind}
                  </span>
                </td>
                <td>{r.description}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </section>
  );
}

function TableDetail({ table }: { table: TableSummary }) {
  const [rows, setRows] = useState<TableRowsResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [offset, setOffset] = useState(0);
  const limit = 30;

  const load = useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      const data = await fetchTableRows(table.name, offset, limit);
      setRows(data);
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "조회 실패");
    } finally {
      setLoading(false);
    }
  }, [table.name, offset]);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <div className="cb-data__tableDetail">
      <div className="cb-data__tableDetailHead">
        <h3>
          <code>{table.name}</code>
          <span className="cb-data__entity">{table.entity}</span>
        </h3>
        {table.description && <p className="cb-data__tableDesc">{table.description}</p>}
        <span className="cb-data__badge">{table.rowCount.toLocaleString()} rows</span>
      </div>

      <div className="cb-data__columns">
        <h4>컬럼</h4>
        <table className="cb-data__schemaTable">
          <thead>
            <tr>
              <th>설명</th>
              <th>이름</th>
              <th>타입</th>
              <th>길이</th>
              <th>NULL</th>
              <th>PK</th>
              <th>기본값</th>
            </tr>
          </thead>
          <tbody>
            {table.columns.map((col) => (
              <tr key={col.name}>
                <td className="cb-data__desc">{col.description}</td>
                <td>
                  <code>{col.name}</code>
                </td>
                <td>{col.type}</td>
                <td>{col.maxLength ?? "—"}</td>
                <td>{col.nullable ? "Y" : "N"}</td>
                <td>{col.primaryKey ? "✓" : ""}</td>
                <td className="cb-data__mono">{col.defaultValue ?? "—"}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <div className="cb-data__sample">
        <div className="cb-data__sampleHead">
          <h4>샘플 데이터</h4>
          <div className="cb-data__pager">
            <button
              type="button"
              className="cb-btn cb-btn--ghost cb-btn--xs"
              disabled={offset <= 0 || loading}
              onClick={() => setOffset((o) => Math.max(0, o - limit))}
            >
              이전
            </button>
            <span>
              {offset + 1}–{Math.min(offset + limit, rows?.total ?? 0)} / {rows?.total ?? 0}
            </span>
            <button
              type="button"
              className="cb-btn cb-btn--ghost cb-btn--xs"
              disabled={loading || !rows || offset + limit >= rows.total}
              onClick={() => setOffset((o) => o + limit)}
            >
              다음
            </button>
          </div>
        </div>
        {err && <p className="cb-data__error">{err}</p>}
        {loading && <p className="cb-muted">불러오는 중…</p>}
        {!loading && rows && rows.rows.length === 0 && (
          <p className="cb-muted">데이터가 없습니다.</p>
        )}
        {!loading && rows && rows.rows.length > 0 && (
          <div className="cb-data__scrollX">
            <table className="cb-data__rowsTable">
              <thead>
                <tr>
                  {rows.columns.map((col) => (
                    <th key={col}>
                      <code>{col}</code>
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody>
                {rows.rows.map((row, i) => (
                  <tr key={i}>
                    {rows.columns.map((col) => (
                      <td key={col} className="cb-data__mono" title={formatCell(row[col])}>
                        {formatCell(row[col])}
                      </td>
                    ))}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
}

export function DataView() {
  const [section, setSection] = useState<DataSection>("overview");
  const [overview, setOverview] = useState<DatabaseOverview | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);
  const [selectedTable, setSelectedTable] = useState<string | null>(null);
  const [backupBusy, setBackupBusy] = useState(false);
  const [restoreBusy, setRestoreBusy] = useState(false);
  const [statusMsg, setStatusMsg] = useState<string | null>(null);

  const reload = useCallback(async () => {
    setLoading(true);
    setErr(null);
    try {
      const data = await fetchDatabaseOverview();
      setOverview(data);
      if (!selectedTable && data.tables.length > 0) {
        setSelectedTable(data.tables[0].name);
      }
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "조회 실패");
    } finally {
      setLoading(false);
    }
  }, [selectedTable]);

  useEffect(() => {
    reload();
  }, [reload]);

  async function handleBackup() {
    setBackupBusy(true);
    setStatusMsg(null);
    try {
      const backup = await downloadDatabaseBackup();
      triggerBackupDownload(backup);
      const tableCount = Object.keys(backup.tables).length;
      const rows = Object.values(backup.tables).reduce((n, t) => n + t.rows.length, 0);
      setStatusMsg(`백업 완료 — ${tableCount}개 테이블, ${rows.toLocaleString()}행`);
    } catch (e: unknown) {
      setStatusMsg(e instanceof Error ? e.message : "백업 실패");
    } finally {
      setBackupBusy(false);
    }
  }

  async function handleRestore(file: File) {
    if (
      !window.confirm(
        "현재 DB의 모든 데이터가 삭제되고 백업 내용으로 교체됩니다.\n계속하시겠습니까?",
      )
    ) {
      return;
    }
    setRestoreBusy(true);
    setStatusMsg(null);
    try {
      const backup = await readBackupFile(file);
      const result = await restoreDatabaseBackup(backup);
      setStatusMsg(
        `복원 완료 — 총 ${result.totalRows.toLocaleString()}행 (${result.restoredAt})`,
      );
      await reload();
    } catch (e: unknown) {
      setStatusMsg(e instanceof Error ? e.message : "복원 실패");
    } finally {
      setRestoreBusy(false);
    }
  }

  const activeTable = overview?.tables.find((t) => t.name === selectedTable) ?? null;

  return (
    <main className="cb-data">
      <header className="cb-data__head">
        <h1 className="cb-data__title">데이터 (개발자)</h1>
        <button type="button" className="cb-btn cb-btn--ghost cb-btn--xs" onClick={() => reload()}>
          새로고침
        </button>
      </header>

      <nav className="cb-data__tabs" aria-label="데이터 하위 메뉴">
        {SECTIONS.map((s) => (
          <button
            key={s.id}
            type="button"
            className={section === s.id ? "is-active" : ""}
            onClick={() => setSection(s.id)}
          >
            {s.label}
          </button>
        ))}
      </nav>

      {loading && <p className="cb-muted">불러오는 중…</p>}
      {err && <p className="cb-data__error">{err}</p>}

      {!loading && overview && section === "overview" && (
        <section className="cb-data__panel">
          <dl className="cb-data__meta">
            <div>
              <dt>DB</dt>
              <dd>{overview.databaseProduct}</dd>
            </div>
            <div>
              <dt>스키마</dt>
              <dd>{overview.schema}</dd>
            </div>
            <div className="cb-data__metaGrow">
              <dt>JDBC URL</dt>
              <dd className="cb-data__mono">{overview.jdbcUrl}</dd>
            </div>
            <div>
              <dt>총 행 수</dt>
              <dd>{overview.totalRows.toLocaleString()}</dd>
            </div>
            <div>
              <dt>테이블 수</dt>
              <dd>{overview.tables.length}</dd>
            </div>
          </dl>

          <table className="cb-data__summaryTable">
            <thead>
              <tr>
                <th>설명</th>
                <th>테이블</th>
                <th>엔티티</th>
                <th>행 수</th>
                <th>컬럼 수</th>
              </tr>
            </thead>
            <tbody>
              {overview.tables.map((t) => (
                <tr key={t.name}>
                  <td className="cb-data__desc">{t.description}</td>
                  <td>
                    <code>{t.name}</code>
                  </td>
                  <td>{t.entity}</td>
                  <td className="cb-data__num">{t.rowCount.toLocaleString()}</td>
                  <td className="cb-data__num">{t.columns.length}</td>
                </tr>
              ))}
            </tbody>
          </table>

          <RelationsPanel relations={overview.relations} />
        </section>
      )}

      {!loading && overview && section === "tables" && (
        <section className="cb-data__panel cb-data__panel--split">
          <ul className="cb-data__tableList">
            {overview.tables.map((t) => (
              <li key={t.name}>
                <button
                  type="button"
                  className={selectedTable === t.name ? "is-active" : ""}
                  onClick={() => {
                    setSelectedTable(t.name);
                  }}
                >
                  <code>{t.name}</code>
                  <span>{t.rowCount.toLocaleString()}</span>
                </button>
              </li>
            ))}
          </ul>
          {activeTable ? (
            <TableDetail key={activeTable.name} table={activeTable} />
          ) : (
            <p className="cb-muted">테이블을 선택하세요.</p>
          )}
        </section>
      )}

      {!loading && overview && section === "query" && (
        <SqlQueryPanel tables={overview.tables} />
      )}

      {section === "backup" && (
        <section className="cb-data__panel">
          <div className="cb-data__backupBlock">
            <h3>전체 백업</h3>
            <p>모든 cb_* 테이블의 원본 컬럼·행을 JSON 파일로보냅니다.</p>
            <button
              type="button"
              className="cb-btn cb-btn--primary"
              disabled={backupBusy}
              onClick={() => void handleBackup()}
            >
              {backupBusy ? "백업 중…" : "백업 다운로드"}
            </button>
          </div>

          <div className="cb-data__backupBlock">
            <h3>전체 복원</h3>
            <p>
              백업 JSON을 업로드하면 기존 데이터를 모두 삭제한 뒤 백업 내용으로 교체합니다. ID 시퀀스도
              재설정됩니다.
            </p>
            <label className="cb-data__fileLabel">
              <input
                type="file"
                accept="application/json,.json"
                disabled={restoreBusy}
                onChange={(e) => {
                  const file = e.target.files?.[0];
                  e.target.value = "";
                  if (file) void handleRestore(file);
                }}
              />
              <span className="cb-btn cb-btn--ghost">{restoreBusy ? "복원 중…" : "백업 파일 선택"}</span>
            </label>
          </div>

          {statusMsg && <p className="cb-data__status">{statusMsg}</p>}
        </section>
      )}
    </main>
  );
}
