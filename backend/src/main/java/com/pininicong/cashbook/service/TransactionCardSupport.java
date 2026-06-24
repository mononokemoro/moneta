package com.pininicong.cashbook.service;

import com.pininicong.cashbook.domain.CbFinancialProduct;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductStatus;
import com.pininicong.cashbook.domain.CbFinancialProduct.ProductType;
import com.pininicong.cashbook.domain.LedgerBook;
import com.pininicong.cashbook.repo.CbFinancialProductRepository;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 거래 ↔ 카드 상품 연결 (card_product_id 기준, 표시명 조회). */
@Service
public class TransactionCardSupport {

    public record ResolvedCard(Long id, String name) {}

    private final CbFinancialProductRepository productRepo;

    public TransactionCardSupport(CbFinancialProductRepository productRepo) {
        this.productRepo = productRepo;
    }

    /** id 우선, 없으면 name으로 CARD 상품을 찾거나 생성합니다. */
    @Transactional
    public Optional<ResolvedCard> resolveCard(
            LedgerBook book, Long cardProductId, String cardName) {
        if (cardProductId != null) {
            return productRepo
                    .findByIdAndBook(cardProductId, book)
                    .filter(p -> p.getProductType() == ProductType.CARD)
                    .map(p -> new ResolvedCard(p.getId(), p.getName()));
        }
        String name = normalize(cardName);
        if (name.isEmpty()) {
            return Optional.empty();
        }
        return productRepo
                .findFirstByBookAndProductTypeAndNameOrderByIdAsc(book, ProductType.CARD, name)
                .or(() -> Optional.of(ensureCardProduct(book, name)))
                .map(p -> new ResolvedCard(p.getId(), p.getName()));
    }

    public String name(LedgerBook book, Long cardProductId) {
        if (cardProductId == null) {
            return "";
        }
        return productRepo
                .findByIdAndBook(cardProductId, book)
                .filter(p -> p.getProductType() == ProductType.CARD)
                .map(CbFinancialProduct::getName)
                .orElse("");
    }

    public Map<Long, String> cardNameIndex(LedgerBook book) {
        Map<Long, String> map = new HashMap<>();
        for (CbFinancialProduct row :
                productRepo.findByBookAndProductTypeOrderBySortOrderAscIdAsc(
                        book, ProductType.CARD)) {
            map.put(row.getId(), row.getName());
        }
        return map;
    }

    public Optional<Long> findCardIdByName(LedgerBook book, String name) {
        return resolveCard(book, null, name).map(ResolvedCard::id);
    }

    @Transactional
    public CbFinancialProduct ensureCardProduct(LedgerBook book, String rawName) {
        String name = normalize(rawName);
        return productRepo
                .findFirstByBookAndProductTypeAndNameOrderByIdAsc(book, ProductType.CARD, name)
                .orElseGet(() -> createCardProduct(book, name));
    }

    private CbFinancialProduct createCardProduct(LedgerBook book, String name) {
        List<CbFinancialProduct> existing =
                productRepo.findByBookAndProductTypeOrderBySortOrderAscIdAsc(
                        book, ProductType.CARD);
        int order =
                existing.stream()
                        .mapToInt(c -> c.getSortOrder() != null ? c.getSortOrder() : 0)
                        .max()
                        .orElse(-1)
                        + 1;
        CbFinancialProduct row = new CbFinancialProduct();
        row.setBook(book);
        row.setProductType(ProductType.CARD);
        row.setStatus(ProductStatus.ACTIVE);
        row.setSortOrder(order);
        row.setClassification("신용카드");
        row.setName(name);
        row.setPaymentDay("13");
        row.setPeriodStartMonth("전월");
        row.setPeriodStartDay("01");
        row.setPeriodEndMonth("전월");
        row.setPeriodEndDay("31");
        return productRepo.save(row);
    }

    public Optional<Long> mirrorCardId(
            LedgerBook sourceBook, LedgerBook targetBook, Long sourceCardProductId) {
        if (sourceCardProductId == null || sourceBook == targetBook) {
            return Optional.ofNullable(sourceCardProductId);
        }
        String name = name(sourceBook, sourceCardProductId);
        if (name.isEmpty()) {
            return Optional.empty();
        }
        return resolveCard(targetBook, null, name).map(ResolvedCard::id);
    }

    private static String normalize(String value) {
        return value != null ? value.trim() : "";
    }
}
