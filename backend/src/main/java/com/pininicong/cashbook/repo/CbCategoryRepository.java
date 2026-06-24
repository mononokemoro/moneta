package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbCategory;
import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.CategoryTier;
import com.pininicong.cashbook.domain.LedgerBook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbCategoryRepository extends JpaRepository<CbCategory, Long> {

    List<CbCategory> findByBookAndCategoryTypeOrderBySortOrderAscNameAsc(
            LedgerBook book, CategoryType type);

    List<CbCategory> findByBookAndCategoryTypeAndTierOrderBySortOrderAscNameAsc(
            LedgerBook book, CategoryType type, CategoryTier tier);

    List<CbCategory> findByBookAndCategoryTypeAndParentIdOrderBySortOrderAscNameAsc(
            LedgerBook book, CategoryType type, Long parentId);

    boolean existsByBookAndCategoryTypeAndName(LedgerBook book, CategoryType type, String name);

    boolean existsByBookAndCategoryTypeAndNameAndTier(
            LedgerBook book, CategoryType type, String name, CategoryTier tier);

    boolean existsByBookAndCategoryTypeAndNameAndTierAndParentId(
            LedgerBook book, CategoryType type, String name, CategoryTier tier, Long parentId);

    java.util.Optional<CbCategory> findByBookAndCategoryTypeAndNameAndTier(
            LedgerBook book, CategoryType type, String name, CategoryTier tier);

    java.util.Optional<CbCategory> findByBookAndCategoryTypeAndNameAndTierAndParentId(
            LedgerBook book, CategoryType type, String name, CategoryTier tier, Long parentId);

    long countByBook(LedgerBook book);

    long countByBookAndCategoryTypeAndParentIdAndTier(
            LedgerBook book, CategoryType type, Long parentId, CategoryTier tier);

    Optional<CbCategory> findByIdAndBook(Long id, LedgerBook book);

    List<CbCategory> findByBookAndCategoryTypeAndTierIsNull(LedgerBook book, CategoryType type);
}
