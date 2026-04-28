package com.pininicong.cashbook.dto;

import java.math.BigDecimal;

public record TransactionRowDto(
        Long id, String title, BigDecimal amount, String category, String cardName, String remarks) {}
