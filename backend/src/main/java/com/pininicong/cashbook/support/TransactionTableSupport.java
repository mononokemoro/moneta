package com.pininicong.cashbook.support;

import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.dto.TransactionTableRowDto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

public final class TransactionTableSupport {

    public static final String TABLE_NAME = "cb_transaction";

    public static final String SELECT_COLUMNS =
            "id, book, tx_date, tx_type, title, amount, category_id, household_category_id,"
                    + " card_product_id, savings_product_id, remarks, accumulated_amount, sort_order, expense_scope,"
                    + " linked_tx_id";

    private TransactionTableSupport() {}

    public static TransactionTableRowDto toRow(CbTransaction tx) {
        return new TransactionTableRowDto(
                tx.getId(),
                tx.getBook() != null ? tx.getBook().name() : "",
                tx.getTxDate() != null ? tx.getTxDate().toString() : "",
                tx.getTxType() != null ? tx.getTxType().name() : "",
                tx.getTitle() != null ? tx.getTitle() : "",
                tx.getAmount() != null ? tx.getAmount() : BigDecimal.ZERO,
                tx.getCategoryId(),
                tx.getHouseholdCategoryId(),
                tx.getCardProductId(),
                tx.getSavingsProductId(),
                tx.getRemarks() != null ? tx.getRemarks() : "",
                tx.getAccumulatedAmount(),
                tx.getSortOrder(),
                tx.getExpenseScope() != null ? tx.getExpenseScope().name() : "",
                tx.getLinkedTxId());
    }

    public static String queryForCategory(LedgerBook book, List<Long> categoryIds) {
        String bookLit = sqlString(book.name());
        String whereCategory =
                categoryIds.isEmpty()
                        ? "1 = 0"
                        : "category_id IN (" + joinIds(categoryIds) + ")";
        return """
                SELECT %s
                FROM %s
                WHERE book = %s
                  AND %s
                ORDER BY tx_date DESC, sort_order ASC, id DESC"""
                .formatted(SELECT_COLUMNS, TABLE_NAME, bookLit, whereCategory);
    }

    public static String queryForDay(LedgerBook book, LocalDate date) {
        return """
                SELECT %s
                FROM %s
                WHERE book = %s
                  AND tx_date = %s
                ORDER BY CASE tx_type
                           WHEN 'EXPENSE' THEN 0
                           WHEN 'INCOME' THEN 1
                           WHEN 'SAVINGS' THEN 2
                           ELSE 3
                         END,
                         sort_order ASC,
                         id ASC"""
                .formatted(SELECT_COLUMNS, TABLE_NAME, sqlString(book.name()), sqlString(date.toString()));
    }

    private static String joinIds(List<Long> ids) {
        return ids.stream().map(String::valueOf).collect(Collectors.joining(", "));
    }

    private static String sqlString(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
}
