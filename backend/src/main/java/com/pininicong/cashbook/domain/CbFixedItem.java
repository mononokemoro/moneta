package com.pininicong.cashbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "cb_fixed_item")
public class CbFixedItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private LedgerBook book = LedgerBook.PERSONAL;

    @Column(nullable = false, length = 200)
    private String title = "";

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal defaultAmount = BigDecimal.ZERO;

    @Column(length = 80)
    private String category = "";

    @Column(length = 80)
    private String cardName = "";

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private TxType txType = TxType.EXPENSE;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    /** 기존 DB 행 호환: nullable 컬럼으로 추가 후 마이그레이션 러너가 채웁니다. */
    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private FixedKind kind;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private FixedScheduleType scheduleType;

    @Column
    private Integer dayOfMonth;

    @Column
    private Boolean lastDayOfMonth;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private FixedHolidayAdjust holidayAdjust;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private FixedPeriodType periodType;

    @Column
    private LocalDate periodStart;

    @Column
    private LocalDate periodEnd;

    @Column(length = 500)
    private String remarks;

    @Column(precision = 19, scale = 2)
    private BigDecimal interestAmount;

    @Column(length = 40)
    private String paymentMethod;

    public Long getId() {
        return id;
    }

    public LedgerBook getBook() {
        return book;
    }

    public void setBook(LedgerBook book) {
        this.book = book;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public BigDecimal getDefaultAmount() {
        return defaultAmount;
    }

    public void setDefaultAmount(BigDecimal defaultAmount) {
        this.defaultAmount = defaultAmount;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getCardName() {
        return cardName;
    }

    public void setCardName(String cardName) {
        this.cardName = cardName;
    }

    public TxType getTxType() {
        return txType;
    }

    public void setTxType(TxType txType) {
        this.txType = txType;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    public FixedKind getKind() {
        return kind != null ? kind : FixedKind.EXPENSE;
    }

    public void setKind(FixedKind kind) {
        this.kind = kind;
    }

    public FixedScheduleType getScheduleType() {
        return scheduleType != null ? scheduleType : FixedScheduleType.MONTHLY;
    }

    public void setScheduleType(FixedScheduleType scheduleType) {
        this.scheduleType = scheduleType;
    }

    public Integer getDayOfMonth() {
        return dayOfMonth;
    }

    public void setDayOfMonth(Integer dayOfMonth) {
        this.dayOfMonth = dayOfMonth;
    }

    public Boolean getLastDayOfMonth() {
        return lastDayOfMonth;
    }

    public void setLastDayOfMonth(Boolean lastDayOfMonth) {
        this.lastDayOfMonth = lastDayOfMonth;
    }

    public FixedHolidayAdjust getHolidayAdjust() {
        return holidayAdjust != null ? holidayAdjust : FixedHolidayAdjust.NONE;
    }

    public void setHolidayAdjust(FixedHolidayAdjust holidayAdjust) {
        this.holidayAdjust = holidayAdjust;
    }

    public FixedPeriodType getPeriodType() {
        return periodType != null ? periodType : FixedPeriodType.CONTINUOUS;
    }

    public void setPeriodType(FixedPeriodType periodType) {
        this.periodType = periodType;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate periodEnd) {
        this.periodEnd = periodEnd;
    }

    public String getRemarks() {
        return remarks != null ? remarks : "";
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount != null ? interestAmount : BigDecimal.ZERO;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public String getPaymentMethod() {
        return paymentMethod != null ? paymentMethod : "현금";
    }

    public void setPaymentMethod(String paymentMethod) {
        this.paymentMethod = paymentMethod;
    }
}
