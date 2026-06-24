package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.ExpenseScope;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionCreateRequest(
        @NotNull LocalDate txDate,
        @NotNull TxType txType,
        String title,
        @NotNull BigDecimal amount,
        Long categoryId,
        String category,
        Long cardProductId,
        String cardName,
        Long savingsProductId,
        String remarks,
        BigDecimal accumulatedAmount,
        LedgerBook book,
        ExpenseScope expenseScope,
        Long householdCategoryId,
        String householdCategory) {}
