package com.pininicong.cashbook.dto;

import com.pininicong.cashbook.domain.FixedHolidayAdjust;
import com.pininicong.cashbook.domain.FixedKind;
import com.pininicong.cashbook.domain.FixedPeriodType;
import com.pininicong.cashbook.domain.FixedScheduleType;
import com.pininicong.cashbook.domain.TxType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public final class FixedItemDto {

    private FixedItemDto() {}

    public record FixedItemRow(
            Long id,
            String title,
            BigDecimal defaultAmount,
            BigDecimal interestAmount,
            String category,
            String cardName,
            String paymentMethod,
            String remarks,
            TxType txType,
            FixedKind kind,
            FixedScheduleType scheduleType,
            Integer dayOfMonth,
            boolean lastDayOfMonth,
            FixedHolidayAdjust holidayAdjust,
            FixedPeriodType periodType,
            LocalDate periodStart,
            LocalDate periodEnd,
            int sortOrder,
            String scheduleLabel,
            boolean holidayCheck) {}

    public record FixedItemListResponse(List<FixedItemRow> items) {}

    public record FixedItemSaveRequest(
            @NotNull FixedKind kind,
            @NotNull FixedScheduleType scheduleType,
            Integer dayOfMonth,
            Boolean lastDayOfMonth,
            FixedHolidayAdjust holidayAdjust,
            FixedPeriodType periodType,
            LocalDate periodStart,
            LocalDate periodEnd,
            @NotBlank String title,
            BigDecimal defaultAmount,
            BigDecimal interestAmount,
            String category,
            String cardName,
            String paymentMethod,
            String remarks) {}

    public record FixedItemBulkDeleteRequest(@NotNull List<Long> ids) {}
}
