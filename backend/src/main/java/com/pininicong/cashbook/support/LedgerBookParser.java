package com.pininicong.cashbook.support;

import com.pininicong.cashbook.domain.LedgerBook;

public final class LedgerBookParser {

    private LedgerBookParser() {}

    public static LedgerBook parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return LedgerBook.PERSONAL;
        }
        try {
            return LedgerBook.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException("알 수 없는 장부: " + raw);
        }
    }
}
