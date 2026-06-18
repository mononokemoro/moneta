package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.TxType;
import java.math.BigDecimal;

public record FixedItemDto(
        Long id, String title, BigDecimal defaultAmount, String category, String cardName, TxType txType) {}
