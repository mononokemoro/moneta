import re
from pathlib import Path
from collections import Counter

def parse_rows(path, kind):
    text = Path(path).read_text(encoding="euc-kr", errors="replace")
    rows = re.findall(r"<tr>\s*(.*?)\s*</tr>", text, re.S | re.I)
    data = []
    for row in rows:
        if "tb_sum" in row:
            continue
        tds = re.findall(r"<td[^>]*>(.*?)</td>", row, re.S | re.I)
        if not tds:
            continue
        tds = [re.sub(r"<[^>]+>", "", td).strip() for td in tds]
        if not tds or tds[0] == "일자":
            continue
        if kind == "expense" and len(tds) >= 7:
            data.append(("EXPENSE", tds[0], tds[1], tds[2], tds[3], tds[4], tds[5], tds[6]))
        elif kind == "income" and len(tds) >= 5:
            data.append(("INCOME", tds[0], tds[1], tds[2], tds[3], tds[4]))
        elif kind in ("savings", "insurance") and len(tds) >= 4:
            data.append((kind.upper(), tds[0], tds[1], tds[2], tds[3]))
    return data

files = {
    "expense": Path(r"c:\Users\user\Downloads\miga\dylbs\지출\미가스마트_보고서_지출_상세내역_(20260101~20260630).xls"),
    "income": Path(r"c:\Users\user\Downloads\miga\dylbs\수입\미가스마트_보고서_수입_상세내역_분류별_(20260101~20260630).xls"),
    "savings": Path(r"c:\Users\user\Downloads\miga\dylbs\저축\미가스마트_보고서_저축_상세내역_(20260101~20260630).xls"),
    "insurance": Path(r"c:\Users\user\Downloads\miga\dylbs\보험\미가스마트_보고서_보험_상세내역_(20260101~20260630).xls"),
}

cats = Counter()
for k, p in files.items():
    rows = parse_rows(p, k)
    print(k, len(rows))
    for r in rows:
        if k in ("expense", "income"):
            cats[(k, r[2])] += 1
        elif k == "savings":
            cats[("savings", r[2])] += 1
        elif k == "insurance":
            cats[("insurance", r[2])] += 1

print("expense categories:", len([c for c in cats if c[0] == "expense"]))
print("income categories:", len([c for c in cats if c[0] == "income"]))
print("savings names:", len([c for c in cats if c[0] == "savings"]))
print("insurance names:", [c[1] for c in cats if c[0] == "insurance"])

out = Path(r"c:\vPlus\app\git\private\moneta\backend\src\main\resources\moneta-categories.json")
import json
payload = {
    "expense": sorted({c[1] for c in cats if c[0] == "expense"}),
    "income": sorted({c[1] for c in cats if c[0] == "income"}),
    "savings": sorted({c[1] for c in cats if c[0] == "savings"}),
    "insurance": sorted({c[1] for c in cats if c[0] == "insurance"}),
}
out.write_text(json.dumps(payload, ensure_ascii=False, indent=2), encoding="utf-8")
print("wrote", out)
