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
  periodStartMonth: string;
  periodStartDay: string;
  periodEndMonth: string;
  periodEndDay: string;
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

function toSavings(row: FinancialProduct): SavingsProduct {
  return {
    id: rowId(row.id),
    classification: row.classification,
    name: row.name,
    joinDate: row.joinDate,
    maturityDate: row.maturityDate,
    autoTransferDay: row.autoTransferDay,
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

function toCard(row: FinancialProduct): CardProduct {
  return {
    id: rowId(row.id),
    classification: row.classification,
    name: row.name,
    paymentDay: row.paymentDay,
    periodStartMonth: row.periodStartMonth,
    periodStartDay: row.periodStartDay,
    periodEndMonth: row.periodEndMonth,
    periodEndDay: row.periodEndDay,
    limit: row.cardLimit,
    status: cardStatusFromApi(row.status),
  };
}

function fromSavings(row: SavingsProduct, sortOrder: number): FinancialProduct {
  return {
    id: null,
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
  };
}

function fromInsurance(row: InsuranceProduct, sortOrder: number): FinancialProduct {
  return {
    id: null,
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
    joinDate: "",
    maturityDate: "",
    startDate: "",
    autoTransferDay: "",
    transferDay: "",
    repaymentDay: "",
    paymentDay: row.paymentDay,
    periodStartMonth: row.periodStartMonth,
    periodStartDay: row.periodStartDay,
    periodEndMonth: row.periodEndMonth,
    periodEndDay: row.periodEndDay,
    principal: "",
    cardLimit: row.limit,
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
    paymentDay: isCredit ? "13" : "01",
    periodStartMonth: isCredit ? "전월" : "당월",
    periodStartDay: isCredit ? "01" : "",
    periodEndMonth: isCredit ? "전월" : "당월",
    periodEndDay: isCredit ? "31" : "",
    limit: "",
    status: "active",
  };
}

export const SAVINGS_CLASSES = ["예금", "적금", "청약저축", "주식/투자", "기타"];
export const INSURANCE_CLASSES = ["보장성보험", "저축성보험", "기타"];
export const INSURANCE_PAYMENTS = ["현금", "카드", "계좌이체"];
export const CARD_CLASSES = ["신용카드", "체크카드", "기타"];
export const MONTH_OPTIONS = ["전월", "당월"];
