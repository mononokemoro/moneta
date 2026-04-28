package com.pininicong.cashbook.dto;

import java.math.BigDecimal;

/** 선택 일자 지출을 결제수단별로 요약 (현금 / 신용·체크 등 휴리스틱) */
public record PaymentSummaryDto(
        BigDecimal cash,
        BigDecimal creditCard,
        BigDecimal debitCard,
        BigDecimal otherCard) {}
