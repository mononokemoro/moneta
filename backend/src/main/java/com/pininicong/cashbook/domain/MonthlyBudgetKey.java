package com.pininicong.cashbook.domain;

import java.io.Serializable;
import java.util.Objects;

public class MonthlyBudgetKey implements Serializable {

    private LedgerBook book;
    private String yearMonth;

    public MonthlyBudgetKey() {}

    public MonthlyBudgetKey(LedgerBook book, String yearMonth) {
        this.book = book;
        this.yearMonth = yearMonth;
    }

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MonthlyBudgetKey that)) return false;
        return book == that.book && Objects.equals(yearMonth, that.yearMonth);
    }

    @Override
    public int hashCode() {
        return Objects.hash(book, yearMonth);
    }
}
