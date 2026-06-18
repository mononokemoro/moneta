package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CbTransactionRepository extends JpaRepository<CbTransaction, Long> {

    List<CbTransaction> findByBookAndTxDateAndTxTypeOrderBySortOrderAscIdAsc(
            LedgerBook book, LocalDate txDate, TxType txType);

    List<CbTransaction> findByBookAndTxDateBetween(LedgerBook book, LocalDate start, LocalDate end);

    void deleteByBook(LedgerBook book);

    @Query(
            """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM CbTransaction t
            WHERE t.book = :book AND t.txType = :tp AND t.txDate >= :start AND t.txDate <= :end
            """)
    BigDecimal sumAmountBetween(
            @Param("book") LedgerBook book,
            @Param("tp") TxType type,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);
}
