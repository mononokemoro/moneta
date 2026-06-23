import { useState } from "react";
import {
  createCategory,
  deleteCategory,
  reorderCategories,
  updateCategory,
  type CategoryGroup,
  type CategoryItem,
  type CategoryType,
} from "../api/categories";
import type { LedgerBook } from "../api/ledgerBook";

type Props = {
  title: string;
  hint: string;
  categoryType: CategoryType;
  book: LedgerBook;
  groups: CategoryGroup[];
  onReload: () => Promise<void>;
};

function swapSortOrder(items: { id: number; sortOrder: number }[], index: number, dir: -1 | 1) {
  const target = index + dir;
  if (target < 0 || target >= items.length) return null;
  const a = items[index];
  const b = items[target];
  return { a: { id: a.id, sortOrder: b.sortOrder }, b: { id: b.id, sortOrder: a.sortOrder } };
}

export function CategoryHierarchySection({
  title,
  hint,
  categoryType,
  book,
  groups,
  onReload,
}: Props) {
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [newMajor, setNewMajor] = useState("");
  const [newMinorByMajor, setNewMinorByMajor] = useState<Record<number, string>>({});
  const [editingMajor, setEditingMajor] = useState<Record<number, string>>({});
  const [editingMinor, setEditingMinor] = useState<Record<number, string>>({});

  async function run(action: () => Promise<void>) {
    if (busy) return;
    setBusy(true);
    setErr(null);
    try {
      await action();
      await onReload();
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "처리 실패");
    } finally {
      setBusy(false);
    }
  }

  async function handleAddMajor() {
    const name = newMajor.trim();
    if (!name) return;
    await run(async () => {
      await createCategory(book, categoryType, "MAJOR", name);
      setNewMajor("");
    });
  }

  async function handleAddMinor(majorId: number) {
    const name = (newMinorByMajor[majorId] ?? "").trim();
    if (!name) return;
    await run(async () => {
      await createCategory(book, categoryType, "MINOR", name, majorId);
      setNewMinorByMajor((prev) => ({ ...prev, [majorId]: "" }));
    });
  }

  async function saveMajor(group: CategoryGroup) {
    const name = (editingMajor[group.id] ?? group.name).trim();
    if (!name || name === group.name) return;
    await run(async () => {
      await updateCategory(group.id, book, name);
    });
  }

  async function saveMinor(item: CategoryItem) {
    const name = (editingMinor[item.id] ?? item.name).trim();
    if (!name || name === item.name) return;
    await run(async () => {
      await updateCategory(item.id, book, name, item.parentId);
    });
  }

  async function moveMinor(item: CategoryItem, parentId: number) {
    if (item.parentId === parentId) return;
    await run(async () => {
      const siblings = groups.find((g) => g.id === parentId)?.children ?? [];
      await updateCategory(item.id, book, item.name, parentId);
      await reorderCategories(book, [
        { id: item.id, sortOrder: siblings.length, parentId },
      ]);
    });
  }

  async function reorderMajor(index: number, dir: -1 | 1) {
    const swapped = swapSortOrder(groups, index, dir);
    if (!swapped) return;
    await run(async () => {
      await reorderCategories(book, [
        { id: swapped.a.id, sortOrder: swapped.a.sortOrder, parentId: null },
        { id: swapped.b.id, sortOrder: swapped.b.sortOrder, parentId: null },
      ]);
    });
  }

  async function reorderMinor(majorId: number, index: number, dir: -1 | 1) {
    const major = groups.find((g) => g.id === majorId);
    if (!major) return;
    const swapped = swapSortOrder(major.children, index, dir);
    if (!swapped) return;
    await run(async () => {
      await reorderCategories(book, [
        { id: swapped.a.id, sortOrder: swapped.a.sortOrder, parentId: majorId },
        { id: swapped.b.id, sortOrder: swapped.b.sortOrder, parentId: majorId },
      ]);
    });
  }

  return (
    <section className="cb-settings__section">
      <h2 className="cb-settings__sectionTitle">{title}</h2>
      <p className="cb-settings__hint">{hint}</p>
      {err && <p className="cb-err">{err}</p>}

      <div className="cb-settings__hierarchy">
        {groups.map((group, majorIdx) => (
          <div key={group.id} className="cb-settings__majorBlock">
            <div className="cb-settings__majorRow">
              <div className="cb-settings__orderBtns">
                <button
                  type="button"
                  className="cb-settings__orderBtn"
                  disabled={busy || majorIdx === 0}
                  onClick={() => void reorderMajor(majorIdx, -1)}
                  aria-label="대분류 위로"
                >
                  ↑
                </button>
                <button
                  type="button"
                  className="cb-settings__orderBtn"
                  disabled={busy || majorIdx === groups.length - 1}
                  onClick={() => void reorderMajor(majorIdx, 1)}
                  aria-label="대분류 아래로"
                >
                  ↓
                </button>
              </div>
              <span className="cb-settings__tierLabel">대분류</span>
              <input
                className="cb-cell cb-settings__majorInput"
                value={editingMajor[group.id] ?? group.name}
                onChange={(e) =>
                  setEditingMajor((prev) => ({ ...prev, [group.id]: e.target.value }))
                }
                onBlur={() => void saveMajor(group)}
                disabled={busy}
              />
              <button
                type="button"
                className="cb-btn cb-btn--ghost cb-settings__del"
                disabled={busy}
                onClick={() => void run(() => deleteCategory(group.id, book))}
              >
                삭제
              </button>
            </div>

            <div className="cb-settings__minorList">
              {group.children.map((child, minorIdx) => (
                <div key={child.id} className="cb-settings__minorRow">
                  <div className="cb-settings__orderBtns">
                    <button
                      type="button"
                      className="cb-settings__orderBtn"
                      disabled={busy || minorIdx === 0}
                      onClick={() => void reorderMinor(group.id, minorIdx, -1)}
                      aria-label="소분류 위로"
                    >
                      ↑
                    </button>
                    <button
                      type="button"
                      className="cb-settings__orderBtn"
                      disabled={busy || minorIdx === group.children.length - 1}
                      onClick={() => void reorderMinor(group.id, minorIdx, 1)}
                      aria-label="소분류 아래로"
                    >
                      ↓
                    </button>
                  </div>
                  <span className="cb-settings__tierLabel">소분류</span>
                  <input
                    className="cb-cell"
                    value={editingMinor[child.id] ?? child.name}
                    onChange={(e) =>
                      setEditingMinor((prev) => ({ ...prev, [child.id]: e.target.value }))
                    }
                    onBlur={() => void saveMinor(child)}
                    disabled={busy}
                  />
                  <select
                    className="cb-cell cb-settings__moveSelect"
                    value={child.parentId ?? group.id}
                    disabled={busy}
                    onChange={(e) => void moveMinor(child, Number(e.target.value))}
                    aria-label="대분류 이동"
                  >
                    {groups.map((g) => (
                      <option key={g.id} value={g.id}>
                        {g.name}
                      </option>
                    ))}
                  </select>
                  <button
                    type="button"
                    className="cb-btn cb-btn--ghost cb-settings__del"
                    disabled={busy}
                    onClick={() => void run(() => deleteCategory(child.id, book))}
                  >
                    삭제
                  </button>
                </div>
              ))}

              <div className="cb-settings__minorRow cb-settings__minorRow--new">
                <span className="cb-settings__tierLabel">소분류</span>
                <input
                  className="cb-cell"
                  placeholder="새 소분류"
                  value={newMinorByMajor[group.id] ?? ""}
                  onChange={(e) =>
                    setNewMinorByMajor((prev) => ({ ...prev, [group.id]: e.target.value }))
                  }
                  onKeyDown={(e) => {
                    if (e.key === "Enter") void handleAddMinor(group.id);
                  }}
                  disabled={busy}
                />
                <button
                  type="button"
                  className="cb-btn cb-btn--ghost"
                  disabled={busy}
                  onClick={() => void handleAddMinor(group.id)}
                >
                  추가
                </button>
              </div>
            </div>
          </div>
        ))}

        <div className="cb-settings__majorRow cb-settings__majorRow--new">
          <span className="cb-settings__tierLabel">대분류</span>
          <input
            className="cb-cell cb-settings__majorInput"
            placeholder="새 대분류"
            value={newMajor}
            onChange={(e) => setNewMajor(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === "Enter") void handleAddMajor();
            }}
            disabled={busy}
          />
          <button
            type="button"
            className="cb-btn cb-btn--ghost"
            disabled={busy}
            onClick={() => void handleAddMajor()}
          >
            대분류 추가
          </button>
        </div>
      </div>
    </section>
  );
}
