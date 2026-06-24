package com.pininicong.cashbook.config;

import com.pininicong.cashbook.domain.CbCategory;
import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.CategoryTier;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.repo.CbCategoryRepository;
import com.pininicong.cashbook.repo.CbTransactionRepository;
import com.pininicong.cashbook.service.TransactionCategorySupport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class TransactionCategoryRelinkSupport {

    private final JdbcTemplate jdbc;
    private final CbCategoryRepository categoryRepo;
    private final CbTransactionRepository txRepo;
    private final TransactionCategorySupport categorySupport;

    public TransactionCategoryRelinkSupport(
            JdbcTemplate jdbc,
            CbCategoryRepository categoryRepo,
            CbTransactionRepository txRepo,
            TransactionCategorySupport categorySupport) {
        this.jdbc = jdbc;
        this.categoryRepo = categoryRepo;
        this.txRepo = txRepo;
        this.categorySupport = categorySupport;
    }

    public int backfillFromLegacyCategoryColumn() {
        if (!tableExists("cb_transaction")) {
            return 0;
        }
        if (!columnExists("cb_transaction", "category")
                || !columnExists("cb_transaction", "category_id")) {
            return 0;
        }
        var rows =
                jdbc.queryForList(
                        """
                        SELECT id, book, tx_type, category AS legacy_name
                        FROM cb_transaction
                        WHERE TRIM(COALESCE(category, '')) <> '' AND category_id IS NULL
                        """);
        int updated = 0;
        for (var row : rows) {
            Long txId = ((Number) row.get("ID")).longValue();
            LedgerBook book = LedgerBook.valueOf(String.valueOf(row.get("BOOK")));
            TxType txType = TxType.valueOf(String.valueOf(row.get("TX_TYPE")));
            String legacyName = String.valueOf(row.get("LEGACY_NAME")).trim();
            var resolved =
                    categorySupport.findMinorIdByName(
                            book, TransactionCategorySupport.categoryTypeFor(txType), legacyName);
            if (resolved.isEmpty()) {
                continue;
            }
            jdbc.update(
                    "UPDATE cb_transaction SET category_id = ? WHERE id = ?",
                    resolved.get(),
                    txId);
            updated++;
        }
        return updated;
    }

    public int consolidateDuplicateMinorLinks() {
        int updated = 0;
        for (LedgerBook book : LedgerBook.values()) {
            for (CategoryType type : List.of(CategoryType.INCOME, CategoryType.EXPENSE)) {
                updated += consolidateFor(book, type);
            }
        }
        return updated;
    }

    private int consolidateFor(LedgerBook book, CategoryType type) {
        TxType txType =
                switch (type) {
                    case INCOME -> TxType.INCOME;
                    case EXPENSE -> TxType.EXPENSE;
                    default -> null;
                };
        if (txType == null) {
            return 0;
        }

        Map<String, Long> canonicalByName = new HashMap<>();
        List<CbCategory> minors =
                categoryRepo.findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
                        book, type, CategoryTier.MINOR);
        for (CbCategory minor : minors) {
            String name = minor.getName();
            Long existing = canonicalByName.get(name);
            if (existing == null) {
                canonicalByName.put(name, minor.getId());
                continue;
            }
            boolean preferNew =
                    isBetterCanonical(minor, categoryRepo.findByIdAndBook(existing, book).orElse(null));
            if (preferNew) {
                canonicalByName.put(name, minor.getId());
            }
        }

        int updated = 0;
        for (var entry : canonicalByName.entrySet()) {
            String name = entry.getKey();
            Long canonicalId = entry.getValue();
            List<Long> duplicateIds =
                    minors.stream()
                            .filter(m -> name.equals(m.getName()))
                            .map(CbCategory::getId)
                            .filter(id -> !id.equals(canonicalId))
                            .toList();
            for (Long oldId : duplicateIds) {
                var txs =
                        txRepo.findByBookAndCategoryIdInOrderByTxDateDescSortOrderAscIdDesc(
                                book, List.of(oldId));
                for (var tx : txs) {
                    if (!canonicalId.equals(tx.getCategoryId())) {
                        tx.setCategoryId(canonicalId);
                        txRepo.save(tx);
                        updated++;
                    }
                }
            }
        }
        return updated;
    }

    /** 트리에 연결된 소분류(parent 있음)를 동명 고아 레코드보다 우선합니다. */
    private static boolean isBetterCanonical(CbCategory candidate, CbCategory current) {
        if (current == null) {
            return true;
        }
        boolean candidateInTree = candidate.getParentId() != null;
        boolean currentInTree = current.getParentId() != null;
        if (candidateInTree != currentInTree) {
            return candidateInTree;
        }
        int sortCandidate = candidate.getSortOrder() != null ? candidate.getSortOrder() : Integer.MAX_VALUE;
        int sortCurrent = current.getSortOrder() != null ? current.getSortOrder() : Integer.MAX_VALUE;
        if (sortCandidate != sortCurrent) {
            return sortCandidate < sortCurrent;
        }
        return candidate.getId() < current.getId();
    }

    private boolean tableExists(String table) {
        Integer count =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES
                        WHERE UPPER(TABLE_NAME) = UPPER(?)
                        """,
                        Integer.class,
                        table);
        return count != null && count > 0;
    }

    private boolean columnExists(String table, String column) {
        Integer count =
                jdbc.queryForObject(
                        """
                        SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS
                        WHERE UPPER(TABLE_NAME) = UPPER(?) AND UPPER(COLUMN_NAME) = UPPER(?)
                        """,
                        Integer.class,
                        table,
                        column);
        return count != null && count > 0;
    }
}
