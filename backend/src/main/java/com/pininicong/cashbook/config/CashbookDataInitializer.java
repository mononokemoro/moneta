package com.pininicong.cashbook.config;

import com.pininicong.cashbook.domain.CbCashBalance;
import com.pininicong.cashbook.domain.CbMonthlyBudget;
import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.repo.CbCashBalanceRepository;
import com.pininicong.cashbook.repo.CbMonthlyBudgetRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CashbookDataInitializer implements ApplicationRunner {

    private final CbCashBalanceRepository cashRepo;
    private final CbMonthlyBudgetRepository budgetRepo;
    private final CbTransactionRepository txRepo;

    public CashbookDataInitializer(
            CbCashBalanceRepository cashRepo,
            CbMonthlyBudgetRepository budgetRepo,
            CbTransactionRepository txRepo) {
        this.cashRepo = cashRepo;
        this.budgetRepo = budgetRepo;
        this.txRepo = txRepo;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (cashRepo.count() > 0) {
            return;
        }

        CbCashBalance cash = new CbCashBalance();
        cash.setAmount(new BigDecimal("850000"));
        cashRepo.save(cash);

        YearMonth ym = YearMonth.now();
        CbMonthlyBudget budget = new CbMonthlyBudget();
        budget.setYearMonth(ym.toString());
        budget.setTotalBudget(new BigDecimal("2500000"));
        budgetRepo.save(budget);

        LocalDate today = LocalDate.now();
        seed(txRepo, today, TxType.EXPENSE, "점심 식비", new BigDecimal("12000"), "식비", "", "");
        seed(txRepo, today, TxType.INCOME, "이자 수입", new BigDecimal("350"), "수입", "", "");
        seedSavings(txRepo, today, "적금 불입", new BigDecimal("300000"), new BigDecimal("3600000"), "청년 적금");
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
}
