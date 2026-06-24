package com.pininicong.cashbook.dto;

import java.math.BigDecimal;

public record TransactionRowDto(
        Long id,
        String title,
        BigDecimal amount,
        Long categoryId,
        String category,
        Long cardProductId,
        String cardName,
        String remarks,
        String expenseScope,
        Long householdCategoryId,
        String householdCategory) {}
