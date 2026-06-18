package com.pininicong.cashbook.config;



import com.pininicong.cashbook.domain.CbCashBalance;

import com.pininicong.cashbook.domain.CbFixedItem;

import com.pininicong.cashbook.domain.CbMonthlyBudget;

import com.pininicong.cashbook.domain.CbTransaction;

import com.pininicong.cashbook.domain.LedgerBook;

import com.pininicong.cashbook.domain.TxType;

import com.pininicong.cashbook.repo.CbCashBalanceRepository;

import com.pininicong.cashbook.repo.CbCategoryRepository;

import com.pininicong.cashbook.repo.CbFixedItemRepository;

import com.pininicong.cashbook.repo.CbMonthlyBudgetRepository;

import com.pininicong.cashbook.repo.CbTransactionRepository;

import com.pininicong.cashbook.service.MonetaImportService;

import java.math.BigDecimal;

import java.time.LocalDate;

import java.time.YearMonth;

import org.springframework.boot.ApplicationArguments;

import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import org.springframework.transaction.annotation.Transactional;



@Component
@Order(50)
public class CashbookDataInitializer implements ApplicationRunner {



    private static final LedgerBook SEED_BOOK = LedgerBook.PERSONAL;



    private final CbCashBalanceRepository cashRepo;

    private final CbMonthlyBudgetRepository budgetRepo;

    private final CbTransactionRepository txRepo;

    private final CbFixedItemRepository fixedRepo;

    private final CbCategoryRepository categoryRepo;

    private final MonetaImportService importService;



    public CashbookDataInitializer(

            CbCashBalanceRepository cashRepo,

            CbMonthlyBudgetRepository budgetRepo,

            CbTransactionRepository txRepo,

            CbFixedItemRepository fixedRepo,

            CbCategoryRepository categoryRepo,

            MonetaImportService importService) {

        this.cashRepo = cashRepo;

        this.budgetRepo = budgetRepo;

        this.txRepo = txRepo;

        this.fixedRepo = fixedRepo;

        this.categoryRepo = categoryRepo;

        this.importService = importService;

    }



    @Override

    @Transactional

    public void run(ApplicationArguments args) {

        if (!cashRepo.existsByBook(SEED_BOOK)) {

            seedBaseData();

        }

        if (fixedRepo.countByBook(SEED_BOOK) == 0) {

            seedFixedItems();

        }

        if (categoryRepo.countByBook(SEED_BOOK) == 0) {

            importService.seedCategoriesFromClasspath(SEED_BOOK);

        }

    }



    private void seedBaseData() {

        CbCashBalance cash = new CbCashBalance();

        cash.setBook(SEED_BOOK);

        cash.setAmount(new BigDecimal("850000"));

        cashRepo.save(cash);



        YearMonth ym = YearMonth.now();

        CbMonthlyBudget budget = new CbMonthlyBudget();

        budget.setBook(SEED_BOOK);

        budget.setYearMonth(ym.toString());

        budget.setTotalBudget(new BigDecimal("2500000"));

        budgetRepo.save(budget);



        LocalDate today = LocalDate.now();

        seed(txRepo, today, TxType.EXPENSE, "점심 식비", new BigDecimal("12000"), "식비", "", "");

        seed(txRepo, today, TxType.INCOME, "이자 수입", new BigDecimal("350"), "수입", "", "");

        seedSavings(txRepo, today, "적금 불입", new BigDecimal("300000"), new BigDecimal("3600000"), "청년 적금");

    }



    private void seedFixedItems() {

        seedFixed(fixedRepo, "네이버적립", TxType.INCOME, "수입", "", 0);

        seedFixed(fixedRepo, "3.네이버", TxType.EXPENSE, "쇼핑", "", 1);

        seedFixed(fixedRepo, "예치.토스이", TxType.SAVINGS, "저축", "", 2);

        seedFixed(fixedRepo, "예치.토스", TxType.SAVINGS, "저축", "", 3);

    }



    private static void seed(

            CbTransactionRepository repo,

            LocalDate date,

            TxType type,

            String title,

            BigDecimal amount,

            String category,

            String cardName,

            String remarks) {

        CbTransaction t = new CbTransaction();

        t.setBook(SEED_BOOK);

        t.setTxDate(date);

        t.setTxType(type);

        t.setTitle(title);

        t.setAmount(amount);

        t.setCategory(category);

        t.setCardName(cardName);

        t.setRemarks(remarks);

        t.setSortOrder(0);

        repo.save(t);

    }



    private static void seedSavings(

            CbTransactionRepository repo,

            LocalDate date,

            String title,

            BigDecimal amount,

            BigDecimal accumulated,

            String remarks) {

        CbTransaction t = new CbTransaction();

        t.setBook(SEED_BOOK);

        t.setTxDate(date);

        t.setTxType(TxType.SAVINGS);

        t.setTitle(title);

        t.setAmount(amount);

        t.setCategory("저축");

        t.setCardName("");

        t.setRemarks(remarks);

        t.setAccumulatedAmount(accumulated);

        t.setSortOrder(0);

        repo.save(t);

    }



    private static void seedFixed(

            CbFixedItemRepository repo,

            String title,

            TxType type,

            String category,

            String cardName,

            int sortOrder) {

        CbFixedItem f = new CbFixedItem();

        f.setBook(SEED_BOOK);

        f.setTitle(title);

        f.setTxType(type);

        f.setCategory(category);

        f.setCardName(cardName);

        f.setSortOrder(sortOrder);

        repo.save(f);

    }

}

