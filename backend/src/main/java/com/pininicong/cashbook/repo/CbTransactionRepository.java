package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.TxType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CbTransactionRepository extends JpaRepository<CbTransaction, Long> {

    List<CbTransaction> findByTxDateAndTxTypeOrderBySortOrderAscIdAsc(LocalDate txDate, TxType txType);

    List<CbTransaction> findByTxDateBetween(LocalDate start, LocalDate end);

    @Query(
            """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM CbTransaction t
            WHERE t.txType = :tp AND t.txDate >= :start AND t.txDate <= :end
            """)
    BigDecimal sumAmountBetween(
            @Param("tp") TxType type, @Param("start") LocalDate start, @Param("end") LocalDate end);
}
