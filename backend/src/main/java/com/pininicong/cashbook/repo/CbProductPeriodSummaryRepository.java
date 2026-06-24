package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbProductPeriodSummary;
import com.pininicong.cashbook.domain.CbProductPeriodSummary.PeriodType;
import com.pininicong.cashbook.domain.LedgerBook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbProductPeriodSummaryRepository extends JpaRepository<CbProductPeriodSummary, Long> {

    Optional<CbProductPeriodSummary> findByBookAndProductIdAndPeriodTypeAndPeriodKey(
            LedgerBook book, Long productId, PeriodType periodType, String periodKey);

    List<CbProductPeriodSummary> findByBookAndPeriodTypeAndPeriodKey(
            LedgerBook book, PeriodType periodType, String periodKey);

    void deleteByBookAndPeriodTypeAndPeriodKey(
            LedgerBook book, PeriodType periodType, String periodKey);
}
