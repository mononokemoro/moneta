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
@Table(name = "cb_product_balance_anchor")
public class CbProductBalanceAnchor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 16, nullable = false)
    private LedgerBook book = LedgerBook.PERSONAL;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "anchor_date", nullable = false)
    private LocalDate anchorDate;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(length = 500)
    private String remarks = "";

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

    public LocalDate getAnchorDate() {
        return anchorDate;
    }

    public void setAnchorDate(LocalDate anchorDate) {
        this.anchorDate = anchorDate;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public void setBalance(BigDecimal balance) {
        this.balance = balance != null ? balance : BigDecimal.ZERO;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks != null ? remarks : "";
    }
}
