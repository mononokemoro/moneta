package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    @Modifying(clearAutomatically = true)
    @Query(
            """
            UPDATE CbTransaction t SET t.category = :newName
            WHERE t.book = :book AND t.txType = :txType AND t.category = :oldName
            """)
    int renameCategory(
            @Param("book") LedgerBook book,
            @Param("txType") TxType txType,
            @Param("oldName") String oldName,
            @Param("newName") String newName);

    @Modifying(clearAutomatically = true)
    @Query(
            """
            UPDATE CbTransaction t SET t.title = :newName
            WHERE t.book = :book AND t.txType = :txType AND t.title = :oldName
            AND (:categoryFilter IS NULL OR t.category = :categoryFilter)
            """)
    int renameTitle(
            @Param("book") LedgerBook book,
            @Param("txType") TxType txType,
            @Param("oldName") String oldName,
            @Param("newName") String newName,
            @Param("categoryFilter") String categoryFilter);

    @Query(
            """
            SELECT DISTINCT t.txDate FROM CbTransaction t
            WHERE t.book = :book AND t.txDate >= :start AND t.txDate <= :end
            AND t.txType IN :types
            ORDER BY t.txDate
            """)
    List<LocalDate> findDistinctEntryDates(
            @Param("book") LedgerBook book,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end,
            @Param("types") List<TxType> types);

    @Query(
            """
            SELECT DISTINCT t.category FROM CbTransaction t
            WHERE t.book = :book AND t.txType = :txType AND TRIM(t.category) <> ''
            """)
    List<String> findDistinctCategoryNames(
            @Param("book") LedgerBook book, @Param("txType") TxType txType);

    @Query(
            """
            SELECT COUNT(t) > 0 FROM CbTransaction t
            WHERE t.book = :book AND t.txType = :txType AND t.category = :categoryName
            """)
    boolean existsByBookAndTxTypeAndCategory(
            @Param("book") LedgerBook book,
            @Param("txType") TxType txType,
            @Param("categoryName") String categoryName);

    @Query(
            """
            SELECT DISTINCT TRIM(t.cardName) FROM CbTransaction t
            WHERE t.book = :book AND t.txType = :type
            AND t.cardName IS NOT NULL AND TRIM(t.cardName) <> ''
            ORDER BY TRIM(t.cardName)
            """)
    List<String> findDistinctCardNames(@Param("book") LedgerBook book, @Param("type") TxType type);
}