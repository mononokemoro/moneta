const NO_STORE: RequestInit = {
  cache: "no-store",
  headers: {
    "Cache-Control": "no-cache, no-store, max-age=0, must-revalidate",
    Pragma: "no-cache",
  },
};

export interface ColumnInfo {
  name: string;
  description: string;
  type: string;
  maxLength: number | null;
  nullable: boolean;
  defaultValue: string | null;
  primaryKey: boolean;
}

export interface TableRelation {
  fromTable: string;
  fromColumn: string;
  toTable: string;
  toColumn: string;
  kind: string;
  description: string;
}

export interface TableSummary {
  name: string;
  description: string;
  entity: string;
  rowCount: number;
  columns: ColumnInfo[];
}

export interface DatabaseOverview {
  databaseProduct: string;
  jdbcUrl: string;
  schema: string;
  totalRows: number;
  tables: TableSummary[];
  relations: TableRelation[];
}

export interface TableRowsResponse {
  table: string;
  total: number;
  offset: number;
  limit: number;
  columns: string[];
  rows: Record<string, unknown>[];
}

export interface BackupExport {
  version: number;
  exportedAt: string;
  databaseProduct: string;
  tables: Record<
    string,
    {
      columns: string[];
      rows: Record<string, unknown>[];
    }
  >;
}

export interface RestoreResult {
  restoredAt: string;
  rowCounts: Record<string, number>;
  totalRows: number;
}

async function parseError(res: Response): Promise<string> {
  try {
    const body = (await res.json()) as { message?: string };
    return body.message ?? `HTTP ${res.status}`;
  } catch {
    return `HTTP ${res.status}`;
  }
}

export async function fetchDatabaseOverview(): Promise<DatabaseOverview> {
  const res = await fetch("/api/database/overview", NO_STORE);
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

export async function fetchTableRows(
  table: string,
  offset = 0,
  limit = 50,
): Promise<TableRowsResponse> {
  const q = new URLSearchParams({ offset: String(offset), limit: String(limit) });
  const res = await fetch(`/api/database/tables/${encodeURIComponent(table)}/rows?${q}`, NO_STORE);
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

export async function downloadDatabaseBackup(): Promise<BackupExport> {
  const res = await fetch("/api/database/backup", NO_STORE);
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

export async function restoreDatabaseBackup(backup: BackupExport): Promise<RestoreResult> {
  const res = await fetch("/api/database/restore", {
    method: "POST",
    headers: { "Content-Type": "application/json", ...NO_STORE.headers },
    body: JSON.stringify(backup),
    cache: "no-store",
  });
  if (!res.ok) throw new Error(await parseError(res));
  return res.json();
}

export function triggerBackupDownload(backup: BackupExport): void {
  const blob = new Blob([JSON.stringify(backup, null, 2)], { type: "application/json" });
  const url = URL.createObjectURL(blob);
  const a = document.createElement("a");
  const stamp = backup.exportedAt.replace(/[:.]/g, "").slice(0, 15);
  a.href = url;
  a.download = `pininicong-cashbook-backup-${stamp}.json`;
  a.click();
  URL.revokeObjectURL(url);
}

export async function readBackupFile(file: File): Promise<BackupExport> {
  const text = await file.text();
  const parsed = JSON.parse(text) as BackupExport;
  if (!parsed.version || !parsed.tables) {
    throw new Error("유효하지 않은 백업 파일 형식입니다.");
  }
  return parsed;
}
