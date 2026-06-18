package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbMonthlyBudget;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.MonthlyBudgetKey;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbMonthlyBudgetRepository extends JpaRepository<CbMonthlyBudget, MonthlyBudgetKey> {

    boolean existsByBook(LedgerBook book);
}
