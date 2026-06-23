package com.pininicong.cashbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "cb_book_settings")
public class CbBookSettings {

    @Id
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private LedgerBook book = LedgerBook.PERSONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "expense_field_mode", nullable = false, length = 16)
    private ItemFieldMode expenseFieldMode = ItemFieldMode.REMARKS;

    @Enumerated(EnumType.STRING)
    @Column(name = "savings_insurance_field_mode", nullable = false, length = 16)
    private ItemFieldMode savingsInsuranceFieldMode = ItemFieldMode.REMARKS;

    @Enumerated(EnumType.STRING)
    @Column(name = "loan_field_mode", nullable = false, length = 16)
    private ItemFieldMode loanFieldMode = ItemFieldMode.REMARKS;

    @Enumerated(EnumType.STRING)
    @Column(name = "income_field_mode", nullable = false, length = 16)
    private ItemFieldMode incomeFieldMode = ItemFieldMode.REMARKS;

    public LedgerBook getBook() {
        return book;
    }

    public void setBook(LedgerBook book) {
        this.book = book;
    }

    public ItemFieldMode getExpenseFieldMode() {
        return expenseFieldMode;
    }

    public void setExpenseFieldMode(ItemFieldMode expenseFieldMode) {
        this.expenseFieldMode = expenseFieldMode != null ? expenseFieldMode : ItemFieldMode.REMARKS;
    }

    public ItemFieldMode getSavingsInsuranceFieldMode() {
        return savingsInsuranceFieldMode;
    }

    public void setSavingsInsuranceFieldMode(ItemFieldMode savingsInsuranceFieldMode) {
        this.savingsInsuranceFieldMode =
                savingsInsuranceFieldMode != null ? savingsInsuranceFieldMode : ItemFieldMode.REMARKS;
    }

    public ItemFieldMode getLoanFieldMode() {
        return loanFieldMode;
    }

    public void setLoanFieldMode(ItemFieldMode loanFieldMode) {
        this.loanFieldMode = loanFieldMode != null ? loanFieldMode : ItemFieldMode.REMARKS;
    }

    public ItemFieldMode getIncomeFieldMode() {
        return incomeFieldMode;
    }

    public void setIncomeFieldMode(ItemFieldMode incomeFieldMode) {
        this.incomeFieldMode = incomeFieldMode != null ? incomeFieldMode : ItemFieldMode.REMARKS;
    }
}
