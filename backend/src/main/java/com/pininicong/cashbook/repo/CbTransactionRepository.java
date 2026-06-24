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
            UPDATE CbTransaction t SET t.title = :newName
            WHERE t.book = :book AND t.txType = :txType AND t.title = :oldName
            AND (:categoryFilter IS NULL OR t.categoryId = :categoryFilterId)
            """)
    int renameTitle(
            @Param("book") LedgerBook book,
            @Param("txType") TxType txType,
            @Param("oldName") String oldName,
            @Param("newName") String newName,
            @Param("categoryFilterId") Long categoryFilterId);

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
            SELECT DISTINCT t.categoryId FROM CbTransaction t
            WHERE t.book = :book AND t.txType = :txType AND t.categoryId IS NOT NULL
            """)
    List<Long> findDistinctCategoryIds(
            @Param("book") LedgerBook book, @Param("txType") TxType txType);

    @Query(
            """
            SELECT COUNT(t) > 0 FROM CbTransaction t
            WHERE t.book = :book AND t.txType = :txType AND t.categoryId = :categoryId
            """)
    boolean existsByBookAndTxTypeAndCategoryId(
            @Param("book") LedgerBook book,
            @Param("txType") TxType txType,
            @Param("categoryId") Long categoryId);

    @Query(
            """
            SELECT DISTINCT t.cardProductId FROM CbTransaction t
            WHERE t.book = :book AND t.txType = :type AND t.cardProductId IS NOT NULL
            ORDER BY t.cardProductId
            """)
    List<Long> findDistinctCardProductIds(
            @Param("book") LedgerBook book, @Param("type") TxType type);

    @Query(
            """
            SELECT DISTINCT t.savingsProductId FROM CbTransaction t
            WHERE t.book = :book AND t.txType = :type AND t.savingsProductId IS NOT NULL
            ORDER BY t.savingsProductId
            """)
    List<Long> findDistinctSavingsProductIds(
            @Param("book") LedgerBook book, @Param("type") TxType type);

    @Query(
            """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM CbTransaction t
            WHERE t.book = :book AND t.txType = com.pininicong.cashbook.domain.TxType.SAVINGS
            AND t.savingsProductId = :productId
            AND t.txDate > :afterDate AND t.txDate <= :untilDate
            """)
    BigDecimal sumSavingsProductFlow(
            @Param("book") LedgerBook book,
            @Param("productId") Long productId,
            @Param("afterDate") LocalDate afterDate,
            @Param("untilDate") LocalDate untilDate);

    @Query(
            """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM CbTransaction t
            WHERE t.book = :book AND t.txType = com.pininicong.cashbook.domain.TxType.SAVINGS
            AND t.title = :title AND t.savingsProductId IS NULL
            AND t.txDate > :afterDate AND t.txDate <= :untilDate
            """)
    BigDecimal sumSavingsTitleFlowUnlinked(
            @Param("book") LedgerBook book,
            @Param("title") String title,
            @Param("afterDate") LocalDate afterDate,
            @Param("untilDate") LocalDate untilDate);

    @Query(
            """
            SELECT COALESCE(SUM(t.amount), 0)
            FROM CbTransaction t
            WHERE t.book = :book AND t.txType = com.pininicong.cashbook.domain.TxType.SAVINGS
            AND t.title = :title AND t.txDate <= :untilDate
            """)
    BigDecimal sumSavingsTitleFlowUpTo(
            @Param("book") LedgerBook book,
            @Param("title") String title,
            @Param("untilDate") LocalDate untilDate);

    @Query(
            """
            SELECT t FROM CbTransaction t
            WHERE t.book = :book AND t.txType = com.pininicong.cashbook.domain.TxType.SAVINGS
            AND t.savingsProductId = :productId
            AND t.txDate >= :start AND t.txDate <= :end
            ORDER BY t.txDate ASC, t.sortOrder ASC, t.id ASC
            """)
    List<CbTransaction> findSavingsByProductBetween(
            @Param("book") LedgerBook book,
            @Param("productId") Long productId,
            @Param("start") LocalDate start,
            @Param("end") LocalDate end);

    @Query(
            """
            SELECT t FROM CbTransaction t
            WHERE t.book = :book AND t.categoryId IN :categoryIds
            ORDER BY t.txDate DESC, t.sortOrder ASC, t.id DESC
            """)
    List<CbTransaction> findByBookAndCategoryIdInOrderByTxDateDescSortOrderAscIdDesc(
            @Param("book") LedgerBook book, @Param("categoryIds") List<Long> categoryIds);

    List<CbTransaction> findByBookAndCategoryIdIsNull(LedgerBook book);

    List<CbTransaction> findByBookAndTxTypeOrderByTxDateDescIdDesc(
            LedgerBook book, TxType txType);
}
