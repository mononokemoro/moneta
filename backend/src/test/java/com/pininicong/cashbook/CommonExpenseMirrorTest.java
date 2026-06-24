package com.pininicong.cashbook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.domain.ExpenseScope;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.TransactionCreateRequest;
import com.pininicong.cashbook.dto.TransactionUpdateRequest;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.CashbookService;
import com.pininicong.cashbook.service.CategoryService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class CommonExpenseMirrorTest {

    @Autowired CashbookService cashbookService;
    @Autowired CbTransactionRepository txRepo;
    @Autowired CategoryService categoryService;

    @org.junit.jupiter.api.BeforeEach
    void seedCategories() {
        categoryService.migrateExpenseHierarchy(LedgerBook.PERSONAL);
        categoryService.migrateExpenseHierarchy(LedgerBook.HOUSEHOLD);
    }

    @Test
    void commonHouseholdExpenseMirrorsToPersonal() {
        LocalDate date = LocalDate.of(2026, 6, 18);
        var created =
                cashbookService.createTransaction(
                        new TransactionCreateRequest(
                                date,
                                TxType.EXPENSE,
                                "공과금",
                                new BigDecimal("50000"),
                                null,
                                "기타",
                                null,
                                "",
                                null,
                                "",
                                null,
                                LedgerBook.HOUSEHOLD,
                                ExpenseScope.COMMON,
                                null,
                                null));

        var household =
                txRepo.findById(created.id()).orElseThrow();
        assertThat(household.getExpenseScope()).isEqualTo(ExpenseScope.COMMON);
        assertThat(household.getLinkedTxId()).isNotNull();

        var personal = txRepo.findById(household.getLinkedTxId()).orElseThrow();
        assertThat(personal.getBook()).isEqualTo(LedgerBook.PERSONAL);
        assertThat(personal.getTitle()).isEqualTo("공과금");
        assertThat(personal.getAmount()).isEqualByComparingTo("50000");
        assertThat(personal.getLinkedTxId()).isEqualTo(household.getId());

        cashbookService.updateTransaction(
                household.getId(),
                new TransactionUpdateRequest(
                        "공과금(수정)",
                        new BigDecimal("55000"),
                        null,
                        "기타",
                        null,
                        "",
                        null,
                        "",
                        null,
                        ExpenseScope.COMMON,
                        null,
                        null,
                        null));

        personal = txRepo.findById(personal.getId()).orElseThrow();
        assertThat(personal.getTitle()).isEqualTo("공과금(수정)");
        assertThat(personal.getAmount()).isEqualByComparingTo("55000");

        long personalId = personal.getId();
        cashbookService.deleteTransaction(household.getId());
        assertThat(txRepo.findById(household.getId())).isEmpty();
        assertThat(txRepo.findById(personalId)).isEmpty();
    }

    @Test
    void commonPersonalExpenseMirrorsToHousehold() {
        LocalDate date = LocalDate.of(2026, 6, 20);
        var created =
                cashbookService.createTransaction(
                        new TransactionCreateRequest(
                                date,
                                TxType.EXPENSE,
                                "식비",
                                new BigDecimal("12000"),
                                null,
                                "기타",
                                null,
                                "",
                                null,
                                "",
                                null,
                                LedgerBook.PERSONAL,
                                ExpenseScope.COMMON,
                                null,
                                null));

        var personal = txRepo.findById(created.id()).orElseThrow();
        assertThat(personal.getExpenseScope()).isEqualTo(ExpenseScope.COMMON);
        assertThat(personal.getLinkedTxId()).isNotNull();

        var household = txRepo.findById(personal.getLinkedTxId()).orElseThrow();
        assertThat(household.getBook()).isEqualTo(LedgerBook.HOUSEHOLD);
        assertThat(household.getTitle()).isEqualTo("식비");
        assertThat(household.getAmount()).isEqualByComparingTo("12000");
        assertThat(household.getLinkedTxId()).isEqualTo(personal.getId());
    }

    @Test
    void normalHouseholdExpenseDoesNotMirror() {
        LocalDate date = LocalDate.of(2026, 6, 19);
        long before = txRepo.count();

        cashbookService.createTransaction(
                new TransactionCreateRequest(
                        date,
                        TxType.EXPENSE,
                        "간식",
                        new BigDecimal("3000"),
                        null,
                        "기타",
                        null,
                        "",
                        null,
                        "",
                        null,
                        LedgerBook.HOUSEHOLD,
                        ExpenseScope.NORMAL,
                        null,
                        null));

        assertThat(txRepo.count()).isEqualTo(before + 1);
    }
}
