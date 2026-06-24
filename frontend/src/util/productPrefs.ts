import type { FinancialProduct, ProductList } from "../api/financialProducts";
import {
  cardStatusFromApi,
  cardStatusToApi,
  fetchFinancialProducts,
  loanStatusFromApi,
  loanStatusToApi,
  saveFinancialProducts,
  savingsStatusFromApi,
  savingsStatusToApi,
  syncCardsFromTransactions as apiSyncCardsFromTransactions,
} from "../api/financialProducts";
import type { LedgerBook } from "../api/ledgerBook";

export type SavingsStatus = "active" | "matured" | "terminated";
export type InsuranceStatus = "active" | "matured" | "terminated";
export type LoanStatus = "active" | "matured" | "prepaid";
export type CardStatus = "active" | "cancelled";

export interface SavingsProduct {
  id: string;
  classification: string;
  name: string;
  joinDate: string;
  maturityDate: string;
  autoTransferDay: string;
  openingBalance: string;
  openingDate: string;
  status: SavingsStatus;
}

export interface InsuranceProduct {
  id: string;
  classification: string;
  name: string;
  paymentMethod: string;
  joinDate: string;
  maturityDate: string;
  transferDay: string;
  openingBalance: string;
  openingDate: string;
  status: InsuranceStatus;
}

export interface LoanProduct {
  id: string;
  name: string;
  principal: string;
  startDate: string;
  maturityDate: string;
  repaymentDay: string;
  status: LoanStatus;
}

export interface CardProduct {
  id: string;
  classification: string;
  name: string;
  paymentDay: string;
  usageStartDate: string;
  cancelDate: string;
  limit: string;
  status: CardStatus;
}

export interface ProductStore {
  savings: SavingsProduct[];
  insurance: InsuranceProduct[];
  loans: LoanProduct[];
  cards: CardProduct[];
}

function newId() {
  return `p-${Date.now()}-${Math.random().toString(36).slice(2, 7)}`;
}

function rowId(id: number | null) {
  return id != null ? String(id) : newId();
}

function apiRowId(id: string): number | null {
  const n = Number(id);
  return Number.isFinite(n) && id.trim() !== "" ? n : null;
}

function openingBalanceInput(value: number | undefined): string {
  if (value == null || value === 0) return "";
  return String(value);
}

function toSavings(row: FinancialProduct): SavingsProduct {
  return {
    id: rowId(row.id),
    classification: row.classification,
    name: row.name,
    joinDate: row.joinDate,
    maturityDate: row.maturityDate,
    autoTransferDay: row.autoTransferDay,
    openingBalance: openingBalanceInput(row.openingBalance),
    openingDate: row.openingDate ?? "",
    status: savingsStatusFromApi(row.status),
  };
}

function toInsurance(row: FinancialProduct): InsuranceProduct {
  return {
    id: rowId(row.id),
    classification: row.classification,
    name: row.name,
    paymentMethod: row.paymentMethod,
    joinDate: row.joinDate,
    maturityDate: row.maturityDate,
    transferDay: row.transferDay,
    openingBalance: openingBalanceInput(row.openingBalance),
    openingDate: row.openingDate ?? "",
    status: savingsStatusFromApi(row.status) as InsuranceStatus,
  };
}

function toLoan(row: FinancialProduct): LoanProduct {
  return {
    id: rowId(row.id),
    name: row.name,
    principal: row.principal,
    startDate: row.startDate,
    maturityDate: row.maturityDate,
    repaymentDay: row.repaymentDay,
    status: loanStatusFromApi(row.status),
  };
}

/** API·입력 공통 YYYYMMDD (빈 값 허용) */
export function normalizeYmdDate(value: string): string {
  return value.replace(/\D/g, "").slice(0, 8);
}

export function todayYmd(): string {
  const d = new Date();
  const y = d.getFullYear();
  const m = String(d.getMonth() + 1).padStart(2, "0");
  const day = String(d.getDate()).padStart(2, "0");
  return `${y}${m}${day}`;
}

function toCard(row: FinancialProduct): CardProduct {
  return {
    id: rowId(row.id),
    classification: row.classification,
    name: row.name,
    paymentDay: normalizeCardDay(row.paymentDay),
    usageStartDate: normalizeYmdDate(row.joinDate),
    cancelDate: normalizeYmdDate(row.maturityDate),
    limit: "",
    status: cardStatusFromApi(row.status),
  };
}

function fromSavings(row: SavingsProduct, sortOrder: number): FinancialProduct {
  return {
    id: apiRowId(row.id),
    productType: "SAVINGS",
    status: savingsStatusToApi(row.status),
    sortOrder,
    classification: row.classification,
    name: row.name,
    paymentMethod: "",
    joinDate: row.joinDate,
    maturityDate: row.maturityDate,
    startDate: "",
    autoTransferDay: row.autoTransferDay,
    transferDay: "",
    repaymentDay: "",
    paymentDay: "",
    periodStartMonth: "",
    periodStartDay: "",
    periodEndMonth: "",
    periodEndDay: "",
    principal: "",
    cardLimit: "",
    openingBalance: Number(row.openingBalance.replace(/,/g, "")) || 0,
    openingDate: normalizeYmdDate(row.openingDate),
  };
}

function fromInsurance(row: InsuranceProduct, sortOrder: number): FinancialProduct {
  return {
    id: apiRowId(row.id),
    productType: "INSURANCE",
    status: savingsStatusToApi(row.status),
    sortOrder,
    classification: row.classification,
    name: row.name,
    paymentMethod: row.paymentMethod,
    joinDate: row.joinDate,
    maturityDate: row.maturityDate,
    startDate: "",
    autoTransferDay: "",
    transferDay: row.transferDay,
    repaymentDay: "",
    paymentDay: "",
    periodStartMonth: "",
    periodStartDay: "",
    periodEndMonth: "",
    periodEndDay: "",
    principal: "",
    cardLimit: "",
    openingBalance: Number(row.openingBalance.replace(/,/g, "")) || 0,
    openingDate: normalizeYmdDate(row.openingDate),
  };
}

function fromLoan(row: LoanProduct, sortOrder: number): FinancialProduct {
  return {
    id: null,
    productType: "LOAN",
    status: loanStatusToApi(row.status),
    sortOrder,
    classification: "",
    name: row.name,
    paymentMethod: "",
    joinDate: "",
    maturityDate: row.maturityDate,
    startDate: row.startDate,
    autoTransferDay: "",
    transferDay: "",
    repaymentDay: row.repaymentDay,
    paymentDay: "",
    periodStartMonth: "",
    periodStartDay: "",
    periodEndMonth: "",
    periodEndDay: "",
    principal: row.principal,
    cardLimit: "",
    openingBalance: 0,
    openingDate: "",
  };
}

function fromCard(row: CardProduct, sortOrder: number): FinancialProduct {
  return {
    id: null,
    productType: "CARD",
    status: cardStatusToApi(row.status),
    sortOrder,
    classification: row.classification,
    name: row.name,
    paymentMethod: "",
    joinDate: normalizeYmdDate(row.usageStartDate),
    maturityDate: normalizeYmdDate(row.cancelDate),
    startDate: "",
    autoTransferDay: "",
    transferDay: "",
    repaymentDay: "",
    paymentDay: normalizeCardDay(row.paymentDay),
    periodStartMonth: "",
    periodStartDay: "",
    periodEndMonth: "",
    periodEndDay: "",
    principal: "",
    cardLimit: "",
    openingBalance: 0,
    openingDate: "",
  };
}

export function mapProductListFromApi(list: ProductList): ProductStore {
  return {
    savings: list.savings.map(toSavings),
    insurance: list.insurance.map(toInsurance),
    loans: list.loans.map(toLoan),
    cards: list.cards.map(toCard),
  };
}

export function mapProductListToApi(store: ProductStore): ProductList {
  let order = 0;
  const savings = store.savings.map((row) => fromSavings(row, order++));
  const insurance = store.insurance.map((row) => fromInsurance(row, order++));
  const loans = store.loans.map((row) => fromLoan(row, order++));
  const cards = store.cards.map((row) => fromCard(row, order++));
  return { savings, insurance, loans, cards };
}

export async function loadProductStore(book: LedgerBook): Promise<ProductStore> {
  const list = await fetchFinancialProducts(book);
  return mapProductListFromApi(list);
}

export async function saveProductStore(book: LedgerBook, store: ProductStore): Promise<ProductStore> {
  const saved = await saveFinancialProducts(book, mapProductListToApi(store));
  return mapProductListFromApi(saved);
}

export async function syncCardsFromTransactions(
  book: LedgerBook
): Promise<{ added: number; store: ProductStore }> {
  const result = await apiSyncCardsFromTransactions(book);
  return { added: result.added, store: mapProductListFromApi(result.products) };
}

export function createSavingsRow(): SavingsProduct {
  return {
    id: newId(),
    classification: "예금",
    name: "",
    joinDate: "",
    maturityDate: "",
    autoTransferDay: "",
    openingBalance: "",
    openingDate: "",
    status: "active",
  };
}

export function createInsuranceRow(): InsuranceProduct {
  return {
    id: newId(),
    classification: "보장성보험",
    name: "",
    paymentMethod: "현금",
    joinDate: "",
    maturityDate: "",
    transferDay: "15",
    openingBalance: "",
    openingDate: "",
    status: "active",
  };
}

export function createLoanRow(): LoanProduct {
  return {
    id: newId(),
    name: "",
    principal: "",
    startDate: "",
    maturityDate: "",
    repaymentDay: "",
    status: "active",
  };
}

export function createCardRow(classification = "신용카드"): CardProduct {
  const isCredit = classification === "신용카드" || classification === "기타";
  return {
    id: newId(),
    classification,
    name: "",
    paymentDay: isCredit ? "13" : "1",
    usageStartDate: "",
    cancelDate: "",
    limit: "",
    status: "active",
  };
}

export const SAVINGS_CLASSES = ["예금", "적금", "청약저축", "주식/투자", "기타"];
export const INSURANCE_CLASSES = ["보장성보험", "저축성보험", "기타"];
export const INSURANCE_PAYMENTS = ["현금", "카드", "계좌이체"];
export const CARD_CLASSES = ["신용카드", "체크카드", "기타"];

/** 카드 결제일·기간 일자 — 숫자만 저장 (1~31) */
export function normalizeCardDay(value: string): string {
  const digits = value.replace(/\D/g, "");
  if (!digits) return "";
  const n = Math.min(31, Math.max(1, parseInt(digits, 10)));
  return String(n);
}
