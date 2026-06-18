package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbCashBalance;
import com.pininicong.cashbook.domain.LedgerBook;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbCashBalanceRepository extends JpaRepository<CbCashBalance, LedgerBook> {

    boolean existsByBook(LedgerBook book);
}
