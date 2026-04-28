package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbMonthlyBudget;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbMonthlyBudgetRepository extends JpaRepository<CbMonthlyBudget, String> {}
