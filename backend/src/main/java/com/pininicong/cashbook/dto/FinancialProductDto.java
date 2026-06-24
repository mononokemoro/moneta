package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.CbFinancialProduct.ProductStatus;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record FinancialProductDto(
        Long id,
        ProductType productType,
        ProductStatus status,
        int sortOrder,
        String classification,
        String name,
        String paymentMethod,
        String joinDate,
        String maturityDate,
        String startDate,
        String autoTransferDay,
        String transferDay,
        String repaymentDay,
        String paymentDay,
        String periodStartMonth,
        String periodStartDay,
        String periodEndMonth,
        String periodEndDay,
        String principal,
        String cardLimit,
        java.math.BigDecimal openingBalance,
        String openingDate) {

    public record FinancialProductListResponse(
            List<FinancialProductDto> savings,
            List<FinancialProductDto> insurance,
            List<FinancialProductDto> loans,
            List<FinancialProductDto> cards) {}

    public record FinancialProductSaveRequest(@NotNull @Valid FinancialProductListResponse products) {}

    public record CardSyncFromTransactionsResponse(int added, FinancialProductListResponse products) {}
}
