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

}

