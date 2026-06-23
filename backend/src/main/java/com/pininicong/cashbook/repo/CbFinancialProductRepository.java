package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbFinancialProduct;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductType;
import com.pininicong.cashbook.domain.LedgerBook;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbFinancialProductRepository extends JpaRepository<CbFinancialProduct, Long> {

    List<CbFinancialProduct> findByBookOrderByProductTypeAscSortOrderAscIdAsc(LedgerBook book);

    List<CbFinancialProduct> findByBookAndProductTypeOrderBySortOrderAscIdAsc(
            LedgerBook book, ProductType productType);

    long countByBook(LedgerBook book);

    void deleteByBook(LedgerBook book);
}
