package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbCategoryKeyword;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbCategoryKeywordRepository extends JpaRepository<CbCategoryKeyword, Long> {

    List<CbCategoryKeyword> findByBookAndTxTypeOrderBySortOrderAscKeywordAsc(
            LedgerBook book, TxType txType);
}
