package com.pininicong.cashbook.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.Objects;

public class DailySheetKey implements Serializable {

    private LedgerBook book;
    private LocalDate sheetDate;

    public DailySheetKey() {}

    public DailySheetKey(LedgerBook book, LocalDate sheetDate) {
        this.book = book;
        this.sheetDate = sheetDate;
    }

    public LedgerBook getBook() {
        return book;
    }

    public void setBook(LedgerBook book) {
        this.book = book;
    }

    public LocalDate getSheetDate() {
        return sheetDate;
    }

    public void setSheetDate(LocalDate sheetDate) {
        this.sheetDate = sheetDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DailySheetKey that)) return false;
        return book == that.book && Objects.equals(sheetDate, that.sheetDate);
    }

    @Override
    public int hashCode() {
        return Objects.hash(book, sheetDate);
    }
}
