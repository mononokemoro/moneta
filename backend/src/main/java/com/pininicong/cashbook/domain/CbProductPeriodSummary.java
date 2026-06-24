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

@Entity
@Table(name = "cb_product_period_summary")
public class CbProductPeriodSummary {

    public enum PeriodType {
        MONTH,
        YEAR
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private LedgerBook book = LedgerBook.PERSONAL;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period_type", length = 8, nullable = false)
    private PeriodType periodType;

    @Column(name = "period_key", length = 16, nullable = false)
    private String periodKey;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal inflow = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal outflow = BigDecimal.ZERO;

    @Column(name = "net_flow", nullable = false, precision = 19, scale = 2)
    private BigDecimal netFlow = BigDecimal.ZERO;

    @Column(name = "end_balance", nullable = false, precision = 19, scale = 2)
    private BigDecimal endBalance = BigDecimal.ZERO;

    public Long getId() {
        return id;
    }

    public LedgerBook getBook() {
        return book;
    }

    public void setBook(LedgerBook book) {
        this.book = book;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public PeriodType getPeriodType() {
        return periodType;
    }

    public void setPeriodType(PeriodType periodType) {
        this.periodType = periodType;
    }

    public String getPeriodKey() {
        return periodKey;
    }

    public void setPeriodKey(String periodKey) {
        this.periodKey = periodKey;
    }

    public BigDecimal getInflow() {
        return inflow;
    }

    public void setInflow(BigDecimal inflow) {
        this.inflow = inflow != null ? inflow : BigDecimal.ZERO;
    }

    public BigDecimal getOutflow() {
        return outflow;
    }

    public void setOutflow(BigDecimal outflow) {
        this.outflow = outflow != null ? outflow : BigDecimal.ZERO;
    }

    public BigDecimal getNetFlow() {
        return netFlow;
    }

    public void setNetFlow(BigDecimal netFlow) {
        this.netFlow = netFlow != null ? netFlow : BigDecimal.ZERO;
    }

    public BigDecimal getEndBalance() {
        return endBalance;
    }

    public void setEndBalance(BigDecimal endBalance) {
        this.endBalance = endBalance != null ? endBalance : BigDecimal.ZERO;
    }
}
