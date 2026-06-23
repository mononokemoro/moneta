import type { CategoryGroup, CategoryItem, CategoryType } from "../api/categories";

export type EditableMinor = {
  key: string;
  id: number | null;
  name: string;
  originalName: string;
  userCreated: boolean;
  inUse: boolean;
  isNew: boolean;
  markedDelete: boolean;
};

export type EditableGroup = {
  key: string;
  id: number | null;
  name: string;
  originalName: string;
  userCreated: boolean;
  inUse: boolean;
  isNew: boolean;
  markedDelete: boolean;
  children: EditableMinor[];
};

let draftSeq = 0;

function nextDraftKey(prefix: string): string {
  draftSeq += 1;
  return `${prefix}-${draftSeq}`;
}

export function resetDraftSeq() {
  draftSeq = 0;
}

function minorFromItem(item: CategoryItem): EditableMinor {
  return {
    key: `id:${item.id}`,
    id: item.id,
    name: item.name,
    originalName: item.name,
    userCreated: item.userCreated,
    inUse: item.inUse,
    isNew: false,
    markedDelete: false,
  };
}

function groupFromApi(group: CategoryGroup): EditableGroup {
  return {
    key: `id:${group.id}`,
    id: group.id,
    name: group.name,
    originalName: group.name,
    userCreated: group.userCreated,
    inUse: group.inUse,
    isNew: false,
    markedDelete: false,
    children: group.children.map(minorFromItem),
  };
}

export function editableFromGroups(groups: CategoryGroup[]): EditableGroup[] {
  return groups.map(groupFromApi);
}

export function addMajorDraft(groups: EditableGroup[]): { groups: EditableGroup[]; key: string } {
  const key = nextDraftKey("major");
  const row: EditableGroup = {
    key,
    id: null,
    name: "",
    originalName: "",
    userCreated: true,
    inUse: false,
    isNew: true,
    markedDelete: false,
    children: [],
  };
  return { groups: [...groups, row], key };
}

export function addMinorDraft(
  groups: EditableGroup[],
  majorKey: string
): { groups: EditableGroup[]; key: string } {
  const key = nextDraftKey("minor");
  const minor: EditableMinor = {
    key,
    id: null,
    name: "",
    originalName: "",
    userCreated: true,
    inUse: false,
    isNew: true,
    markedDelete: false,
  };
  return {
    groups: groups.map((g) =>
      g.key === majorKey ? { ...g, children: [...g.children, minor] } : g
    ),
    key,
  };
}

export function updateNodeName(
  groups: EditableGroup[],
  nodeKey: string,
  name: string
): EditableGroup[] {
  return groups.map((g) => {
    if (g.key === nodeKey) return { ...g, name };
    return {
      ...g,
      children: g.children.map((c) => (c.key === nodeKey ? { ...c, name } : c)),
    };
  });
}

export function markSelectedForDelete(
  groups: EditableGroup[],
  selectedKeys: Set<string>
): EditableGroup[] {
  return groups
    .map((g) => {
      const children = g.children
        .map((c) => {
          if (!selectedKeys.has(c.key)) return c;
          if (c.isNew) return null;
          return { ...c, markedDelete: true };
        })
        .filter((c): c is EditableMinor => c !== null);

      if (!selectedKeys.has(g.key)) {
        return { ...g, children };
      }
      if (g.isNew) return null;
      return { ...g, markedDelete: true, children };
    })
    .filter((g): g is EditableGroup => g !== null);
}

export function visibleGroups(groups: EditableGroup[]): EditableGroup[] {
  return groups
    .filter((g) => !g.markedDelete)
    .map((g) => ({
      ...g,
      children: g.children.filter((c) => !c.markedDelete),
    }));
}

export function moveMajor(
  groups: EditableGroup[],
  dragKey: string,
  dropKey: string
): EditableGroup[] | null {
  if (dragKey === dropKey) return null;
  const active = groups.filter((g) => !g.markedDelete);
  const fromIdx = active.findIndex((g) => g.key === dragKey);
  const toIdx = active.findIndex((g) => g.key === dropKey);
  if (fromIdx < 0 || toIdx < 0) return null;
  const nextActive = [...active];
  const [item] = nextActive.splice(fromIdx, 1);
  nextActive.splice(toIdx, 0, item);
  const deleted = groups.filter((g) => g.markedDelete);
  return [...nextActive, ...deleted];
}

export function moveMinor(
  groups: EditableGroup[],
  majorKey: string,
  dragKey: string,
  dropKey: string
): EditableGroup[] | null {
  if (dragKey === dropKey) return null;
  let changed = false;
  const next = groups.map((g) => {
    if (g.key !== majorKey) return g;
    const active = g.children.filter((c) => !c.markedDelete);
    const fromIdx = active.findIndex((c) => c.key === dragKey);
    const toIdx = active.findIndex((c) => c.key === dropKey);
    if (fromIdx < 0 || toIdx < 0) return g;
    const nextActive = [...active];
    const [item] = nextActive.splice(fromIdx, 1);
    nextActive.splice(toIdx, 0, item);
    changed = true;
    const deleted = g.children.filter((c) => c.markedDelete);
    return { ...g, children: [...nextActive, ...deleted] };
  });
  return changed ? next : null;
}

/** 소분류를 다른 대분류로 옮기거나, 같은 대분류 안에서 위치를 바꿉니다. */
export function relocateMinor(
  groups: EditableGroup[],
  dragKey: string,
  fromMajorKey: string,
  toMajorKey: string,
  beforeKey: string | null
): EditableGroup[] | null {
  const fromGroup = groups.find((g) => g.key === fromMajorKey);
  if (!fromGroup) return null;
  const dragged = fromGroup.children.find((c) => c.key === dragKey && !c.markedDelete);
  if (!dragged) return null;

  if (fromMajorKey === toMajorKey) {
    if (beforeKey) return moveMinor(groups, fromMajorKey, dragKey, beforeKey);
    const active = fromGroup.children.filter((c) => !c.markedDelete);
    const fromIdx = active.findIndex((c) => c.key === dragKey);
    if (fromIdx < 0 || fromIdx === active.length - 1) return null;
    const nextActive = [...active];
    const [item] = nextActive.splice(fromIdx, 1);
    nextActive.push(item);
    const deleted = fromGroup.children.filter((c) => c.markedDelete);
    return groups.map((g) =>
      g.key === fromMajorKey ? { ...g, children: [...nextActive, ...deleted] } : g
    );
  }

  const toGroup = groups.find((g) => g.key === toMajorKey);
  if (!toGroup) return null;

  const without = groups.map((g) =>
    g.key === fromMajorKey
      ? { ...g, children: g.children.filter((c) => c.key !== dragKey) }
      : g
  );

  return without.map((g) => {
    if (g.key !== toMajorKey) return g;
    const active = g.children.filter((c) => !c.markedDelete);
    const deleted = g.children.filter((c) => c.markedDelete);
    const insertIdx = beforeKey ? active.findIndex((c) => c.key === beforeKey) : active.length;
    if (beforeKey && insertIdx < 0) return g;
    const nextActive = [...active];
    nextActive.splice(insertIdx < 0 ? nextActive.length : insertIdx, 0, dragged);
    return { ...g, children: [...nextActive, ...deleted] };
  });
}

export function minorParentChanged(
  before: EditableGroup[],
  after: EditableGroup[],
  dragKey: string,
  fromMajorKey: string,
  toMajorKey: string
): boolean {
  if (fromMajorKey === toMajorKey) return false;
  const beforeParent = before.find((g) => g.children.some((c) => c.key === dragKey))?.key;
  const afterParent = after.find((g) => g.children.some((c) => c.key === dragKey))?.key;
  return beforeParent === fromMajorKey && afterParent === toMajorKey;
}

export function majorReorderItems(
  groups: EditableGroup[]
): { id: number; sortOrder: number; parentId: null }[] {
  return groups
    .filter((g) => !g.markedDelete && g.id != null)
    .map((g, i) => ({ id: g.id!, sortOrder: i, parentId: null }));
}

export function minorReorderItems(
  group: EditableGroup
): { id: number; sortOrder: number; parentId: number }[] {
  if (group.id == null) return [];
  return group.children
    .filter((c) => !c.markedDelete && c.id != null)
    .map((c, i) => ({ id: c.id!, sortOrder: i, parentId: group.id! }));
}

export function majorOrderChanged(before: EditableGroup[], after: EditableGroup[]): boolean {
  const ids = (rows: EditableGroup[]) =>
    rows.filter((g) => !g.markedDelete && g.id != null).map((g) => g.id);
  const a = ids(before);
  const b = ids(after);
  return a.length !== b.length || a.some((id, i) => id !== b[i]);
}

export function minorOrderChanged(groupBefore: EditableGroup, groupAfter: EditableGroup): boolean {
  const ids = (g: EditableGroup) =>
    g.children.filter((c) => !c.markedDelete && c.id != null).map((c) => c.id);
  const a = ids(groupBefore);
  const b = ids(groupAfter);
  return a.length !== b.length || a.some((id, i) => id !== b[i]);
}

export function collectKeys(groups: EditableGroup[]): Set<string> {
  const keys = new Set<string>();
  for (const g of groups) {
    keys.add(g.key);
    for (const c of g.children) keys.add(c.key);
  }
  return keys;
}

export function isDeletableNode(groups: EditableGroup[], key: string): boolean {
  for (const g of groups) {
    if (g.key === key) {
      if (g.isNew) return true;
      return g.userCreated && !g.inUse;
    }
    for (const c of g.children) {
      if (c.key === key) {
        if (c.isNew) return true;
        return c.userCreated && !c.inUse;
      }
    }
  }
  return false;
}

export function isEditableName(groups: EditableGroup[], key: string): boolean {
  for (const g of groups) {
    if (g.key === key) return g.isNew || g.id != null;
    for (const c of g.children) {
      if (c.key === key) return c.isNew || c.id != null;
    }
  }
  return false;
}

export type TreeSavePlan = {
  createsMajor: { key: string; name: string }[];
  createsMinor: { key: string; majorKey: string; name: string }[];
  updates: { id: number; name: string }[];
  deletes: number[];
};

export function buildSavePlan(groups: EditableGroup[]): TreeSavePlan {
  const createsMajor: TreeSavePlan["createsMajor"] = [];
  const createsMinor: TreeSavePlan["createsMinor"] = [];
  const updates: TreeSavePlan["updates"] = [];
  const deletes: number[] = [];

  for (const g of groups) {
    if (g.markedDelete) {
      if (g.id != null) deletes.push(g.id);
      continue;
    }
    const majorName = g.name.trim();
    if (g.isNew) {
      createsMajor.push({ key: g.key, name: majorName });
    } else if (g.id != null && g.name.trim() !== g.originalName) {
      updates.push({ id: g.id, name: majorName });
    }

    for (const c of g.children) {
      if (c.markedDelete) {
        if (c.id != null) deletes.push(c.id);
        continue;
      }
      const minorName = c.name.trim();
      if (c.isNew) {
        createsMinor.push({ key: c.key, majorKey: g.key, name: minorName });
      } else if (c.id != null && c.name.trim() !== c.originalName) {
        updates.push({ id: c.id, name: minorName });
      }
    }
  }

  return { createsMajor, createsMinor, updates, deletes };
}

export function validateSavePlan(plan: TreeSavePlan): string | null {
  for (const row of plan.createsMajor) {
    if (!row.name) return "새 대분류 이름을 입력하세요.";
  }
  for (const row of plan.createsMinor) {
    if (!row.name) return "새 소분류 이름을 입력하세요.";
  }
  for (const row of plan.updates) {
    if (!row.name) return "분류 이름을 비울 수 없습니다.";
  }
  return null;
}

export function hasTreeChanges(groups: EditableGroup[]): boolean {
  const plan = buildSavePlan(groups);
  return (
    plan.createsMajor.length > 0 ||
    plan.createsMinor.length > 0 ||
    plan.updates.length > 0 ||
    plan.deletes.length > 0
  );
}

export async function applyTreeSavePlan(
  book: import("../api/ledgerBook").LedgerBook,
  type: CategoryType,
  groups: EditableGroup[],
  plan: TreeSavePlan,
  api: {
    createCategory: typeof import("../api/categories").createCategory;
    updateCategory: typeof import("../api/categories").updateCategory;
    deleteCategory: typeof import("../api/categories").deleteCategory;
  }
): Promise<void> {
  const majorIdByKey = new Map<string, number>();

  for (const g of groups) {
    if (!g.isNew || g.markedDelete) continue;
    if (g.id != null) majorIdByKey.set(g.key, g.id);
  }

  for (const row of plan.createsMajor) {
    const created = await api.createCategory(book, type, "MAJOR", row.name);
    majorIdByKey.set(row.key, created.id);
  }

  for (const row of plan.createsMinor) {
    const majorId = majorIdByKey.get(row.majorKey);
    if (majorId == null) {
      throw new Error("소분류의 대분류를 찾을 수 없습니다.");
    }
    await api.createCategory(book, type, "MINOR", row.name, majorId);
  }

  for (const row of plan.updates) {
    await api.updateCategory(row.id, book, row.name);
  }

  for (const id of plan.deletes) {
    await api.deleteCategory(id, book);
  }
}
