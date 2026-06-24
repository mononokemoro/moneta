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
@Table(name = "cb_balance_adjustment")
public class CbBalanceAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private LedgerBook book = LedgerBook.PERSONAL;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "adj_date", nullable = false)
    private LocalDate adjDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(length = 200)
    private String reason = "";

    public Long getId() {
        return id;
    }

    public LedgerBook getBook() {
        return book;
    }

    public void setBook(LedgerBook book) {
        this.book = book;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public LocalDate getAdjDate() {
        return adjDate;
    }

    public void setAdjDate(LocalDate adjDate) {
        this.adjDate = adjDate;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount != null ? amount : BigDecimal.ZERO;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason != null ? reason : "";
    }
}
