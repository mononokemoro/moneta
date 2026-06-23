const CHOSEONG = "ㄱㄲㄴㄷㄸㄹㅁㅂㅃㅅㅆㅇㅈㅉㅊㅋㅌㅍㅎ";

/** 한글·영문·숫자 문자열에서 초성(및 영숫자) 시퀀스를 추출합니다. */
export function extractChoseong(text: string): string {
  let result = "";
  for (const char of text) {
    const code = char.charCodeAt(0);
    if (code >= 0xac00 && code <= 0xd7a3) {
      result += CHOSEONG[Math.floor((code - 0xac00) / 588)];
    } else if (code >= 0x3131 && code <= 0x314e) {
      result += char;
    } else if ((code >= 65 && code <= 90) || (code >= 97 && code <= 122) || (code >= 48 && code <= 57)) {
      result += char.toLowerCase();
    }
  }
  return result;
}

/** 일반 부분 일치 또는 초성(ㄱ-ㅎ) 검색 */
export function matchesKoreanSearch(text: string, query: string): boolean {
  const q = query.trim();
  if (!q) return true;
  if (text.toLowerCase().includes(q.toLowerCase())) return true;

  const choseongQuery = q.replace(/\s/g, "");
  if (!/[ㄱ-ㅎ]/.test(choseongQuery)) return false;

  return extractChoseong(text).includes(choseongQuery);
}
