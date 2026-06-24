package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbFinancialProduct;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductType;
import com.pininicong.cashbook.domain.CbTransaction;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.repo.CbFinancialProductRepository;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 거래 ↔ 저축/보험 상품 연결 (savings_product_id 기준). */
@Service
@Transactional(readOnly = true)
public class TransactionSavingsProductSupport {

    private static final Logger log = LoggerFactory.getLogger(TransactionSavingsProductSupport.class);

    private final CbFinancialProductRepository productRepo;

    public TransactionSavingsProductSupport(CbFinancialProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    public record ResolvedSavingsProduct(long id, String name, ProductType productType) {}

    public Optional<ResolvedSavingsProduct> resolveProduct(
            LedgerBook book, Long savingsProductId, String titleOrName) {
        LedgerBook ledger = book != null ? book : LedgerBook.PERSONAL;
        if (savingsProductId != null) {
            return productRepo
                    .findByIdAndBook(savingsProductId, ledger)
                    .filter(p -> isSavingsOrInsurance(p.getProductType()))
                    .map(this::toResolved);
        }
        String name = titleOrName != null ? titleOrName.trim() : "";
        if (name.isEmpty()) {
            return Optional.empty();
        }
        Optional<CbFinancialProduct> bySavings =
                findByName(ledger, ProductType.SAVINGS, name);
        if (bySavings.isPresent()) {
            return bySavings.map(this::toResolved);
        }
        return findByName(ledger, ProductType.INSURANCE, name).map(this::toResolved);
    }

    public Optional<CbFinancialProduct> resolveProductEntity(
            LedgerBook book, Long savingsProductId, String titleOrName) {
        return resolveProduct(book, savingsProductId, titleOrName)
                .flatMap(resolved -> productRepo.findByIdAndBook(resolved.id(), book));
    }

    @Transactional
    public void applySavingsProduct(CbTransaction t, Long savingsProductId, String titleOrName) {
        if (t.getTxType() != com.pininicong.cashbook.domain.TxType.SAVINGS) {
            t.setSavingsProductId(null);
            return;
        }
        resolveProduct(t.getBook(), savingsProductId, titleOrName)
                .ifPresentOrElse(
                        resolved -> t.setSavingsProductId(resolved.id()),
                        () -> t.setSavingsProductId(null));
    }

    public String name(LedgerBook book, Long savingsProductId) {
        if (savingsProductId == null) {
            return "";
        }
        return productRepo
                .findByIdAndBook(savingsProductId, book)
                .map(CbFinancialProduct::getName)
                .orElse("");
    }

    private Optional<CbFinancialProduct> findByName(
            LedgerBook book, ProductType type, String name) {
        var matches =
                productRepo.findByBookAndProductTypeAndNameOrderByIdAsc(book, type, name);
        if (matches.isEmpty()) {
            return Optional.empty();
        }
        CbFinancialProduct chosen = matches.getFirst();
        if (matches.size() > 1) {
            log.warn(
                    "동일 이름 {} 상품 {}건(book={}, type={}) — id={} 사용",
                    name,
                    matches.size(),
                    book,
                    type,
                    chosen.getId());
        }
        return Optional.of(chosen);
    }

    private ResolvedSavingsProduct toResolved(CbFinancialProduct p) {
        return new ResolvedSavingsProduct(p.getId(), p.getName(), p.getProductType());
    }

    private static boolean isSavingsOrInsurance(ProductType type) {
        return type == ProductType.SAVINGS || type == ProductType.INSURANCE;
    }
}
