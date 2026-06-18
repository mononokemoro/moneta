package com.pininicong.cashbook.domain;

public enum LedgerBook {
    PERSONAL("개인"),
    HOUSEHOLD("가계");

    private final String label;

    LedgerBook(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
