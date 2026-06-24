import { useEffect, useState } from "react";
import {
  createCategoryKeyword,
  deleteCategoryKeyword,
  fetchCategoryKeywords,
  updateCategoryKeyword,
  type CategoryKeyword,
} from "../api/categoryKeywords";
import type { TxType } from "../api/cashbook";
import type { LedgerBook } from "../api/ledgerBook";
import { MonetaHint, MonetaPanel } from "./MonetaPanel";

type Props = {
  book: LedgerBook;
  expenseCategories: string[];
  incomeCategories: string[];
};

const KEYWORD_TABS = [
  { id: "expense", label: "지출 예약어" },
  { id: "income", label: "수입 예약어" },
] as const;

type KeywordDraft = {
  key: string;
  id?: number;
  keyword: string;
  categoryName: string;
};

function emptyDraft(): KeywordDraft {
  return { key: `new-${Math.random().toString(36).slice(2)}`, keyword: "", categoryName: "" };
}

function fromKeyword(row: CategoryKeyword): KeywordDraft {
  return { key: String(row.id), id: row.id, keyword: row.keyword, categoryName: row.categoryName };
}

function KeywordTable({
  txType,
  book,
  categories,
  rows,
  onReload,
}: {
  txType: TxType;
  book: LedgerBook;
  categories: string[];
  rows: CategoryKeyword[];
  onReload: () => Promise<void>;
}) {
  const [drafts, setDrafts] = useState<KeywordDraft[]>([emptyDraft()]);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    const saved = rows.filter((r) => r.txType === txType).map(fromKeyword);
    setDrafts([...saved, emptyDraft()]);
  }, [rows, txType]);

  function patch(key: string, p: Partial<KeywordDraft>) {
    setDrafts((prev) => prev.map((d) => (d.key === key ? { ...d, ...p } : d)));
  }

  async function commitRow(d: KeywordDraft) {
    if (busy) return;
    const keyword = d.keyword.trim();
    const categoryName = d.categoryName.trim();
    if (!keyword || !categoryName) return;

    setBusy(true);
    setErr(null);
    try {
      if (d.id) {
        const orig = rows.find((r) => r.id === d.id);
        if (orig && orig.keyword === keyword && orig.categoryName === categoryName) return;
        await updateCategoryKeyword(d.id, { txType, keyword, categoryName });
      } else {
        await createCategoryKeyword(book, { txType, keyword, categoryName });
      }
      await onReload();
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setBusy(false);
    }
  }

  function handleBlur(d: KeywordDraft, e: React.FocusEvent<HTMLTableRowElement>) {
    if (e.currentTarget.contains(e.relatedTarget as Node)) return;
    void commitRow(d);
  }

  async function handleDelete(id: number) {
    if (busy) return;
    setBusy(true);
    setErr(null);
    try {
      await deleteCategoryKeyword(id, book);
      await onReload();
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "삭제 실패");
    } finally {
      setBusy(false);
    }
  }

  return (
    <>
      {err && <p className="cb-err cb-settings__err">{err}</p>}
      <table className="cb-prod__table">
        <thead>
          <tr>
            <th>예약어</th>
            <th>분류</th>
            <th className="cb-prod__colAction" />
          </tr>
        </thead>
        <tbody>
          {drafts.map((d) => (
            <tr key={d.key} onBlur={(e) => handleBlur(d, e)}>
              <td>
                <input
                  className="cb-prod__cell"
                  value={d.keyword}
                  placeholder="예: 이마트"
                  disabled={busy}
                  onChange={(e) => patch(d.key, { keyword: e.target.value })}
                />
              </td>
              <td>
                <input
                  className="cb-prod__cell"
                  list={`cat-kw-${txType}`}
                  value={d.categoryName}
                  placeholder="분류 선택"
                  disabled={busy}
                  onChange={(e) => patch(d.key, { categoryName: e.target.value })}
                />
              </td>
              <td>
                {d.id ? (
                  <button
                    type="button"
                    className="cb-catmgmt__miniBtn"
                    disabled={busy}
                    onClick={() => void handleDelete(d.id!)}
                  >
                    삭제
                  </button>
                ) : null}
              </td>
            </tr>
          ))}
        </tbody>
      </table>
      <datalist id={`cat-kw-${txType}`}>
        {categories.map((name) => (
          <option key={name} value={name} />
        ))}
      </datalist>
    </>
  );
}

export function KeywordSettingsPanel({ book, expenseCategories, incomeCategories }: Props) {
  const [tab, setTab] = useState<"expense" | "income">("expense");
  const [rows, setRows] = useState<CategoryKeyword[]>([]);
  const [loading, setLoading] = useState(true);

  const reload = async () => {
    const kw = await fetchCategoryKeywords(book);
    setRows(kw);
  };

  useEffect(() => {
    setLoading(true);
    reload().finally(() => setLoading(false));
  }, [book]);

  return (
    <MonetaPanel
      tabs={[...KEYWORD_TABS]}
      activeTab={tab}
      onTabChange={(id) => setTab(id as "expense" | "income")}
      hint={
        <MonetaHint>
          항목명에 예약어가 포함되면 해당 분류가 자동으로 선택됩니다. 긴 예약어가 우선 적용됩니다.
        </MonetaHint>
      }
      onSave={() => window.alert("예약어는 입력 후 포커스를 이동하면 자동 저장됩니다.")}
    >
      {loading ? (
        <p className="cb-muted">불러오는 중…</p>
      ) : (
        <KeywordTable
          txType={tab === "expense" ? "EXPENSE" : "INCOME"}
          book={book}
          categories={tab === "expense" ? expenseCategories : incomeCategories}
          rows={rows}
          onReload={reload}
        />
      )}
    </MonetaPanel>
  );
}
