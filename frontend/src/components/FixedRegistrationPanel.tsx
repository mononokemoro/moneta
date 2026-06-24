import { useCallback, useEffect, useMemo, useState } from "react";
import {
  createFixedItem,
  deleteFixedItems,
  fetchFixedItems,
  updateFixedItem,
  type FixedHolidayAdjust,
  type FixedItemRow,
  type FixedItemSaveBody,
  type FixedKind,
  type FixedPeriodType,
  type FixedScheduleType,
} from "../api/fixedItems";
import type { CategoryList } from "../api/categories";
import { fetchFinancialProducts, type ProductList } from "../api/financialProducts";
import type { LedgerBook } from "../api/ledgerBook";
import { formatMoney } from "../formatMoney";
import { amountToInput, formatAmountInput, parseAmount } from "../util/parseAmount";
import { confirmDelete } from "../util/confirmDialog";
import { CategoryPicker } from "./CategoryPicker";
import { CompactDateInput } from "./CompactDateInput";
import { ComboInput } from "./ComboInput";
import { MonetaHint, MonetaSubNav } from "./MonetaPanel";
import { SettingsSectionToolbar } from "./SettingsSectionToolbar";

type Props = {
  book: LedgerBook;
  categories: CategoryList;
};

type MainTab = FixedKind;
type ScheduleTab = FixedScheduleType;

type FormState = {
  dayKey: string;
  holidayAdjust: FixedHolidayAdjust;
  category: string;
  subCategory: string;
  title: string;
  defaultAmount: string;
  interestAmount: string;
  paymentMethod: string;
  remarks: string;
  periodType: FixedPeriodType;
  periodStart: string;
  periodEnd: string;
};

const MAIN_TABS: { id: MainTab; label: string }[] = [
  { id: "INCOME", label: "수입" },
  { id: "SAVINGS", label: "저축/보험" },
  { id: "LOAN", label: "대출" },
  { id: "EXPENSE", label: "지출" },
];

const SCHEDULE_TABS: { id: ScheduleTab; label: string }[] = [
  { id: "MONTHLY", label: "매월 고정" },
  { id: "DAILY", label: "매일 고정" },
];

const HOLIDAY_OPTIONS: { value: FixedHolidayAdjust; label: string }[] = [
  { value: "NONE", label: "선택안함" },
  { value: "PREVIOUS", label: "휴일체크" },
];

const PERIOD_OPTIONS: { value: FixedPeriodType; label: string }[] = [
  { value: "CONTINUOUS", label: "계속" },
  { value: "RANGE", label: "기간지정" },
];

const PAYMENT_OPTIONS = ["현금", "신용카드", "체크카드", "기타카드"];
const SAVINGS_CLASSES = ["저축", "보험"];

function emptyForm(): FormState {
  return {
    dayKey: "1",
    holidayAdjust: "NONE",
    category: "",
    subCategory: "저축",
    title: "",
    defaultAmount: "",
    interestAmount: "",
    paymentMethod: "현금",
    remarks: "",
    periodType: "CONTINUOUS",
    periodStart: "",
    periodEnd: "",
  };
}

function dayOptions() {
  const opts = Array.from({ length: 31 }, (_, i) => ({
    value: String(i + 1),
    label: String(i + 1),
  }));
  opts.push({ value: "LAST", label: "말일" });
  return opts;
}

function periodLabel(row: FixedItemRow): string {
  if (row.periodType === "RANGE") {
    const start = row.periodStart ?? "";
    const end = row.periodEnd ?? "";
    if (start || end) return `${start || "…"}~${end || "…"}`;
  }
  return "계속";
}

function buildSaveBody(kind: MainTab, scheduleType: ScheduleTab, form: FormState): FixedItemSaveBody {
  const lastDayOfMonth = scheduleType === "MONTHLY" && form.dayKey === "LAST";
  const dayOfMonth =
    scheduleType === "MONTHLY" && !lastDayOfMonth ? Number(form.dayKey) || 1 : null;

  let category = form.category.trim();
  if (kind === "SAVINGS") {
    category = form.subCategory.trim() || "저축";
  } else if (kind === "LOAN") {
    category = "대출";
  }

  let title = form.title.trim();
  if (!title && kind === "EXPENSE") title = form.category.trim();

  return {
    kind,
    scheduleType,
    dayOfMonth,
    lastDayOfMonth,
    holidayAdjust: form.holidayAdjust,
    periodType: form.periodType,
    periodStart: form.periodType === "RANGE" && form.periodStart ? form.periodStart : null,
    periodEnd: form.periodType === "RANGE" && form.periodEnd ? form.periodEnd : null,
    title,
    defaultAmount: parseAmount(form.defaultAmount),
    interestAmount: kind === "LOAN" ? parseAmount(form.interestAmount) : 0,
    category,
    paymentMethod: kind === "EXPENSE" ? form.paymentMethod : "현금",
    remarks: form.remarks.trim(),
  };
}

function rowToForm(row: FixedItemRow): FormState {
  return {
    dayKey: row.lastDayOfMonth ? "LAST" : String(row.dayOfMonth ?? 1),
    holidayAdjust: row.holidayAdjust,
    category: row.category,
    subCategory: row.category === "보험" ? "보험" : "저축",
    title: row.title,
    defaultAmount: amountToInput(row.defaultAmount),
    interestAmount: amountToInput(row.interestAmount),
    paymentMethod: row.paymentMethod || "현금",
    remarks: row.remarks,
    periodType: row.periodType,
    periodStart: row.periodStart ?? "",
    periodEnd: row.periodEnd ?? "",
  };
}

export function FixedRegistrationPanel({ book, categories }: Props) {
  const [mainTab, setMainTab] = useState<MainTab>("INCOME");
  const [scheduleTab, setScheduleTab] = useState<ScheduleTab>("MONTHLY");
  const [rows, setRows] = useState<FixedItemRow[]>([]);
  const [products, setProducts] = useState<ProductList | null>(null);
  const [form, setForm] = useState<FormState>(() => emptyForm());
  const [editId, setEditId] = useState<number | null>(null);
  const [selected, setSelected] = useState<Set<number>>(() => new Set());
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [err, setErr] = useState<string | null>(null);

  const incomeGroups = categories.income;
  const expenseGroups = categories.expense;

  const nameOptions = useMemo(() => {
    if (!products) return [];
    if (mainTab === "SAVINGS") {
      return [
        ...products.savings.map((p) => p.name),
        ...products.insurance.map((p) => p.name),
      ].filter(Boolean);
    }
    if (mainTab === "LOAN") return products.loans.map((p) => p.name).filter(Boolean);
    return [];
  }, [mainTab, products]);

  const reload = useCallback(async () => {
    const [items, prod] = await Promise.all([
      fetchFixedItems(book, mainTab, scheduleTab),
      fetchFinancialProducts(book),
    ]);
    setRows(items);
    setProducts(prod);
    setErr(null);
  }, [book, mainTab, scheduleTab]);

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
  }, [reload]);

  useEffect(() => {
    setForm(emptyForm());
    setEditId(null);
    setSelected(new Set());
  }, [mainTab, scheduleTab, book]);

  function patchForm(p: Partial<FormState>) {
    setForm((prev) => ({ ...prev, ...p }));
  }

  function resetForm() {
    setForm(emptyForm());
    setEditId(null);
  }

  async function handleRegister() {
    setBusy(true);
    setErr(null);
    try {
      const body = buildSaveBody(mainTab, scheduleTab, form);
      if (!body.title) {
        setErr("항목명을 입력하세요.");
        return;
      }
      if (editId) {
        await updateFixedItem(editId, book, body);
      } else {
        await createFixedItem(book, body);
      }
      resetForm();
      await reload();
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "저장 실패");
    } finally {
      setBusy(false);
    }
  }

  async function handleDeleteSelected() {
    if (selected.size === 0) return;
    if (!confirmDelete(selected.size)) return;
    setBusy(true);
    setErr(null);
    try {
      await deleteFixedItems(book, [...selected]);
      setSelected(new Set());
      if (editId && selected.has(editId)) resetForm();
      await reload();
    } catch (e: unknown) {
      setErr(e instanceof Error ? e.message : "삭제 실패");
    } finally {
      setBusy(false);
    }
  }

  function toggleSelected(id: number, checked: boolean) {
    setSelected((prev) => {
      const next = new Set(prev);
      if (checked) next.add(id);
      else next.delete(id);
      return next;
    });
  }

  function startEdit(row: FixedItemRow) {
    setEditId(row.id);
    setForm(rowToForm(row));
  }

  const daySelect =
    scheduleTab === "MONTHLY" ? (
      <select
        className="cb-fixedreg__cell"
        value={form.dayKey}
        disabled={busy}
        onChange={(e) => patchForm({ dayKey: e.target.value })}
      >
        {dayOptions().map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
    ) : (
      <span className="cb-fixedreg__static">매일</span>
    );

  const dayCell = (
    <div className="cb-fixedreg__dayCell">
      {daySelect}
      {scheduleTab === "MONTHLY" && (
        <select
          className="cb-fixedreg__cell cb-fixedreg__cell--holiday"
          value={form.holidayAdjust}
          disabled={busy}
          onChange={(e) => patchForm({ holidayAdjust: e.target.value as FixedHolidayAdjust })}
        >
          {HOLIDAY_OPTIONS.map((o) => (
            <option key={o.value} value={o.value}>
              {o.label}
            </option>
          ))}
        </select>
      )}
    </div>
  );

  const periodCell = (
    <div className="cb-fixedreg__periodCell">
      <select
        className="cb-fixedreg__cell"
        value={form.periodType}
        disabled={busy}
        onChange={(e) => patchForm({ periodType: e.target.value as FixedPeriodType })}
      >
        {PERIOD_OPTIONS.map((o) => (
          <option key={o.value} value={o.value}>
            {o.label}
          </option>
        ))}
      </select>
      {form.periodType === "RANGE" && (
        <div className="cb-fixedreg__periodRange">
          <CompactDateInput
            className="cb-fixedreg__cell"
            value={form.periodStart}
            disabled={busy}
            onChange={(iso) => patchForm({ periodStart: iso })}
          />
          <span className="cb-fixedreg__periodSep">~</span>
          <CompactDateInput
            className="cb-fixedreg__cell"
            value={form.periodEnd}
            disabled={busy}
            onChange={(iso) => patchForm({ periodEnd: iso })}
          />
        </div>
      )}
    </div>
  );

  function renderFormTable() {
    if (mainTab === "INCOME") {
      return (
        <table className="cb-fixedreg__table">
          <colgroup>
            <col className="cb-fixedreg__col-day" />
            <col className="cb-fixedreg__col-category" />
            <col className="cb-fixedreg__col-title" />
            <col className="cb-fixedreg__col-amount" />
            <col className="cb-fixedreg__col-period" />
          </colgroup>
          <thead>
            <tr>
              <th>일자 및 휴일체크 *</th>
              <th>분류 +</th>
              <th>수입내역 *</th>
              <th className="cb-num">금액</th>
              <th>고정기간</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>{dayCell}</td>
              <td>
                <CategoryPicker
                  className="cb-fixedreg__cell"
                  groups={incomeGroups}
                  value={form.category}
                  onChange={(v) => patchForm({ category: v })}
                  disabled={busy}
                />
              </td>
              <td>
                <input
                  className="cb-fixedreg__cell"
                  value={form.title}
                  disabled={busy}
                  onChange={(e) => patchForm({ title: e.target.value })}
                />
              </td>
              <td>
                <input
                  className="cb-fixedreg__cell cb-num"
                  value={form.defaultAmount}
                  disabled={busy}
                  onChange={(e) => patchForm({ defaultAmount: formatAmountInput(e.target.value) })}
                />
              </td>
              <td>{periodCell}</td>
            </tr>
          </tbody>
        </table>
      );
    }

    if (mainTab === "SAVINGS") {
      return (
        <table className="cb-fixedreg__table">
          <colgroup>
            <col className="cb-fixedreg__col-day" />
            <col className="cb-fixedreg__col-kind" />
            <col className="cb-fixedreg__col-title" />
            <col className="cb-fixedreg__col-amount" />
            <col className="cb-fixedreg__col-remarks" />
            <col className="cb-fixedreg__col-period" />
          </colgroup>
          <thead>
            <tr>
              <th>일자 및 휴일체크 *</th>
              <th>구분</th>
              <th>저축/보험명 *</th>
              <th className="cb-num">금액</th>
              <th>비고</th>
              <th>고정기간</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>{dayCell}</td>
              <td>
                <select
                  className="cb-fixedreg__cell"
                  value={form.subCategory}
                  disabled={busy}
                  onChange={(e) => patchForm({ subCategory: e.target.value })}
                >
                  {SAVINGS_CLASSES.map((c) => (
                    <option key={c} value={c}>
                      {c}
                    </option>
                  ))}
                </select>
              </td>
              <td>
                <ComboInput
                  className="cb-fixedreg__cell"
                  options={nameOptions}
                  value={form.title}
                  onChange={(v) => patchForm({ title: v })}
                  disabled={busy}
                />
              </td>
              <td>
                <input
                  className="cb-fixedreg__cell cb-num"
                  value={form.defaultAmount}
                  disabled={busy}
                  onChange={(e) => patchForm({ defaultAmount: formatAmountInput(e.target.value) })}
                />
              </td>
              <td>
                <input
                  className="cb-fixedreg__cell"
                  value={form.remarks}
                  disabled={busy}
                  onChange={(e) => patchForm({ remarks: e.target.value })}
                />
              </td>
              <td>{periodCell}</td>
            </tr>
          </tbody>
        </table>
      );
    }

    if (mainTab === "LOAN") {
      return (
        <table className="cb-fixedreg__table">
          <colgroup>
            <col className="cb-fixedreg__col-day" />
            <col className="cb-fixedreg__col-title" />
            <col className="cb-fixedreg__col-amount" />
            <col className="cb-fixedreg__col-amount" />
            <col className="cb-fixedreg__col-remarks" />
            <col className="cb-fixedreg__col-period" />
          </colgroup>
          <thead>
            <tr>
              <th>일자 및 휴일체크 *</th>
              <th>대출명 *</th>
              <th className="cb-num">상환원금</th>
              <th className="cb-num">상환이자</th>
              <th>비고</th>
              <th>고정기간</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>{dayCell}</td>
              <td>
                <ComboInput
                  className="cb-fixedreg__cell"
                  options={nameOptions}
                  value={form.title}
                  onChange={(v) => patchForm({ title: v })}
                  disabled={busy}
                />
              </td>
              <td>
                <input
                  className="cb-fixedreg__cell cb-num"
                  value={form.defaultAmount}
                  disabled={busy}
                  onChange={(e) => patchForm({ defaultAmount: formatAmountInput(e.target.value) })}
                />
              </td>
              <td>
                <input
                  className="cb-fixedreg__cell cb-num"
                  value={form.interestAmount}
                  disabled={busy}
                  onChange={(e) => patchForm({ interestAmount: formatAmountInput(e.target.value) })}
                />
              </td>
              <td>
                <input
                  className="cb-fixedreg__cell"
                  value={form.remarks}
                  disabled={busy}
                  onChange={(e) => patchForm({ remarks: e.target.value })}
                />
              </td>
              <td>{periodCell}</td>
            </tr>
          </tbody>
        </table>
      );
    }

    return (
      <table className="cb-fixedreg__table">
        <colgroup>
          <col className="cb-fixedreg__col-day" />
          <col className="cb-fixedreg__col-category" />
          <col className="cb-fixedreg__col-title" />
          <col className="cb-fixedreg__col-amount" />
          <col className="cb-fixedreg__col-pay" />
          <col className="cb-fixedreg__col-period" />
        </colgroup>
        <thead>
          <tr>
            <th>일자 및 휴일체크 *</th>
            <th>분류</th>
            <th>지출내역 *</th>
            <th className="cb-num">금액</th>
            <th>현금/카드</th>
            <th>고정기간</th>
          </tr>
        </thead>
        <tbody>
          <tr>
            <td>{dayCell}</td>
            <td>
              <CategoryPicker
                className="cb-fixedreg__cell"
                groups={expenseGroups}
                value={form.category}
                onChange={(v) => patchForm({ category: v })}
                disabled={busy}
              />
            </td>
            <td>
              <input
                className="cb-fixedreg__cell"
                value={form.title}
                disabled={busy}
                onChange={(e) => patchForm({ title: e.target.value })}
              />
            </td>
            <td>
              <input
                className="cb-fixedreg__cell cb-num"
                value={form.defaultAmount}
                disabled={busy}
                onChange={(e) => patchForm({ defaultAmount: formatAmountInput(e.target.value) })}
              />
            </td>
            <td>
              <select
                className="cb-fixedreg__cell"
                value={form.paymentMethod}
                disabled={busy}
                onChange={(e) => patchForm({ paymentMethod: e.target.value })}
              >
                {PAYMENT_OPTIONS.map((o) => (
                  <option key={o} value={o}>
                    {o}
                  </option>
                ))}
              </select>
            </td>
            <td>{periodCell}</td>
          </tr>
        </tbody>
      </table>
    );
  }

  return (
    <div className="cb-settings__section cb-fixedreg">
      <SettingsSectionToolbar
        tabs={MAIN_TABS}
        activeTab={mainTab}
        onTabChange={(id) => setMainTab(id as MainTab)}
      />

      <MonetaSubNav
        items={SCHEDULE_TABS}
        active={scheduleTab}
        onChange={(id) => setScheduleTab(id as ScheduleTab)}
      />

      <div className="cb-fixedreg__hintBar">
        <MonetaHint>매월/매일 고정으로 발생하는 내역을 등록하여 관리하세요.</MonetaHint>
        <div className="cb-fixedreg__hintActions">
          <button type="button" className="cb-btn cb-btn--primary" disabled={busy} onClick={() => void handleRegister()}>
            {editId ? "수정" : "등록"}
          </button>
          <button type="button" className="cb-btn cb-btn--secondary" disabled={busy} onClick={resetForm}>
            취소
          </button>
        </div>
      </div>
      {err && <p className="cb-err cb-settings__err">{err}</p>}

      <div className="cb-fixedreg__form">
        <div className="cb-fixedreg__tableWrap cb-fixedreg__tableWrap--form">{renderFormTable()}</div>
      </div>

      <div className="cb-fixedreg__tableWrap">
        {loading ? (
          <p className="cb-muted">불러오는 중…</p>
        ) : (
          <table className="cb-fixedreg__table">
            <thead>
              <tr>
                <th className="cb-fixedreg__check" />
                <th>일자</th>
                {mainTab !== "LOAN" && <th>분류</th>}
                <th>
                  {mainTab === "INCOME"
                    ? "수입내역"
                    : mainTab === "SAVINGS"
                      ? "저축/보험명"
                      : mainTab === "LOAN"
                        ? "대출명"
                        : "지출내역"}
                </th>
                {mainTab === "LOAN" ? (
                  <>
                    <th className="cb-num">상환원금</th>
                    <th className="cb-num">상환이자</th>
                  </>
                ) : (
                  <th className="cb-num">금액</th>
                )}
                {mainTab === "SAVINGS" || mainTab === "LOAN" ? <th>비고</th> : null}
                {mainTab === "EXPENSE" ? <th>현금/카드</th> : null}
                <th>고정기간</th>
              </tr>
            </thead>
            <tbody>
              {rows.length === 0 ? (
                <tr>
                  <td colSpan={10} className="cb-fixedreg__empty">
                    등록된 고정 항목이 없습니다.
                  </td>
                </tr>
              ) : (
                rows.map((row) => (
                  <tr
                    key={row.id}
                    className={editId === row.id ? "is-editing" : ""}
                    onDoubleClick={() => startEdit(row)}
                  >
                    <td>
                      <input
                        type="checkbox"
                        checked={selected.has(row.id)}
                        onChange={(e) => toggleSelected(row.id, e.target.checked)}
                        disabled={busy}
                      />
                    </td>
                    <td>
                      {row.scheduleType === "DAILY" ? "매일" : row.scheduleLabel}
                      {row.holidayCheck ? <span className="cb-fixedreg__badge">휴일체크</span> : null}
                    </td>
                    {mainTab !== "LOAN" && <td>{row.category}</td>}
                    <td>{row.title}</td>
                    {mainTab === "LOAN" ? (
                      <>
                        <td className="cb-num">{formatMoney(row.defaultAmount)}</td>
                        <td className="cb-num">{formatMoney(row.interestAmount)}</td>
                      </>
                    ) : (
                      <td className="cb-num">{formatMoney(row.defaultAmount)}</td>
                    )}
                    {mainTab === "SAVINGS" || mainTab === "LOAN" ? <td>{row.remarks}</td> : null}
                    {mainTab === "EXPENSE" ? <td>{row.paymentMethod || "현금"}</td> : null}
                    <td>{periodLabel(row)}</td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        )}
      </div>

      <div className="cb-fixedreg__footer">
        <button
          type="button"
          className="cb-btn cb-btn--ghost"
          disabled={busy || selected.size === 0}
          onClick={() => void handleDeleteSelected()}
        >
          삭제
        </button>
      </div>
    </div>
  );
}
