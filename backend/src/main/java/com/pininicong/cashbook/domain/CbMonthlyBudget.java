package com.pininicong.cashbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;

@Entity
@Table(name = "cb_monthly_budget")
public class CbMonthlyBudget {

    /** YYYY-MM */
    @Id
    @Column(length = 7)
    private String yearMonth;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal totalBudget = BigDecimal.ZERO;

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
