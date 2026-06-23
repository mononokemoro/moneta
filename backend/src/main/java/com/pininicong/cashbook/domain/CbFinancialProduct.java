package com.pininicong.cashbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cb_financial_product")
public class CbFinancialProduct {

    public enum ProductType {
        SAVINGS,
        INSURANCE,
        LOAN,
        CARD
    }

    public enum ProductStatus {
        ACTIVE,
        MATURED,
        TERMINATED,
        PREPAID,
        CANCELLED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private LedgerBook book = LedgerBook.PERSONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 16)
    private ProductType productType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ProductStatus status = ProductStatus.ACTIVE;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(length = 40)
    private String classification = "";

    @Column(nullable = false, length = 120)
    private String name = "";

    @Column(name = "payment_method", length = 24)
    private String paymentMethod = "";

    @Column(name = "join_date", length = 8)
    private String joinDate = "";

    @Column(name = "maturity_date", length = 8)
    private String maturityDate = "";

    @Column(name = "start_date", length = 8)
    private String startDate = "";

    @Column(name = "auto_transfer_day", length = 4)
    private String autoTransferDay = "";

    @Column(name = "transfer_day", length = 4)
    private String transferDay = "";

    @Column(name = "repayment_day", length = 4)
    private String repaymentDay = "";

    @Column(name = "payment_day", length = 4)
    private String paymentDay = "";

    @Column(name = "period_start_month", length = 8)
    private String periodStartMonth = "";

    @Column(name = "period_start_day", length = 4)
    private String periodStartDay = "";

    @Column(name = "period_end_month", length = 8)
    private String periodEndMonth = "";

    @Column(name = "period_end_day", length = 4)
    private String periodEndDay = "";

    @Column(length = 24)
    private String principal = "";

    @Column(name = "card_limit", length = 24)
    private String cardLimit = "";

    public Long getId() {
        return id;
    }

    public LedgerBook getBook() {
        return book;
    }

    public void setBook(LedgerBook book) {
        this.book = book;
    }

    public ProductType getProductType() {
        return productType;
    }

    public void setProductType(ProductType productType) {
        this.productType = productType;
    }

    public ProductStatus getStatus() {
        return status;
    }

    public void setStatus(ProductStatus status) {
        this.status = status != null ? status : ProductStatus.ACTIVE;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder != null ? sortOrder : 0;
    }

    public String getClassification() {
        return classification;
    }

    public void setClassification(String classification) {
        this.classification = classification != null ? classification : "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name != null ? name : "";
    }

    public String getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod != null ? paymentMethod : "";
    }

    public String getJoinDate() {
        return joinDate;
    }

    public void setJoinDate(String joinDate) {
        this.joinDate = joinDate != null ? joinDate : "";
    }

    public String getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(String maturityDate) {
        this.maturityDate = maturityDate != null ? maturityDate : "";
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate != null ? startDate : "";
    }

    public String getAutoTransferDay() {
        return autoTransferDay;
    }

    public void setAutoTransferDay(String autoTransferDay) {
        this.autoTransferDay = autoTransferDay != null ? autoTransferDay : "";
    }

    public String getTransferDay() {
        return transferDay;
    }

    public void setTransferDay(String transferDay) {
        this.transferDay = transferDay != null ? transferDay : "";
    }

    public String getRepaymentDay() {
        return repaymentDay;
    }

    public void setRepaymentDay(String repaymentDay) {
        this.repaymentDay = repaymentDay != null ? repaymentDay : "";
    }

    public String getPaymentDay() {
        return paymentDay;
    }

    public void setPaymentDay(String paymentDay) {
        this.paymentDay = paymentDay != null ? paymentDay : "";
    }

    public String getPeriodStartMonth() {
        return periodStartMonth;
    }

    public void setPeriodStartMonth(String periodStartMonth) {
        this.periodStartMonth = periodStartMonth != null ? periodStartMonth : "";
    }

    public String getPeriodStartDay() {
        return periodStartDay;
    }

    public void setPeriodStartDay(String periodStartDay) {
        this.periodStartDay = periodStartDay != null ? periodStartDay : "";
    }

    public String getPeriodEndMonth() {
        return periodEndMonth;
    }

    public void setPeriodEndMonth(String periodEndMonth) {
        this.periodEndMonth = periodEndMonth != null ? periodEndMonth : "";
    }

    public String getPeriodEndDay() {
        return periodEndDay;
    }

    public void setPeriodEndDay(String periodEndDay) {
        this.periodEndDay = periodEndDay != null ? periodEndDay : "";
    }

    public String getPrincipal() {
        return principal;
    }

    public void setPrincipal(String principal) {
        this.principal = principal != null ? principal : "";
    }

    public String getCardLimit() {
        return cardLimit;
    }

    public void setCardLimit(String cardLimit) {
        this.cardLimit = cardLimit != null ? cardLimit : "";
    }
}
