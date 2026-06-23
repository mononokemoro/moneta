package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbFixedItem;
import com.pininicong.cashbook.domain.FixedHolidayAdjust;
import com.pininicong.cashbook.domain.FixedKind;
import com.pininicong.cashbook.domain.FixedPeriodType;
import com.pininicong.cashbook.domain.FixedScheduleType;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.domain.TxType;
import com.pininicong.cashbook.dto.FixedItemDto;
import com.pininicong.cashbook.repo.CbFixedItemRepository;
import com.pininicong.cashbook.support.FixedScheduleUtil;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FixedItemService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final CbFixedItemRepository fixedRepo;

    public FixedItemService(CbFixedItemRepository fixedRepo) {
        this.fixedRepo = fixedRepo;
    }

    public FixedItemDto.FixedItemListResponse list(
            LedgerBook book, FixedKind kind, FixedScheduleType scheduleType) {
        LedgerBook ledger = bookOrDefault(book);
        List<CbFixedItem> rows = fixedRepo.findByBookOrderBySortOrderAscIdAsc(ledger);
        return new FixedItemDto.FixedItemListResponse(
                rows.stream()
                        .filter(
                                row ->
                                        (kind == null || row.getKind() == kind)
                                                && (scheduleType == null
                                                        || row.getScheduleType() == scheduleType))
                        .map(FixedItemService::toRow)
                        .collect(Collectors.toList()));
    }

    @Transactional
    public FixedItemDto.FixedItemRow create(LedgerBook book, FixedItemDto.FixedItemSaveRequest req) {
        LedgerBook ledger = bookOrDefault(book);
        CbFixedItem item = new CbFixedItem();
        item.setBook(ledger);
        item.setSortOrder(nextSortOrder(ledger, req.kind(), req.scheduleType()));
        applyRequest(item, req);
        return toRow(fixedRepo.save(item));
    }

    @Transactional
    public FixedItemDto.FixedItemRow update(
            Long id, LedgerBook book, FixedItemDto.FixedItemSaveRequest req) {
        CbFixedItem item = requireItem(id, bookOrDefault(book));
        applyRequest(item, req);
        return toRow(fixedRepo.save(item));
    }

    @Transactional
    public void delete(Long id, LedgerBook book) {
        CbFixedItem item = requireItem(id, bookOrDefault(book));
        fixedRepo.delete(item);
    }

    @Transactional
    public void deleteMany(LedgerBook book, List<Long> ids) {
        LedgerBook ledger = bookOrDefault(book);
        for (Long id : ids) {
            CbFixedItem item = requireItem(id, ledger);
            fixedRepo.delete(item);
        }
    }

    static FixedItemDto.FixedItemRow toRow(CbFixedItem item) {
        return new FixedItemDto.FixedItemRow(
                item.getId(),
                item.getTitle(),
                n(item.getDefaultAmount()),
                n(item.getInterestAmount()),
                item.getCategory() != null ? item.getCategory() : "",
                item.getCardName() != null ? item.getCardName() : "",
                item.getPaymentMethod() != null ? item.getPaymentMethod() : "현금",
                item.getRemarks() != null ? item.getRemarks() : "",
                item.getTxType(),
                item.getKind(),
                item.getScheduleType(),
                item.getDayOfMonth(),
                Boolean.TRUE.equals(item.getLastDayOfMonth()),
                item.getHolidayAdjust() != null ? item.getHolidayAdjust() : FixedHolidayAdjust.NONE,
                item.getPeriodType() != null ? item.getPeriodType() : FixedPeriodType.CONTINUOUS,
                item.getPeriodStart(),
                item.getPeriodEnd(),
                item.getSortOrder() != null ? item.getSortOrder() : 0,
                FixedScheduleUtil.formatScheduleLabel(item),
                item.getHolidayAdjust() != null && item.getHolidayAdjust() != FixedHolidayAdjust.NONE);
    }

    private CbFixedItem requireItem(Long id, LedgerBook book) {
        CbFixedItem item =
                fixedRepo
                        .findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("고정 항목 없음: " + id));
        if (item.getBook() != book) {
            throw new IllegalArgumentException("고정 항목 장부 불일치: " + id);
        }
        return item;
    }

    private int nextSortOrder(LedgerBook book, FixedKind kind, FixedScheduleType scheduleType) {
        return (int)
                fixedRepo.findByBookOrderBySortOrderAscIdAsc(book).stream()
                        .filter(
                                row ->
                                        row.getKind() == kind
                                                && row.getScheduleType() == scheduleType)
                        .count();
    }

    private static void applyRequest(CbFixedItem item, FixedItemDto.FixedItemSaveRequest req) {
        FixedKind kind = req.kind();
        item.setKind(kind);
        item.setTxType(kindToTxType(kind));
        item.setScheduleType(req.scheduleType());
        item.setLastDayOfMonth(Boolean.TRUE.equals(req.lastDayOfMonth()));
        item.setDayOfMonth(Boolean.TRUE.equals(req.lastDayOfMonth()) ? null : req.dayOfMonth());
        item.setHolidayAdjust(
                req.holidayAdjust() != null ? req.holidayAdjust() : FixedHolidayAdjust.NONE);
        item.setPeriodType(req.periodType() != null ? req.periodType() : FixedPeriodType.CONTINUOUS);
        item.setPeriodStart(req.periodStart());
        item.setPeriodEnd(req.periodEnd());
        item.setTitle(req.title().trim());
        item.setDefaultAmount(req.defaultAmount() != null ? req.defaultAmount() : ZERO);
        item.setInterestAmount(req.interestAmount() != null ? req.interestAmount() : ZERO);
        item.setCategory(req.category() != null ? req.category().trim() : "");
        item.setRemarks(req.remarks() != null ? req.remarks().trim() : "");
        item.setPaymentMethod(req.paymentMethod() != null ? req.paymentMethod().trim() : "현금");
        if (kind == FixedKind.EXPENSE) {
            item.setCardName(
                    "현금".equals(item.getPaymentMethod()) ? "" : item.getPaymentMethod());
        } else {
            item.setCardName(req.cardName() != null ? req.cardName().trim() : "");
        }
        validateSchedule(item);
    }

    private static void validateSchedule(CbFixedItem item) {
        if (item.getScheduleType() == FixedScheduleType.MONTHLY
                && !Boolean.TRUE.equals(item.getLastDayOfMonth())
                && (item.getDayOfMonth() == null || item.getDayOfMonth() < 1 || item.getDayOfMonth() > 31)) {
            throw new IllegalArgumentException("매월 고정 일자를 선택하세요.");
        }
        if (item.getPeriodType() == FixedPeriodType.RANGE
                && item.getPeriodStart() != null
                && item.getPeriodEnd() != null
                && item.getPeriodStart().isAfter(item.getPeriodEnd())) {
            throw new IllegalArgumentException("고정기간 시작일이 종료일보다 늦을 수 없습니다.");
        }
        if (item.getTitle().isBlank()) {
            throw new IllegalArgumentException("항목명을 입력하세요.");
        }
    }

    static TxType kindToTxType(FixedKind kind) {
        return switch (kind) {
            case INCOME -> TxType.INCOME;
            case SAVINGS -> TxType.SAVINGS;
            case EXPENSE, LOAN -> TxType.EXPENSE;
        };
    }

    private static LedgerBook bookOrDefault(LedgerBook book) {
        return book != null ? book : LedgerBook.PERSONAL;
    }

    private static BigDecimal n(BigDecimal value) {
        return value != null ? value : ZERO;
    }
}
