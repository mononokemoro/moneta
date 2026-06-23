package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbFixedItem;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CbFixedItemRepository extends JpaRepository<CbFixedItem, Long> {

    List<CbFixedItem> findByBookOrderBySortOrderAscIdAsc(LedgerBook book);

    void deleteByBook(LedgerBook book);

    long countByBook(LedgerBook book);

    @Modifying(clearAutomatically = true)
    @Query(
            """
            UPDATE CbFixedItem f SET f.category = :newName
            WHERE f.book = :book AND f.txType = :txType AND f.category = :oldName
            """)
    int renameCategory(
            @Param("book") LedgerBook book,
            @Param("txType") TxType txType,
            @Param("oldName") String oldName,
            @Param("newName") String newName);

    @Modifying(clearAutomatically = true)
    @Query(
            """
            UPDATE CbFixedItem f SET f.title = :newName
            WHERE f.book = :book AND f.txType = :txType AND f.title = :oldName
            """)
    int renameTitle(
            @Param("book") LedgerBook book,
            @Param("txType") TxType txType,
            @Param("oldName") String oldName,
            @Param("newName") String newName);
}
