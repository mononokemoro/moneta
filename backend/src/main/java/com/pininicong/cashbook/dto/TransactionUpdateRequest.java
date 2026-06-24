package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.ExpenseScope;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionUpdateRequest(
        String title,
        @NotNull BigDecimal amount,
        Long categoryId,
        String category,
        Long cardProductId,
        String cardName,
        Long savingsProductId,
        String remarks,
        BigDecimal accumulatedAmount,
        ExpenseScope expenseScope,
        Long householdCategoryId,
        String householdCategory,
        LocalDate txDate) {}
