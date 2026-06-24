package com.pininicong.cashbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cb_transaction")
public class CbTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private LedgerBook book = LedgerBook.PERSONAL;

    @Column(nullable = false)
    private LocalDate txDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TxType txType;

    /** 지출/수입/저축 항목명 */
    @Column(nullable = false, length = 200)
    private String title = "";

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    /** 지출/수입 소분류 → cb_category.id */
    @Column(name = "category_id")
    private Long categoryId;

    /** 지출: 카드 상품 → cb_financial_product.id (CARD, 현금이면 null) */
    @Column(name = "card_product_id")
    private Long cardProductId;

    /** 저축/보험: 금융상품 → cb_financial_product.id */
    @Column(name = "savings_product_id")
    private Long savingsProductId;

    @Column(length = 500)
    private String remarks = "";

    /** 저축/보험: 누적금액 (해당 타입에서만 사용) */
    @Column(precision = 19, scale = 2)
    private BigDecimal accumulatedAmount;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    /** 가계 지출: 공통 항목이면 개인 장부에 연동 */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private ExpenseScope expenseScope = ExpenseScope.NORMAL;

    /** 연동된 다른 장부 거래 ID */
    private Long linkedTxId;

    /** 개인 지출: 가계 장부에 반영할 분류 → cb_category.id (HOUSEHOLD) */
    @Column(name = "household_category_id")
    private Long householdCategoryId;

    public Long getHouseholdCategoryId() {
        return householdCategoryId;
    }

    public void setHouseholdCategoryId(Long householdCategoryId) {
        this.householdCategoryId = householdCategoryId;
    }

    public Long getId() {
        return id;
    }

    public LedgerBook getBook() {
        return book;
    }

    public void setBook(LedgerBook book) {
        this.book = book;
    }

    public LocalDate getTxDate() {
        return txDate;
    }

    public void setTxDate(LocalDate txDate) {
        this.txDate = txDate;
    }

    public TxType getTxType() {
        return txType;
    }

    public void setTxType(TxType txType) {
        this.txType = txType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getCardProductId() {
        return cardProductId;
    }

    public void setCardProductId(Long cardProductId) {
        this.cardProductId = cardProductId;
    }

    public Long getSavingsProductId() {
        return savingsProductId;
    }

    public void setSavingsProductId(Long savingsProductId) {
        this.savingsProductId = savingsProductId;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public BigDecimal getAccumulatedAmount() {
        return accumulatedAmount;
    }

    public void setAccumulatedAmount(BigDecimal accumulatedAmount) {
        this.accumulatedAmount = accumulatedAmount;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public ExpenseScope getExpenseScope() {
        return expenseScope;
    }

    public void setExpenseScope(ExpenseScope expenseScope) {
        this.expenseScope = expenseScope != null ? expenseScope : ExpenseScope.NORMAL;
    }

    public Long getLinkedTxId() {
        return linkedTxId;
    }

    public void setLinkedTxId(Long linkedTxId) {
        this.linkedTxId = linkedTxId;
    }
}
