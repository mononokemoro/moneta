package com.pininicong.cashbook;

import static org.assertj.core.api.Assertions.assertThat;

import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.ExpenseScope;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.domain.CategoryTier;
import com.pininicong.cashbook.dto.CategoryCreateRequest;
import com.pininicong.cashbook.dto.TransactionCreateRequest;
import com.pininicong.cashbook.dto.TransactionUpdateRequest;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.CashbookService;
import com.pininicong.cashbook.service.CategoryService;
import com.pininicong.cashbook.service.TransactionCategorySupport;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class PersonalHouseholdMirrorTest {

    @Autowired CashbookService cashbookService;
    @Autowired CbTransactionRepository txRepo;
    @Autowired CategoryService categoryService;
    @Autowired TransactionCategorySupport categorySupport;

    private Long householdMinorId;

    @BeforeEach
    void seedCategories() {
        categoryService.migrateExpenseHierarchy(LedgerBook.PERSONAL);
        categoryService.migrateExpenseHierarchy(LedgerBook.HOUSEHOLD);
        householdMinorId =
                categorySupport
                        .findMinorIdByName(LedgerBook.HOUSEHOLD, CategoryType.EXPENSE, "식료품")
                        .orElseGet(
                                () -> {
                                    var major =
                                            categoryService.create(
                                                    LedgerBook.HOUSEHOLD,
                                                    new CategoryCreateRequest(
                                                            CategoryType.EXPENSE,
                                                            CategoryTier.MAJOR,
                                                            "가계테스트",
                                                            null));
                                    return categoryService
                                            .create(
                                                    LedgerBook.HOUSEHOLD,
                                                    new CategoryCreateRequest(
                                                            CategoryType.EXPENSE,
                                                            CategoryTier.MINOR,
                                                            "식료품",
                                                            major.id()))
                                            .id();
                                });
    }

    @Test
    void personalExpenseWithHouseholdCategoryMirrorsToHouseholdBook() {
        LocalDate date = LocalDate.of(2026, 6, 18);
        long before = txRepo.count();

        var created =
                cashbookService.createTransaction(
                        new TransactionCreateRequest(
                                date,
                                TxType.EXPENSE,
                                "마트",
                                new BigDecimal("35000"),
                                null,
                                "외식",
                                null,
                                "",
                                null,
                                "",
                                null,
                                LedgerBook.PERSONAL,
                                ExpenseScope.NORMAL,
                                householdMinorId,
                                null));

        assertThat(txRepo.count()).isEqualTo(before + 2);

        var personal = txRepo.findById(created.id()).orElseThrow();
        assertThat(personal.getCategoryId()).isNotNull();
        assertThat(personal.getHouseholdCategoryId()).isEqualTo(householdMinorId);
        assertThat(personal.getLinkedTxId()).isNotNull();

        var household = txRepo.findById(personal.getLinkedTxId()).orElseThrow();
        assertThat(household.getBook()).isEqualTo(LedgerBook.HOUSEHOLD);
        assertThat(household.getCategoryId()).isEqualTo(householdMinorId);
        assertThat(household.getTitle()).isEqualTo("마트");
        assertThat(household.getExpenseScope()).isEqualTo(ExpenseScope.NORMAL);
        assertThat(household.getLinkedTxId()).isEqualTo(personal.getId());
    }

    @Test
    void clearingHouseholdCategoryRemovesMirror() {
        LocalDate date = LocalDate.of(2026, 6, 19);
        var created =
                cashbookService.createTransaction(
                        new TransactionCreateRequest(
                                date,
                                TxType.EXPENSE,
                                "커피",
                                new BigDecimal("5000"),
                                null,
                                "외식",
                                null,
                                "",
                                null,
                                "",
                                null,
                                LedgerBook.PERSONAL,
                                ExpenseScope.NORMAL,
                                householdMinorId,
                                null));

        var personal = txRepo.findById(created.id()).orElseThrow();
        Long householdId = personal.getLinkedTxId();
        assertThat(householdId).isNotNull();

        cashbookService.updateTransaction(
                personal.getId(),
                new TransactionUpdateRequest(
                        "커피",
                        new BigDecimal("5000"),
                        null,
                        "외식",
                        null,
                        "",
                        null,
                        "",
                        null,
                        ExpenseScope.NORMAL,
                        null,
                        null,
                        null));

        assertThat(txRepo.findById(householdId)).isEmpty();
        personal = txRepo.findById(personal.getId()).orElseThrow();
        assertThat(personal.getLinkedTxId()).isNull();
    }

    @Test
    void deletingPersonalExpenseDeletesHouseholdMirror() {
        LocalDate date = LocalDate.of(2026, 6, 21);
        var created =
                cashbookService.createTransaction(
                        new TransactionCreateRequest(
                                date,
                                TxType.EXPENSE,
                                "세탁비",
                                new BigDecimal("12000"),
                                null,
                                "외식",
                                null,
                                "",
                                null,
                                "",
                                null,
                                LedgerBook.PERSONAL,
                                ExpenseScope.NORMAL,
                                householdMinorId,
                                null));

        var personal = txRepo.findById(created.id()).orElseThrow();
        Long householdId = personal.getLinkedTxId();

        cashbookService.deleteTransaction(personal.getId());

        assertThat(txRepo.findById(personal.getId())).isEmpty();
        assertThat(txRepo.findById(householdId)).isEmpty();
    }
}
