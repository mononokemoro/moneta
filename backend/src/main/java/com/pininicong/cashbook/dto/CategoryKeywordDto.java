package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.TxType;

public record CategoryKeywordDto(Long id, TxType txType, String keyword, String categoryName) {}
