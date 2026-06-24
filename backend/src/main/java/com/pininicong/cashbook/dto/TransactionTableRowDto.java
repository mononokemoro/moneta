package com.pininicong.cashbook.dto;

import java.math.BigDecimal;

/** cb_transaction 테이블 컬럼 그대로. */
public record TransactionTableRowDto(
        Long id,
        String book,
        String txDate,
        String txType,
        String title,
        BigDecimal amount,
        Long categoryId,
        Long householdCategoryId,
        Long cardProductId,
        Long savingsProductId,
        String remarks,
        BigDecimal accumulatedAmount,
        Integer sortOrder,
        String expenseScope,
        Long linkedTxId) {}
