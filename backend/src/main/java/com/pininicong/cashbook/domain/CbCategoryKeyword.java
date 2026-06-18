package com.pininicong.cashbook.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "cb_category_keyword",
        uniqueConstraints = @UniqueConstraint(columnNames = {"book", "tx_type", "keyword"}))
public class CbCategoryKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(length = 16)
    private LedgerBook book = LedgerBook.PERSONAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "tx_type", nullable = false, length = 16)
    private TxType txType;

    @Column(nullable = false, length = 80)
    private String keyword = "";

    @Column(name = "category_name", nullable = false, length = 80)
    private String categoryName = "";

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

    public TxType getTxType() {
        return txType;
    }

    public void setTxType(TxType txType) {
        this.txType = txType;
    }

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public Integer getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }
}
