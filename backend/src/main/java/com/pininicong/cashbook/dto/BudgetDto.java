package com.pininicong.cashbook.dto;

import java.math.BigDecimal;

public record BudgetDto(BigDecimal totalBudget, BigDecimal spentInMonth, BigDecimal remainingBudget, String periodLabel) {}
