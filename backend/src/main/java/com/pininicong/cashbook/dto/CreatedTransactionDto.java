package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.TxType;

public record CreatedTransactionDto(Long id, TxType txType) {}
