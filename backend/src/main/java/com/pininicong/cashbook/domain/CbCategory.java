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

        name = "cb_category",

        uniqueConstraints = @UniqueConstraint(columnNames = {"book", "category_type", "name"}))

public class CbCategory {



    public enum CategoryType {

        EXPENSE,

        INCOME,

        SAVINGS,

        INSURANCE

    }



    @Id

    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;



    @Enumerated(EnumType.STRING)

    @Column(length = 16)

    private LedgerBook book = LedgerBook.PERSONAL;



    @Enumerated(EnumType.STRING)

    @Column(name = "category_type", nullable = false, length = 16)

    private CategoryType categoryType;



    @Column(nullable = false, length = 80)

    private String name = "";



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



    public CategoryType getCategoryType() {

        return categoryType;

    }



    public void setCategoryType(CategoryType categoryType) {

        this.categoryType = categoryType;

    }



    public String getName() {

        return name;

    }



    public void setName(String name) {

        this.name = name;

    }



    public Integer getSortOrder() {

        return sortOrder;

    }



    public void setSortOrder(Integer sortOrder) {

        this.sortOrder = sortOrder;

    }

}

