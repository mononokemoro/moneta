/** 메인 화면 지출·수입·저축 테이블 기본 표시 행 수 */
export const DEFAULT_TABLE_ROWS = 6;

/** 스크롤 없이 한 번에 보이는 행 수 (나머지는 세로 스크롤) */
export const VISIBLE_TABLE_ROWS = 6;

/** 저장된 행 + 빈 입력 행을 최소 minRows 만큼 표시 */
export function buildTableDrafts<T extends { id?: number }>(
  saved: T[],
  createEmpty: () => T,
  minRows = DEFAULT_TABLE_ROWS
): T[] {
  const drafts = [...saved];
  while (drafts.length < minRows) {
    drafts.push(createEmpty());
  }
  if (drafts[drafts.length - 1]?.id != null) {
    drafts.push(createEmpty());
  }
  return drafts;
}
