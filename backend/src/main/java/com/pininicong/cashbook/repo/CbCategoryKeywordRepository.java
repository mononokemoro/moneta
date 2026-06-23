package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbCategoryKeyword;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CbCategoryKeywordRepository extends JpaRepository<CbCategoryKeyword, Long> {

    List<CbCategoryKeyword> findByBookAndTxTypeOrderBySortOrderAscKeywordAsc(
            LedgerBook book, TxType txType);

    @Modifying(clearAutomatically = true)
    @Query(
            """
            UPDATE CbCategoryKeyword k SET k.categoryName = :newName
            WHERE k.book = :book AND k.txType = :txType AND k.categoryName = :oldName
            """)
    int renameCategoryName(
            @Param("book") LedgerBook book,
            @Param("txType") TxType txType,
            @Param("oldName") String oldName,
            @Param("newName") String newName);
}
