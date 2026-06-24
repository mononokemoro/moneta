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
import org.hibernate.annotations.ColumnDefault;

@Entity
@Table(
        name = "cb_category",
        uniqueConstraints =
                @UniqueConstraint(columnNames = {"book", "category_type", "name", "tier", "parent_id"}))
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

    @Enumerated(EnumType.STRING)
    @Column(length = 8)
    private CategoryTier tier = CategoryTier.MINOR;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false, length = 80)
    private String name = "";

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(nullable = false)
    @ColumnDefault("true")
    private Boolean enabled = true;

    @Column(name = "user_created", nullable = false)
    @ColumnDefault("false")
    private Boolean userCreated = false;

    @Column(name = "fixed_expense", nullable = false)
    @ColumnDefault("false")
    private Boolean fixedExpense = false;

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

    public CategoryTier getTier() {
        return tier;
    }

    public void setTier(CategoryTier tier) {
        this.tier = tier != null ? tier : CategoryTier.MINOR;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
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

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled != null ? enabled : true;
    }

    public Boolean getUserCreated() {
        return userCreated;
    }

    public void setUserCreated(Boolean userCreated) {
        this.userCreated = userCreated != null ? userCreated : false;
    }

    public Boolean getFixedExpense() {
        return fixedExpense;
    }

    public void setFixedExpense(Boolean fixedExpense) {
        this.fixedExpense = fixedExpense != null ? fixedExpense : false;
    }
}
