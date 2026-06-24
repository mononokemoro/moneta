package com.pininicong.cashbook.repo;

import com.pininicong.cashbook.domain.CbFinancialProduct;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductType;
import com.pininicong.cashbook.domain.LedgerBook;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CbFinancialProductRepository extends JpaRepository<CbFinancialProduct, Long> {

    Optional<CbFinancialProduct> findByIdAndBook(Long id, LedgerBook book);

    List<CbFinancialProduct> findByBookAndProductTypeAndNameOrderByIdAsc(
            LedgerBook book, ProductType productType, String name);

    /** 동일 이름 상품이 여러 건이면 id가 가장 작은 것을 사용합니다. */
    default Optional<CbFinancialProduct> findFirstByBookAndProductTypeAndNameOrderByIdAsc(
            LedgerBook book, ProductType productType, String name) {
        var matches = findByBookAndProductTypeAndNameOrderByIdAsc(book, productType, name);
        return matches.isEmpty() ? Optional.empty() : Optional.of(matches.getFirst());
    }

    List<CbFinancialProduct> findByBookOrderByProductTypeAscSortOrderAscIdAsc(LedgerBook book);

    List<CbFinancialProduct> findByBookAndProductTypeOrderBySortOrderAscIdAsc(
            LedgerBook book, ProductType productType);

    long countByBook(LedgerBook book);

    void deleteByBook(LedgerBook book);
}
