import { useEffect, useState } from "react";
import {
  createCategoryKeyword,
  deleteCategoryKeyword,
  fetchCategoryKeywords,
  updateCategoryKeyword,
  type CategoryKeyword,
} from "../api/categoryKeywords";
import { fetchCategories, type CategoryList } from "../api/categories";
import type { TxType } from "../api/cashbook";
import type { LedgerBook } from "../api/ledgerBook";
import { BookSwitcher } from "./BookSwitcher";

type Props = {
  book: LedgerBook;
  onBookChange: (book: LedgerBook) => void;
};

type Draft = {
  key: string;
  id?: number;
  keyword: string;
  categoryName: string;
};

function emptyDraft(): Draft {
  return {
    key: `new-${Math.random().toString(36).slice(2)}`,
    keyword: "",
    categoryName: "",
  };
}

function fromRow(row: CategoryKeyword): Draft {
  return {
    key: String(row.id),
    id: row.id,
    keyword: row.keyword,
    categoryName: row.categoryName,
  };
}

function KeywordSection({
  title,
  txType,
  book,
  categories,
  rows,
  onReload,
}: {
  title: string;
  txType: TxType;
  book: LedgerBook;
  categories: string[];
  rows: CategoryKeyword[];
  onReload: () => Promise<void>;
}) {
  const [drafts, setDrafts] = useState<Draft[]>([emptyDraft()]);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  useEffect(() => {
    const saved = rows.filter((r) => r.txType === txType).map(fromRow);
    setDrafts([...saved, emptyDraft()]);
  }, [rows, txType]);

  function patch(key: string, patch: Partial<Draft>) {
    setDrafts((prev) => prev.map((d) => (d.key === key ? { ...d, ...patch } : d)));
  }

  async function commitRow(d: Draft) {
    if (busy) return;
    const keyword = d.keyword.trim();
    const categoryName = d.categoryName.trim();
    if (!keyword || !categoryName) return;

    setBusy(true);
    setErr(null);
    try {
      if (d.id) {
        const orig = rows.find((r) => r.id === d.id);
        if (
          orig &&
          orig.keyword === keyword &&
          orig.categoryName === categoryName
        ) {
          return;
        }
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

  function handleBlur(d: Draft, e: React.FocusEvent<HTMLTableRowElement>) {
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
    <section className="cb-settings__section">
      <h2 className="cb-settings__sectionTitle">{title}</h2>
      <p className="cb-settings__hint">
        항목명에 예약어가 포함되면 해당 분류가 자동으로 선택됩니다. 긴 예약어가 우선 적용됩니다.
      </p>
      {err && <p className="cb-err">{err}</p>}
      <div className="cb-settings__tablewrap">
        <table className="cb-table cb-table--inline cb-settings__table">
          <thead>
            <tr>
              <th>예약어</th>
              <th>분류</th>
              <th className="cb-col-check" />
            </tr>
          </thead>
          <tbody>
            {drafts.map((d) => (
              <tr
                key={d.key}
                className={d.id ? "cb-row--saved" : "cb-row--new"}
                onBlur={(e) => handleBlur(d, e)}
              >
                <td>
                  <input
                    className="cb-cell"
                    value={d.keyword}
                    placeholder="예: 이마트"
                    onChange={(e) => patch(d.key, { keyword: e.target.value })}
                    disabled={busy}
                  />
                </td>
                <td>
                  <input
                    className="cb-cell"
                    list={`cat-kw-${txType}`}
                    value={d.categoryName}
                    placeholder="분류 선택"
                    onChange={(e) => patch(d.key, { categoryName: e.target.value })}
                    disabled={busy}
                  />
                </td>
                <td>
                  {d.id ? (
                    <button
                      type="button"
                      className="cb-btn cb-btn--ghost cb-settings__del"
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
      </div>
    </section>
  );
}

export function SettingsView({ book, onBookChange }: Props) {
  const [keywords, setKeywords] = useState<CategoryKeyword[]>([]);
  const [categories, setCategories] = useState<CategoryList | null>(null);
  const [loading, setLoading] = useState(true);
  const [err, setErr] = useState<string | null>(null);

  const reload = async () => {
    const [kw, cats] = await Promise.all([fetchCategoryKeywords(book), fetchCategories(book)]);
    setKeywords(kw);
    setCategories(cats);
    setErr(null);
  };

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    reload()
      .catch((e: unknown) => {
        if (!cancelled) setErr(e instanceof Error ? e.message : "조회 실패");
      })
      .finally(() => {
        if (!cancelled) setLoading(false);
      });
    return () => {
      cancelled = true;
    };
  }, [book]);

  const expenseCats = categories?.expense.map((c) => c.name) ?? [];
  const incomeCats = categories?.income.map((c) => c.name) ?? [];

  return (
    <main className="cb-settings">
      <header className="cb-settings__top">
        <div>
          <h1 className="cb-settings__title">설정</h1>
          <p className="cb-muted">가계부 기능을 설정합니다.</p>
        </div>
        <BookSwitcher book={book} onChange={onBookChange} />
      </header>

      {loading && <p className="cb-muted">불러오는 중…</p>}
      {err && <p className="cb-err">{err}</p>}

      {!loading && !err && (
        <>
          <KeywordSection
            title="지출 예약어"
            txType="EXPENSE"
            book={book}
            categories={expenseCats}
            rows={keywords}
            onReload={reload}
          />
          <KeywordSection
            title="수입 예약어"
            txType="INCOME"
            book={book}
            categories={incomeCats}
            rows={keywords}
            onReload={reload}
          />
        </>
      )}
    </main>
  );
}
