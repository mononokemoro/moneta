package com.pininicong.cashbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "cb_monthly_budget")
@IdClass(MonthlyBudgetKey.class)
public class CbMonthlyBudget {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private LedgerBook book = LedgerBook.PERSONAL;

    /** YYYY-MM */
    @Id
    @Column(name = "year_month", length = 7)
    private String yearMonth;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalBudget = BigDecimal.ZERO;

    public LedgerBook getBook() {
        return book;
    }

    public void setBook(LedgerBook book) {
        this.book = book;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public void setYearMonth(String yearMonth) {
        this.yearMonth = yearMonth;
    }

    public BigDecimal getTotalBudget() {
        return totalBudget;
    }

    public void setTotalBudget(BigDecimal totalBudget) {
        this.totalBudget = totalBudget;
    }
}
