import { Fragment, useEffect, useState } from "react";
import {
  createCategory,
  deleteCategory,
  reorderCategories,
  saveCategoryPreferences,
  updateCategory,
  type CategoryGroup,
  type CategoryItem,
  type CategoryList,
  type CategoryType,
} from "../api/categories";
import type { LedgerBook } from "../api/ledgerBook";
import {
  addMajorDraft,
  addMinorDraft,
  applyTreeSavePlan,
  buildSavePlan,
  editableFromGroups,
  hasTreeChanges,
  isDeletableNode,
  isEditableName,
  majorOrderChanged,
  majorReorderItems,
  markSelectedForDelete,
  minorOrderChanged,
  minorReorderItems,
  moveMajor,
  relocateMinor,
  minorParentChanged,
  resetDraftSeq,
  updateNodeName,
  validateSavePlan,
  visibleGroups,
  type EditableGroup,
} from "../util/categoryEditTree";
import { MonetaHint } from "./MonetaPanel";
import { SettingsSectionToolbar } from "./SettingsSectionToolbar";

type Props = {
  book: LedgerBook;
  categories: CategoryList;
  onReload: () => Promise<void>;
};

type MainTab = "add-delete" | "my-settings";

function allMinors(groups: CategoryGroup[]): CategoryItem[] {
  return groups.flatMap((g) => g.children);
}

function prefsFromCategories(categories: CategoryList) {
  const disabled = new Set<number>();
  for (const g of [...categories.income, ...categories.expense]) {
    if (!g.enabled) disabled.add(g.id);
    for (const c of g.children) {
      if (!c.enabled) disabled.add(c.id);
    }
  }
  return disabled;
}

function syncEditTrees(categories: CategoryList) {
  resetDraftSeq();
  return {
    income: editableFromGroups(categories.income),
    expense: editableFromGroups(categories.expense),
  };
}

type DragMeta = { kind: "major" | "minor"; key: string; majorKey?: string };

function CategoryTreeColumn({
  title,
  tone,
  groups,
  expandedMajorKeys,
  activeMajorKey,
  focusKey,
  checkedKeys,
  disabled,
  onExpandMajor,
  onToggleMajor,
  onToggleCheck,
  onNameChange,
  onAddMajor,
  onAddMinor,
  onDeleteSelected,
  onMoveMajor,
  onRelocateMinor,
}: {
  title: string;
  tone: "income" | "expense";
  groups: EditableGroup[];
  expandedMajorKeys: Set<string>;
  activeMajorKey: string | null;
  focusKey: string | null;
  checkedKeys: Set<string>;
  disabled?: boolean;
  onExpandMajor: (key: string) => void;
  onToggleMajor: (key: string) => void;
  onToggleCheck: (key: string) => void;
  onNameChange: (key: string, name: string) => void;
  onAddMajor: () => void;
  onAddMinor: (majorKey: string) => void;
  onDeleteSelected: () => void;
  onMoveMajor: (dragKey: string, dropKey: string) => void;
  onRelocateMinor: (
    fromMajorKey: string,
    dragKey: string,
    toMajorKey: string,
    beforeKey: string | null
  ) => void;
}) {
  const [dragMeta, setDragMeta] = useState<DragMeta | null>(null);
  const [dropKey, setDropKey] = useState<string | null>(null);
  const visible = visibleGroups(groups);
  const selectedMajor =
    activeMajorKey && expandedMajorKeys.has(activeMajorKey)
      ? (visible.find((g) => g.key === activeMajorKey) ?? null)
      : null;
  const columnKeys = new Set<string>();
  for (const g of groups) {
    columnKeys.add(g.key);
    for (const c of g.children) columnKeys.add(c.key);
  }
  const hasDeleteSelection = [...checkedKeys].some((key) => columnKeys.has(key));

  function clearDrag() {
    setDragMeta(null);
    setDropKey(null);
  }

  function startDragMajor(key: string, e: React.DragEvent) {
    if (disabled) return;
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", key);
    setDragMeta({ kind: "major", key });
  }

  function startDragMinor(majorKey: string, key: string, e: React.DragEvent) {
    if (disabled) return;
    e.dataTransfer.effectAllowed = "move";
    e.dataTransfer.setData("text/plain", key);
    setDragMeta({ kind: "minor", key, majorKey });
  }

  function canDropOnMajor(targetKey: string): boolean {
    if (!dragMeta) return false;
    if (dragMeta.kind === "major") return dragMeta.key !== targetKey;
    return dragMeta.kind === "minor";
  }

  function canDropOnMinor(_majorKey: string, targetKey: string): boolean {
    return dragMeta?.kind === "minor" && dragMeta.key !== targetKey;
  }

  function handleDragOverRow(
    e: React.DragEvent,
    allowed: boolean,
    targetKey: string
  ) {
    if (!allowed) return;
    e.preventDefault();
    e.dataTransfer.dropEffect = "move";
    setDropKey(targetKey);
  }

  function handleDropMajor(targetKey: string) {
    if (!dragMeta) {
      clearDrag();
      return;
    }
    if (dragMeta.kind === "major") {
      if (dragMeta.key !== targetKey) onMoveMajor(dragMeta.key, targetKey);
      clearDrag();
      return;
    }
    if (dragMeta.kind === "minor" && dragMeta.majorKey) {
      onRelocateMinor(dragMeta.majorKey, dragMeta.key, targetKey, null);
    }
    clearDrag();
  }

  function handleDropMinor(majorKey: string, targetKey: string) {
    if (!dragMeta || dragMeta.kind !== "minor" || dragMeta.key === targetKey) {
      clearDrag();
      return;
    }
    if (!dragMeta.majorKey) {
      clearDrag();
      return;
    }
    onRelocateMinor(dragMeta.majorKey, dragMeta.key, majorKey, targetKey);
    clearDrag();
  }

  function renderDragHandle(
    onDragStart: (e: React.DragEvent) => void
  ) {
    return (
      <span
        className="cb-catmgmt__dragHandle"
        draggable={!disabled}
        aria-label="드래그하여 순서 변경"
        title="드래그하여 순서 변경 또는 다른 대분류로 이동"
        onDragStart={onDragStart}
        onDragEnd={clearDrag}
        onClick={(e) => e.stopPropagation()}
      >
        ⠿
      </span>
    );
  }

  function renderItemCell(key: string, name: string, minor = false) {
    if (isEditableName(groups, key)) {
      return (
        <input
          type="text"
          className="cb-cell"
          value={name}
          autoFocus={focusKey === key}
          placeholder="이름 입력"
          onChange={(e) => onNameChange(key, e.target.value)}
        />
      );
    }
    return (
      <span className={`cb-catmgmt__cellLabel${minor ? " cb-catmgmt__cellLabel--minor" : ""}`}>{name}</span>
    );
  }

  function renderMajorItem(groupKey: string, name: string, expanded: boolean) {
    return (
      <div className="cb-catmgmt__treeItem cb-catmgmt__treeItem--major">
        <button
          type="button"
          className="cb-catmgmt__treeToggle"
          aria-label={expanded ? "접기" : "펼치기"}
          onClick={(e) => {
            e.stopPropagation();
            onToggleMajor(groupKey);
          }}
        >
          {expanded ? "▼" : "▶"}
        </button>
        <div className="cb-catmgmt__treeContent">{renderItemCell(groupKey, name)}</div>
      </div>
    );
  }

  function renderMinorItem(
    childKey: string,
    name: string,
    linePos: "mid" | "last" | "only"
  ) {
    const lineClass =
      linePos === "last" || linePos === "only" ? " cb-catmgmt__treeItem--last" : "";
    return (
      <div className={`cb-catmgmt__treeItem cb-catmgmt__treeItem--minor${lineClass}`}>
        <span className="cb-catmgmt__treeLines" aria-hidden="true" />
        <div className="cb-catmgmt__treeContent">{renderItemCell(childKey, name, true)}</div>
      </div>
    );
  }

  function renderCheckCell(key: string) {
    if (!isDeletableNode(groups, key)) return null;
    return (
      <input
        type="checkbox"
        className="cb-scope-check"
        checked={checkedKeys.has(key)}
        onChange={() => onToggleCheck(key)}
      />
    );
  }

  return (
    <section className={`cb-panel cb-panel--excel cb-catmgmt__treePanel cb-catmgmt__treePanel--${tone}`}>
      <div className={`cb-panel__head cb-th cb-th--${tone}`}>
        <div className="cb-panel__headInner">
          <div className="cb-panel__title">
            <span>{title}</span>
          </div>
          <div className="cb-panel__headActions">
            <button type="button" className="cb-panel__headBtn" onClick={() => onAddMajor()}>
              + 대분류
            </button>
            <button
              type="button"
              className="cb-panel__headBtn"
              disabled={!selectedMajor}
              onClick={() => selectedMajor && onAddMinor(selectedMajor.key)}
            >
              + 소분류
            </button>
            <button
              type="button"
              className="cb-panel__headBtn"
              disabled={!hasDeleteSelection}
              onClick={onDeleteSelected}
            >
              선택 삭제
            </button>
          </div>
        </div>
      </div>
      <div className="cb-panel__tablewrap cb-catmgmt__treeTableWrap">
        <table className="cb-table cb-table--inline cb-table--excel cb-catmgmt__treeTable">
          <tbody>
            {visible.map((group) => {
              const expanded = expandedMajorKeys.has(group.key);
              const rowClass = group.isNew ? "cb-row--new" : "cb-row--saved";
              const activeClass = expanded ? " cb-catmgmt__majorRow--active" : "";
              const dropClass =
                dropKey === group.key && canDropOnMajor(group.key)
                  ? " cb-catmgmt__row--dropTarget"
                  : "";
              const childCount = group.children.length;
              return (
                <Fragment key={group.key}>
                  <tr
                    className={`${rowClass} cb-catmgmt__majorRow${activeClass}${dropClass}`}
                    onClick={() => onExpandMajor(group.key)}
                    onDragOver={(e) => handleDragOverRow(e, canDropOnMajor(group.key), group.key)}
                    onDragLeave={() => setDropKey(null)}
                    onDrop={(e) => {
                      e.preventDefault();
                      handleDropMajor(group.key);
                    }}
                  >
                    <td className="cb-catmgmt__dragCell" onClick={(e) => e.stopPropagation()}>
                      {renderDragHandle((e) => startDragMajor(group.key, e))}
                    </td>
                    <td className="cb-catmgmt__checkCell" onClick={(e) => e.stopPropagation()}>
                      {renderCheckCell(group.key)}
                    </td>
                    <td className="cb-catmgmt__treeCell" onClick={(e) => e.stopPropagation()}>
                      {renderMajorItem(group.key, group.name, expanded)}
                    </td>
                  </tr>
                  {expanded
                    ? group.children.map((child, idx) => {
                        const linePos: "mid" | "last" | "only" =
                          childCount === 1 ? "only" : idx === childCount - 1 ? "last" : "mid";
                        const minorDropClass =
                          dropKey === child.key && canDropOnMinor(group.key, child.key)
                            ? " cb-catmgmt__row--dropTarget"
                            : "";
                        return (
                          <tr
                            key={child.key}
                            className={`${child.isNew ? "cb-row--new" : "cb-row--saved"} cb-catmgmt__minorRow${minorDropClass}`}
                            onDragOver={(e) =>
                              handleDragOverRow(e, canDropOnMinor(group.key, child.key), child.key)
                            }
                            onDragLeave={() => setDropKey(null)}
                            onDrop={(e) => {
                              e.preventDefault();
                              handleDropMinor(group.key, child.key);
                            }}
                          >
                            <td className="cb-catmgmt__dragCell">
                              {renderDragHandle((e) => startDragMinor(group.key, child.key, e))}
                            </td>
                            <td className="cb-catmgmt__checkCell">{renderCheckCell(child.key)}</td>
                            <td className="cb-catmgmt__treeCell">
                              {renderMinorItem(child.key, child.name, linePos)}
                            </td>
                          </tr>
                        );
                      })
                    : null}
                  {expanded && childCount === 0 ? (
                    <tr className="cb-catmgmt__minorRow cb-catmgmt__minorRow--empty">
                      <td />
                      <td className="cb-catmgmt__checkCell" />
                      <td className="cb-catmgmt__treeCell">
                        <div className="cb-catmgmt__treeItem cb-catmgmt__treeItem--minor cb-catmgmt__treeItem--last">
                          <span className="cb-catmgmt__treeLines" aria-hidden="true" />
                          <span className="cb-catmgmt__cellLabel cb-catmgmt__cellLabel--minor cb-muted">
                            소분류 없음
                          </span>
                        </div>
                      </td>
                    </tr>
                  ) : null}
                </Fragment>
              );
            })}
          </tbody>
        </table>
      </div>
    </section>
  );
}

export function CategoryManagementPanel({ book, categories, onReload }: Props) {
  const [mainTab, setMainTab] = useState<MainTab>("add-delete");
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);
  const [disabledIds, setDisabledIds] = useState(() => prefsFromCategories(categories));
  const [incomeEdit, setIncomeEdit] = useState<EditableGroup[]>(() => editableFromGroups(categories.income));
  const [expenseEdit, setExpenseEdit] = useState<EditableGroup[]>(() => editableFromGroups(categories.expense));
  const [checkedDeleteKeys, setCheckedDeleteKeys] = useState<Set<string>>(() => new Set());
  const [expandedIncomeMajorKeys, setExpandedIncomeMajorKeys] = useState<Set<string>>(() => new Set());
  const [expandedExpenseMajorKeys, setExpandedExpenseMajorKeys] = useState<Set<string>>(() => new Set());
  const [activeIncomeMajorKey, setActiveIncomeMajorKey] = useState<string | null>(null);
  const [activeExpenseMajorKey, setActiveExpenseMajorKey] = useState<string | null>(null);
  const [focusKey, setFocusKey] = useState<string | null>(null);

  const incomeGroups = categories.income;
  const expenseGroups = categories.expense;

  useEffect(() => {
    setDisabledIds(prefsFromCategories(categories));
    const synced = syncEditTrees(categories);
    setIncomeEdit(synced.income);
    setExpenseEdit(synced.expense);
    setCheckedDeleteKeys(new Set());
    setFocusKey(null);
  }, [categories]);

  function toggleDeleteCheckKey(key: string) {
    setCheckedDeleteKeys((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
  }

  function addMajorToTree(type: CategoryType) {
    const setter = type === "INCOME" ? setIncomeEdit : setExpenseEdit;
    const setExpanded = type === "INCOME" ? setExpandedIncomeMajorKeys : setExpandedExpenseMajorKeys;
    const setActive = type === "INCOME" ? setActiveIncomeMajorKey : setActiveExpenseMajorKey;
    setter((prev) => {
      const { groups, key } = addMajorDraft(prev);
      setExpanded((expanded) => new Set(expanded).add(key));
      setActive(key);
      setFocusKey(key);
      return groups;
    });
    setErr(null);
  }

  function addMinorToTree(type: CategoryType, majorKey: string) {
    const setter = type === "INCOME" ? setIncomeEdit : setExpenseEdit;
    const setExpanded = type === "INCOME" ? setExpandedIncomeMajorKeys : setExpandedExpenseMajorKeys;
    const setActive = type === "INCOME" ? setActiveIncomeMajorKey : setActiveExpenseMajorKey;
    setter((prev) => {
      const { groups, key } = addMinorDraft(prev, majorKey);
      setExpanded((expanded) => new Set(expanded).add(majorKey));
      setActive(majorKey);
      setFocusKey(key);
      return groups;
    });
    setErr(null);
  }

  function deleteSelectedInTree(type: CategoryType) {
    const editGroups = type === "INCOME" ? incomeEdit : expenseEdit;
    const columnKeys = new Set<string>();
    for (const g of editGroups) {
      columnKeys.add(g.key);
      for (const c of g.children) columnKeys.add(c.key);
    }
    const selected = [...checkedDeleteKeys].filter((k) => columnKeys.has(k));
    if (selected.length === 0) {
      setErr("삭제할 항목을 선택하세요.");
      return;
    }
    const setter = type === "INCOME" ? setIncomeEdit : setExpenseEdit;
    setter((prev) => markSelectedForDelete(prev, new Set(selected)));
    setCheckedDeleteKeys((prev) => {
      const next = new Set(prev);
      for (const k of selected) next.delete(k);
      return next;
    });
    setErr(null);
  }

  async function saveTreeEdits(type: CategoryType, groups: EditableGroup[]) {
    const plan = buildSavePlan(groups);
    const validationErr = validateSavePlan(plan);
    if (validationErr) throw new Error(validationErr);
    await applyTreeSavePlan(book, type, groups, plan, {
      createCategory,
      updateCategory,
      deleteCategory,
    });
  }

  async function moveMajorInTree(type: CategoryType, dragKey: string, dropKey: string) {
    const setter = type === "INCOME" ? setIncomeEdit : setExpenseEdit;
    const current = type === "INCOME" ? incomeEdit : expenseEdit;
    const next = moveMajor(current, dragKey, dropKey);
    if (!next) return;
    setter(next);
    if (!majorOrderChanged(current, next)) return;
    const items = majorReorderItems(next);
    if (items.length === 0) return;
    setBusy(true);
    setErr(null);
    try {
      await reorderCategories(book, items);
    } catch (e: unknown) {
      setter(current);
      setErr(e instanceof Error ? e.message : "순서 변경 실패");
    } finally {
      setBusy(false);
    }
  }

  function ensureMajorExpanded(type: CategoryType, key: string) {
    const setExpanded = type === "INCOME" ? setExpandedIncomeMajorKeys : setExpandedExpenseMajorKeys;
    const setActive = type === "INCOME" ? setActiveIncomeMajorKey : setActiveExpenseMajorKey;
    setExpanded((prev) => {
      const next = new Set(prev);
      next.add(key);
      return next;
    });
    setActive(key);
  }

  async function relocateMinorInTree(
    type: CategoryType,
    fromMajorKey: string,
    dragKey: string,
    toMajorKey: string,
    beforeKey: string | null
  ) {
    const setter = type === "INCOME" ? setIncomeEdit : setExpenseEdit;
    const current = type === "INCOME" ? incomeEdit : expenseEdit;
    const next = relocateMinor(current, dragKey, fromMajorKey, toMajorKey, beforeKey);
    if (!next) return;
    setter(next);

    if (fromMajorKey !== toMajorKey) {
      ensureMajorExpanded(type, toMajorKey);
    }

    const parentChanged = minorParentChanged(current, next, dragKey, fromMajorKey, toMajorKey);
    const beforeFrom = current.find((g) => g.key === fromMajorKey);
    const afterFrom = next.find((g) => g.key === fromMajorKey);
    const beforeTo = current.find((g) => g.key === toMajorKey);
    const afterTo = next.find((g) => g.key === toMajorKey);
    const orderChanged =
      (beforeFrom &&
        afterFrom &&
        fromMajorKey === toMajorKey &&
        minorOrderChanged(beforeFrom, afterFrom)) ||
      (beforeFrom && afterFrom && fromMajorKey !== toMajorKey && minorOrderChanged(beforeFrom, afterFrom)) ||
      (beforeTo && afterTo && fromMajorKey !== toMajorKey && minorOrderChanged(beforeTo, afterTo));

    if (!parentChanged && !orderChanged) return;

    const moved = next.flatMap((g) => g.children).find((c) => c.key === dragKey);
    const targetGroup = next.find((g) => g.key === toMajorKey);
    if (parentChanged && moved?.id != null && targetGroup?.id == null) {
      return;
    }

    const items: { id: number; sortOrder: number; parentId: number }[] = [];
    for (const majorKey of new Set([fromMajorKey, toMajorKey])) {
      const group = next.find((g) => g.key === majorKey);
      if (group?.id != null) items.push(...minorReorderItems(group));
    }
    if (items.length === 0) return;

    setBusy(true);
    setErr(null);
    try {
      await reorderCategories(book, items);
    } catch (e: unknown) {
      setter(current);
      setErr(e instanceof Error ? e.message : "이동 실패");
    } finally {
      setBusy(false);
    }
  }

  async function handleSaveAddDelete() {
    if (busy) return;
    setBusy(true);
    setErr(null);
    try {
      await saveTreeEdits("INCOME", incomeEdit);
      await saveTreeEdits("EXPENSE", expenseEdit);
      await onReload();
      window.alert("저장되었습니다.");
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setBusy(false);
    }
  }

  async function handleSave() {
    if (mainTab === "add-delete") {
      await handleSaveAddDelete();
      return;
    }
    await handleSavePrefs();
  }

  async function handleSavePrefs() {
    if (busy) return;
    setBusy(true);
    setErr(null);
    try {
      const items = [...allMinors(incomeGroups), ...allMinors(expenseGroups)].map((c) => ({
        id: c.id,
        enabled: !disabledIds.has(c.id),
      }));
      await saveCategoryPreferences(book, items);
      await onReload();
      window.alert("저장되었습니다.");
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setBusy(false);
    }
  }

  function toggleExpandMajorKey(type: CategoryType, key: string) {
    const setExpanded = type === "INCOME" ? setExpandedIncomeMajorKeys : setExpandedExpenseMajorKeys;
    const setActive = type === "INCOME" ? setActiveIncomeMajorKey : setActiveExpenseMajorKey;
    setExpanded((prev) => {
      const next = new Set(prev);
      if (next.has(key)) next.delete(key);
      else next.add(key);
      return next;
    });
    setActive(key);
  }

  function selectMajorKey(type: CategoryType, key: string) {
    toggleExpandMajorKey(type, key);
  }

  function handleResetSelection() {
    setErr(null);
    setCheckedDeleteKeys(new Set());
    setFocusKey(null);
    setExpandedIncomeMajorKeys(new Set());
    setExpandedExpenseMajorKeys(new Set());
    setActiveIncomeMajorKey(null);
    setActiveExpenseMajorKey(null);
    const synced = syncEditTrees(categories);
    setIncomeEdit(synced.income);
    setExpenseEdit(synced.expense);
    setDisabledIds(prefsFromCategories(categories));
  }

  const treeDirty = hasTreeChanges(incomeEdit) || hasTreeChanges(expenseEdit);

  function renderHint() {
    if (mainTab === "add-delete") {
      return (
        <MonetaHint>
          ▶로 소분류를 펼치고 이름을 입력한 뒤 저장하세요. ⠿ 아이콘을 드래그하면 순서를 변경하거나 다른
          대분류로 이동할 수 있습니다. 내역에 사용 중인 분류는 삭제할 수 없습니다.
        </MonetaHint>
      );
    }
    if (mainTab === "my-settings") {
      return <MonetaHint>전체선택(사용하지 않는 분류를 제외할 수 있습니다.)</MonetaHint>;
    }
    return null;
  }

  function renderAddDeleteTree() {
    return (
      <div className="cb-catmgmt__dual cb-catmgmt__dual--treeGrid">
        <div className="cb-catmgmt__dualCol">
          <CategoryTreeColumn
            title="수입"
            tone="income"
            groups={incomeEdit}
            expandedMajorKeys={expandedIncomeMajorKeys}
            activeMajorKey={activeIncomeMajorKey}
            focusKey={focusKey}
            checkedKeys={checkedDeleteKeys}
            disabled={busy}
            onExpandMajor={(key) => selectMajorKey("INCOME", key)}
            onToggleMajor={(key) => toggleExpandMajorKey("INCOME", key)}
            onToggleCheck={toggleDeleteCheckKey}
            onNameChange={(key, name) => setIncomeEdit((prev) => updateNodeName(prev, key, name))}
            onAddMajor={() => addMajorToTree("INCOME")}
            onAddMinor={(majorKey) => addMinorToTree("INCOME", majorKey)}
            onDeleteSelected={() => deleteSelectedInTree("INCOME")}
            onMoveMajor={(dragKey, dropKey) => void moveMajorInTree("INCOME", dragKey, dropKey)}
            onRelocateMinor={(fromMajorKey, dragKey, toMajorKey, beforeKey) =>
              void relocateMinorInTree("INCOME", fromMajorKey, dragKey, toMajorKey, beforeKey)
            }
          />
        </div>
        <div className="cb-catmgmt__dualCol">
          <CategoryTreeColumn
            title="지출"
            tone="expense"
            groups={expenseEdit}
            expandedMajorKeys={expandedExpenseMajorKeys}
            activeMajorKey={activeExpenseMajorKey}
            focusKey={focusKey}
            checkedKeys={checkedDeleteKeys}
            disabled={busy}
            onExpandMajor={(key) => selectMajorKey("EXPENSE", key)}
            onToggleMajor={(key) => toggleExpandMajorKey("EXPENSE", key)}
            onToggleCheck={toggleDeleteCheckKey}
            onNameChange={(key, name) => setExpenseEdit((prev) => updateNodeName(prev, key, name))}
            onAddMajor={() => addMajorToTree("EXPENSE")}
            onAddMinor={(majorKey) => addMinorToTree("EXPENSE", majorKey)}
            onDeleteSelected={() => deleteSelectedInTree("EXPENSE")}
            onMoveMajor={(dragKey, dropKey) => void moveMajorInTree("EXPENSE", dragKey, dropKey)}
            onRelocateMinor={(fromMajorKey, dragKey, toMajorKey, beforeKey) =>
              void relocateMinorInTree("EXPENSE", fromMajorKey, dragKey, toMajorKey, beforeKey)
            }
          />
        </div>
      </div>
    );
  }

  function renderMySettings() {
    const allMinorsList = [...allMinors(incomeGroups), ...allMinors(expenseGroups)];
    const allEnabled = allMinorsList.every((m) => !disabledIds.has(m.id));

    function toggleAll(checked: boolean) {
      if (checked) {
        setDisabledIds(new Set());
      } else {
        setDisabledIds(new Set(allMinorsList.map((m) => m.id)));
      }
    }

    function renderColumn(type: CategoryType, tone: "income" | "expense", groups: CategoryGroup[]) {
      return (
        <div className={`cb-catmgmt__myCol cb-catmgmt__myCol--${tone}`}>
          <div className={`cb-catmgmt__listboxHead cb-catmgmt__listboxHead--${tone}`}>
            {type === "INCOME" ? "수입" : "지출"}
          </div>
          <div className="cb-catmgmt__myGrid">
            {groups.map((g) => (
              <div key={g.id} className="cb-catmgmt__myGroup">
                <div className="cb-catmgmt__myGroupTitle">{g.name}</div>
                {g.children.map((c) => (
                  <label key={c.id} className="cb-catmgmt__myItem">
                    <input
                      type="checkbox"
                      checked={!disabledIds.has(c.id)}
                      onChange={(e) => {
                        setDisabledIds((prev) => {
                          const next = new Set(prev);
                          if (e.target.checked) next.delete(c.id);
                          else next.add(c.id);
                          return next;
                        });
                      }}
                    />
                    <span>{c.name}</span>
                  </label>
                ))}
              </div>
            ))}
          </div>
        </div>
      );
    }

    return (
      <div className="cb-catmgmt__stack">
        <label className="cb-catmgmt__selectAll">
          <input type="checkbox" checked={allEnabled} onChange={(e) => toggleAll(e.target.checked)} />
          <span>전체선택(사용하지 않는 분류를 제외할 수 있습니다.)</span>
        </label>
        <div className="cb-catmgmt__myPanel">
          {renderColumn("INCOME", "income", incomeGroups)}
          {renderColumn("EXPENSE", "expense", expenseGroups)}
        </div>
      </div>
    );
  }

  function renderBody() {
    if (mainTab === "my-settings") return renderMySettings();
    return renderAddDeleteTree();
  }

  return (
    <div className="cb-settings__section">
      <SettingsSectionToolbar
        tabs={[
          { id: "add-delete", label: "분류관리" },
          { id: "my-settings", label: "분류설정" },
        ]}
        activeTab={mainTab}
        onTabChange={(id) => setMainTab(id as MainTab)}
        actions={
          <>
            <button
              type="button"
              className="cb-btn cb-btn--primary"
              disabled={busy || (mainTab === "add-delete" && !treeDirty)}
              onClick={() => void handleSave()}
            >
              저장
            </button>
            <button type="button" className="cb-btn cb-btn--secondary" onClick={handleResetSelection}>
              {mainTab === "add-delete" ? "변경 취소" : "선택 초기화"}
            </button>
          </>
        }
      />

      {renderHint()}
      {err && <p className="cb-err cb-settings__err">{err}</p>}

      <div className="cb-settings__content">{renderBody()}</div>
    </div>
  );
}
