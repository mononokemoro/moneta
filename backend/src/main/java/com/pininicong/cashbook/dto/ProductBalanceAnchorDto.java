package com.pininicong.cashbook.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductBalanceAnchorDto(
        Long id,
        Long productId,
        String productName,
        LocalDate anchorDate,
        BigDecimal balance,
        String remarks) {}
