package com.pininicong.cashbook.dto;

public record MonetaImportResult(
        int expensesImported,
        int incomesImported,
        int savingsImported,
        int insuranceImported,
        int categoriesRegistered,
        int fixedItemsRegistered) {}
