export function confirmDelete(count: number): boolean {
  if (count <= 0) return false;
  return window.confirm(
    `선택한 ${count}개 항목을 삭제할까요?\n삭제하면 되돌릴 수 없습니다.`,
  );
}

export function confirmLeaveUnsaved(): boolean {
  return window.confirm(
    "저장하지 않은 입력 내용이 있습니다.\n이 페이지를 떠나면 입력한 내용이 사라집니다.\n계속하시겠습니까?",
  );
}
