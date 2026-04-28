package com.pininicong.cashbook.dto;

import java.math.BigDecimal;

public record SavingsRowDto(
        Long id, String title, BigDecimal amount, BigDecimal accumulatedAmount, String remarks) {}
