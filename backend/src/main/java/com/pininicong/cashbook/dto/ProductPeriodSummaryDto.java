package com.pininicong.cashbook.dto;

import java.math.BigDecimal;

public record ProductPeriodSummaryDto(
        Long productId,
        String productName,
        String periodType,
        String periodKey,
        BigDecimal inflow,
        BigDecimal outflow,
        BigDecimal netFlow,
        BigDecimal endBalance) {}
