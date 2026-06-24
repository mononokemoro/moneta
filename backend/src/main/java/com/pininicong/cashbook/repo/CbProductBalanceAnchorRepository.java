package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbProductBalanceAnchor;
import com.pininicong.cashbook.domain.LedgerBook;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbProductBalanceAnchorRepository extends JpaRepository<CbProductBalanceAnchor, Long> {

    Optional<CbProductBalanceAnchor> findTopByBookAndProductIdAndAnchorDateLessThanEqualOrderByAnchorDateDescIdDesc(
            LedgerBook book, Long productId, LocalDate anchorDate);

    Optional<CbProductBalanceAnchor> findByBookAndProductIdAndAnchorDate(
            LedgerBook book, Long productId, LocalDate anchorDate);

    List<CbProductBalanceAnchor> findByBookAndProductIdOrderByAnchorDateAscIdAsc(
            LedgerBook book, Long productId);
}
