package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbCategory;
import com.pininicong.cashbook.domain.CbCategory.CategoryType;
import com.pininicong.cashbook.domain.LedgerBook;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbCategoryRepository extends JpaRepository<CbCategory, Long> {

    List<CbCategory> findByBookAndCategoryTypeOrderBySortOrderAscNameAsc(
            LedgerBook book, CategoryType type);

    boolean existsByBookAndCategoryTypeAndName(LedgerBook book, CategoryType type, String name);

    long countByBook(LedgerBook book);
}
