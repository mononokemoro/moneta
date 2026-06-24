package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbBalanceAdjustment;
import com.pininicong.cashbook.domain.LedgerBook;
import java.time.LocalDate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CbBalanceAdjustmentRepository extends JpaRepository<CbBalanceAdjustment, Long> {

    @Query(
            """
            SELECT COALESCE(SUM(a.amount), 0)
            FROM CbBalanceAdjustment a
            WHERE a.book = :book AND a.productId = :productId
            AND a.adjDate > :afterDate AND a.adjDate <= :untilDate
            """)
    java.math.BigDecimal sumBetween(
            @Param("book") LedgerBook book,
            @Param("productId") Long productId,
            @Param("afterDate") LocalDate afterDate,
            @Param("untilDate") LocalDate untilDate);
}
