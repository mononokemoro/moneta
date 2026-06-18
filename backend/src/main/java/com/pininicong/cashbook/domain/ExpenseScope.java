package com.pininicong.cashbook.domain;

public enum ExpenseScope {
    NORMAL(""),
    COMMON("공통");

    private final String label;

    ExpenseScope(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
