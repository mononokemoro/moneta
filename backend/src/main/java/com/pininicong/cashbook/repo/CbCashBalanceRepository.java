package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbCashBalance;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbCashBalanceRepository extends JpaRepository<CbCashBalance, Long> {}
